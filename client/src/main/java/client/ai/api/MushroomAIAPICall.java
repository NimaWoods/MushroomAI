package mushroomai.ai.api;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MushroomAIAPICall {

	private static final String BASE_URL = ApiConfig.getBaseUrl();
	public static HttpURLConnection connection = null;

	public void sendMessage(String message){

		try {
			URL chatURL = new URL(BASE_URL + "/chat/generate");
			connection = (HttpURLConnection) chatURL.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type",
					"application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Language", "de-DE");

			connection.setUseCaches(false);
			connection.setDoOutput(true);



		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

	}

}
