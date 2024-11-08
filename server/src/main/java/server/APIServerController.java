package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@RequestMapping("/api/chat")
public class APIServerController {

	private static final Path VENV_PATH = Path.of("server/src/main/java/server/python/venv/");
	private static final Path VENV_EXECUTABLE = isWindows()
			? VENV_PATH.resolve("Scripts/python.exe")
			: VENV_PATH.resolve("bin/python");

	public static void startServer(String[] args) {
		SpringApplication.run(APIServerController.class, args);
	}

	@PostMapping("/generate")
	public ResponseEntity<?> generate(@RequestBody Map<String, String> requestBody) {
		String prompt = requestBody.get("prompt");

		if (prompt == null || prompt.isEmpty()) {
			return ResponseEntity
					.status(HttpStatus.BAD_REQUEST)
					.body(Map.of("error", "Invalid request, 'prompt' missing"));
		}

		try {
			String response = getResponse(prompt);
			if (!response.isEmpty()) {
				return ResponseEntity.ok(Map.of("response", response));
			} else {
				throw new Exception("No response from AI server.");
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Internal server error", "details", e.getMessage()));
		}
	}

	private static String getResponse(String prompt) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder(VENV_EXECUTABLE.toString(), "server/src/main/java/server/python/AIServer.py", prompt);
		processBuilder.redirectErrorStream(true);
		Process process = null;
		try {
			process = processBuilder.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
			StringBuilder responseBuilder = new StringBuilder();
			String line;

			while ((line = reader.readLine()) != null) {
				responseBuilder.append(line);
			}

			// Wait for the process to complete with a timeout
			if (!process.waitFor(60, TimeUnit.SECONDS)) {
				process.destroy();
				throw new IOException("Process timed out and was terminated.");
			}

			int exitCode = process.exitValue();
			if (exitCode != 0) {
				throw new IOException("Python script exited with non-zero code: " + exitCode);
			}

			String response = responseBuilder.toString();
			if (response.isEmpty()) {
				throw new IOException("Empty response from Python script.");
			}
			return response;
		} finally {
			if (process != null) {
				process.destroy();
			}
		}
	}

	@GetMapping("/healthcheck")
	public ResponseEntity<?> healthcheck() {
		return ResponseEntity.ok(Map.of("status", "ok", "message", "Server is running"));
	}

	private static boolean isWindows() {
		return System.getProperty("os.name").toLowerCase().contains("win");
	}
}
