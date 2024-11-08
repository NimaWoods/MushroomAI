package server;

import java.io.BufferedReader;
import java.io.File;
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

import com.fasterxml.jackson.databind.ObjectMapper;

public class PythonServerLauncher {

	private static final Path VENV_PATH = Path.of("server/src/main/java/server/python/venv/");
	private static final Path REQUIREMENTS_PATH = Path.of("server/src/main/java/server/python/requirements.txt");
	private static final Path MODEL_FOLDER_PATH = Path.of("server/src/main/java/server/model/");
	private static final Path VENV_EXECUTABLE = isWindows() ? VENV_PATH.resolve("Scripts/python.exe") : VENV_PATH.resolve("bin/python");
	private static final String SYSTEM_PYTHON_EXECUTABLE = isWindows() ? "python" : "python3";

	public static void main(String[] args) {
		System.out.println("Starting API server...");
		APIServerController.startServer(args);
		launch();
	}

	public static void launch() {
		try {
			createFilesAndDirectories();
			setupVirtualEnvironment();
			installPythonDependencies();
			downloadModelIfNeeded();
			startPromptLoop();

		} catch (IOException | InterruptedException e) {
			System.err.println("Setup error: " + e.getMessage());
			e.printStackTrace();
		} finally {
			System.out.println("Server has been stopped.");
		}
	}

	private static void setupVirtualEnvironment() throws IOException, InterruptedException {
		if (Files.notExists(VENV_PATH)) {
			System.out.println("Creating virtual environment...");
			runProcess(SYSTEM_PYTHON_EXECUTABLE, "-m", "venv", VENV_PATH.toString());
			System.out.println("Virtual environment created successfully.");
		}
	}

	private static void installPythonDependencies() throws IOException, InterruptedException {
		System.out.println("Updating pip and installing dependencies...");
		runProcess(VENV_EXECUTABLE.toString(), "-m", "pip", "install", "--upgrade", "pip");
		runProcess(VENV_EXECUTABLE.toString(), "-m", "pip", "install", "-r", REQUIREMENTS_PATH.toString());
		System.out.println("Python dependencies installed successfully.");
	}

	private static void downloadModelIfNeeded() throws IOException, InterruptedException {
		if (Files.notExists(MODEL_FOLDER_PATH)) {
			Files.createDirectory(MODEL_FOLDER_PATH);
			System.out.println("Model folder created.");
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(MODEL_FOLDER_PATH)) {
			if (!stream.iterator().hasNext()) {
				System.out.println("Model folder is empty...");
				runProcessAsync(VENV_EXECUTABLE.toString(), "server/src/main/java/server/python/Downloader.py");
				System.out.println("Model downloaded successfully.");

				// Train newly created Model
				runProcessAsync(VENV_EXECUTABLE.toString(), "server/src/main/java/server/python/AITrainer.py");
				System.out.println("Model trained successfully.");
			}
		}
		runProcessAsync(VENV_EXECUTABLE.toString(), "server/src/main/java/server/python/AITrainer.py");
		System.out.println("Model trained successfully.");
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

	private static void runProcess(String... commands) throws IOException, InterruptedException {
		Process process = new ProcessBuilder(commands).start();
		try (BufferedReader outputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		     BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
			outputReader.lines().forEach(System.out::println);
			errorReader.lines().forEach(System.err::println);
		}
		int exitCode = process.waitFor();
		if (exitCode != 0) {
			System.err.println("Process failed with exit code: " + exitCode);
		}
	}

	private static void runProcessAsync(String... commands) throws InterruptedException, IOException {
		Process process = new ProcessBuilder(commands).start();

		Thread outputThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				reader.lines().forEach(System.out::println);
			} catch (IOException e) {
				System.err.println("Error reading process output: " + e.getMessage());
			}
		});

		Thread errorThread = new Thread(() -> {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
				reader.lines().forEach(System.err::println);
			} catch (IOException e) {
				System.err.println("Error reading process error: " + e.getMessage());
			}
		});

		outputThread.start();
		errorThread.start();
		process.waitFor();
		outputThread.join();
		errorThread.join();
	}

	public static void createFilesAndDirectories() {
		try {
			Files.createDirectories(MODEL_FOLDER_PATH);

			File configFile = new File("/");
			if (configFile.createNewFile()) {
				System.out.println("Config file created: " + configFile.getName());
			} else {
				System.out.println("Config file already exists.");
			}

		} catch (IOException e) {
			System.err.println("Error creating directories: " + e.getMessage());
		}
	}

	public static void startPromptLoop() {
		Scanner scanner = new Scanner(System.in);
		while (true) {
			System.out.print("Enter a prompt: ");
			String prompt = scanner.nextLine();
			if (prompt.equals("exit")) {
				break;
			}
			sendApiRequest(prompt);
		}
		scanner.close();
	}
}