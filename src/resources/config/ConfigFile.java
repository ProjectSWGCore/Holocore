package resources.config;

public enum ConfigFile {
	PRIMARY	("nge.cfg");
	
	private String filename;
	
	ConfigFile(String filename) {
		this.filename = filename;
	}
	
	public String getFilename() {
		return filename;
	}
}
