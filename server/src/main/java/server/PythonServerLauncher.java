package server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Scanner;

public class PythonServerLauncher {

	private static final Path VENV_PATH = Path.of("server/src/main/java/server/python/venv");
	private static final String REQUIREMENTS_PATH = "server/src/main/java/server/python/requirements.txt";
	private static final Path MODEL_FOLDER_PATH = Path.of("server/src/main/java/server/model/");
	private static String pipExecutable;

	public static void main(String[] args) {
		System.out.println("Starting API server...");
		APIServerController.startServer(args);

		launch();
	}

	public static void launch() {
		Process apiServerProcess = null;
		Process downloadProcess = null;

		pipExecutable = VENV_PATH + (isWindows() ? "/Scripts/pip.exe" : "/bin/pip");

		try {
			setupVirtualEnvironment();
			installPythonDependencies();
			downloadModelIfNeeded();

			// Loop for API testing
			try (Scanner scanner = new Scanner(System.in)) {
				while (true) {
					System.out.println("\nEnter a prompt:");
					String prompt = scanner.nextLine();
					sendApiRequest(prompt);
				}
			}

		} catch (IOException | InterruptedException e) {
			System.err.println("An error occurred during the setup: " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (apiServerProcess != null) apiServerProcess.destroy();
			if (downloadProcess != null) downloadProcess.destroy();
			System.out.println("Server has been stopped.");
		}
	}

	private static void setupVirtualEnvironment() throws IOException, InterruptedException {
		if (Files.notExists(VENV_PATH)) {
			System.out.println("Creating virtual environment...");
			new ProcessBuilder("python", "-m", "venv", VENV_PATH.toString()).start().waitFor();
		}
	}

	private static void installPythonDependencies() throws IOException, InterruptedException {
		System.out.println("Updating pip...");
		new ProcessBuilder(pipExecutable, "install", "--upgrade", "pip").start().waitFor();

		System.out.println("Installing Python dependencies...");
		Process pipProcess = new ProcessBuilder(pipExecutable, "install", "-r", REQUIREMENTS_PATH).start();

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipProcess.getInputStream()));
			 BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipProcess.getErrorStream()))) {

			String line;
			while ((line = reader.readLine()) != null) {
				System.out.println("Pip Install: " + line);
			}
			while ((line = errorReader.readLine()) != null) {
				System.err.println("Pip Install [ERROR]: " + line);
			}
		}
		pipProcess.waitFor();
		System.out.println("Python dependencies installed successfully.");
	}

	private static void downloadModelIfNeeded() throws IOException, InterruptedException {
		System.out.println("Checking model folder contents...");
		if (Files.notExists(MODEL_FOLDER_PATH)) {
			Files.createDirectory(MODEL_FOLDER_PATH);
			System.out.println("Model folder created.");
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(MODEL_FOLDER_PATH)) {
			boolean modelExists = stream.iterator().hasNext();
			if (!modelExists) {
				System.out.println("Model folder is empty. Downloading model...");
				ProcessBuilder processBuilder = new ProcessBuilder("python", "server/src/main/java/server/python/Downloader.py");
				Process downloadProcess = processBuilder.start();

				// Separate Threads to read output and error streams
				Thread outputThread = new Thread(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(downloadProcess.getInputStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							System.out.println(line);
						}
					} catch (IOException e) {
						System.err.println("Error reading download output: " + e.getMessage());
					}
				});

				Thread errorThread = new Thread(() -> {
					try (BufferedReader reader = new BufferedReader(new InputStreamReader(downloadProcess.getErrorStream()))) {
						String line;
						while ((line = reader.readLine()) != null) {
							System.err.println(line);
						}
					} catch (IOException e) {
						System.err.println("Error reading download error output: " + e.getMessage());
					}
				});

				outputThread.start();
				errorThread.start();

				downloadProcess.waitFor();
				outputThread.join();
				errorThread.join();

				System.out.println("Model downloaded successfully.");
			}
		}
	}

	private static void sendApiRequest(String prompt) {
		String url = "http://localhost:8080/api/chat/generate";
		Map<String, String> requestBody = Map.of("prompt", prompt);
		ObjectMapper mapper = new ObjectMapper();

		try {
			String jsonBody = mapper.writeValueAsString(requestBody);
			HttpRequest request = HttpRequest.newBuilder()
					.uri(new URI(url))
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();

			HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
			System.out.println("Status code: " + response.statusCode());
			System.out.println("Response body: " + response.body());

		} catch (Exception e) {
			System.err.println("API request error: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
}
