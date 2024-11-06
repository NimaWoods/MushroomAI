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

import static server.Constants.PATHS.*;

public class PythonServerLauncher {

	public static void main(String[] args) {
		// Start the API server
		System.out.println("Starting API server...");
		APIServerController.startServer(args);

		launch();
	}

	public static void launch() {
		Process apiServerProcess = null;
		Process downloadProcess = null;

		Path venvPath = Path.of(PYTHON_FOLDER.getPath() + "venv");
		String pipExecutable = venvPath + "/Scripts/pip";

		try {
			// Check if venv exists, if not, create it
			if (Files.notExists(venvPath)) {
				System.out.println("Creating virtual environment...");
				new ProcessBuilder("python", "-m", "venv", venvPath.toString()).start().waitFor();
			}

			// Set pip executable path for Windows or Unix-based systems
			if (System.getProperty("os.name").toLowerCase().contains("win")) {
				pipExecutable += ".exe";
			} else {
				pipExecutable = venvPath + "/bin/pip";
			}

			// Update pip
			System.out.println("Updating pip...");
			new ProcessBuilder(pipExecutable, "install", "--upgrade", "pip").start().waitFor();

			// Install dependencies
			System.out.println("Installing Python dependencies...");
			ProcessBuilder pipInstall = new ProcessBuilder(pipExecutable, "install", "-r", REQUIREMENTS_FILE.getPath());
			Process pipProcess = pipInstall.start();

			// Read output from the pip installation process
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(pipProcess.getInputStream())); BufferedReader errorReader = new BufferedReader(new InputStreamReader(pipProcess.getErrorStream()))) {
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

			// Check if model folder exists
			Path modelFolder = Path.of(MODEL_FOLDER.getPath());
			if (Files.notExists(modelFolder)) {
				System.out.println("Model folder does not exist. Creating...");
				Files.createDirectory(modelFolder);
			}

			// Check if model folder is empty and download model if needed
			System.out.println("Checking model folder contents...");
			boolean modelExists = false;
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(modelFolder)) {
				modelExists = stream.iterator().hasNext();

				if (!modelExists) {
					System.out.println("Model folder is empty. Downloading model...");
					downloadProcess = new ProcessBuilder(pipExecutable, "python ", PYTHON_FOLDER.getPath(), "Downloader.py").start();
					downloadProcess.waitFor();
					System.out.println("Model downloaded successfully.");
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

			while (true) {
				// Send api call to server for testing purposes
				Scanner scanner = new Scanner(System.in);
				String url = "http://localhost:8080/api/chat/generate";

				System.out.println("");
				System.out.println("Enter a prompt:");
				String prompt = scanner.next();

				try {
					// Prepare JSON request body
					Map<String, String> requestBody = Map.of("prompt", prompt);
					ObjectMapper mapper = new ObjectMapper();
					String jsonBody = mapper.writeValueAsString(requestBody);

					// Create an HTTP client
					HttpClient client = HttpClient.newHttpClient();

					// Create an HTTP request
					HttpRequest request = HttpRequest.newBuilder()
							.uri(new URI(url))
							.header("Content-Type", "application/json")
							.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
							.build();

					// Send the request and get the response
					HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

					// Output the response
					System.out.println("Status code: " + response.statusCode());
					System.out.println("Response body: " + response.body());
				} catch (Exception e) {
					System.err.println("Error occurred: " + e.getMessage());
				}
			}

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		} finally {
			// Terminate processes on Java program exit
			if (apiServerProcess != null) apiServerProcess.destroy();
			if (downloadProcess != null) downloadProcess.destroy();
			System.out.println("Server has been stopped.");
		}
	}
}
