package server;

public class Constants {
	public enum PATHS {
		BASE_FOLDER("server/src/main/java/server/"),
		PYTHON_FOLDER(BASE_FOLDER + "python/"),
		MODEL_FOLDER(BASE_FOLDER + "model/"),
		REQUIREMENTS_FILE(PYTHON_FOLDER + "requirements.txt");

		private final String path;

		PATHS(String path) {
			this.path = path;
		}

		public String getPath() {
			return path;
		}
	}
}
