package server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

@SpringBootApplication
@RestController
@RequestMapping("/api/chat")
public class APIServerController {

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
				throw new Exception("No Response from AI");
			}
		} catch (Exception e) {
			System.err.println("Error: " + e.getMessage());
			return ResponseEntity
					.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(Map.of("error", "Internal server error"));
		}
	}

	private static String getResponse(String prompt) throws IOException, InterruptedException {
		ProcessBuilder processBuilder = new ProcessBuilder("python3", "server/src/main/java/server/AIServer.py", prompt);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder responseBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			responseBuilder.append(line);
		}

		String response = responseBuilder.toString();
		return response;
	}

	@GetMapping("/healthcheck")
	public ResponseEntity<?> healthcheck() {
		return ResponseEntity.ok(Map.of("status", "ok", "message", "Server is running"));
	}
}