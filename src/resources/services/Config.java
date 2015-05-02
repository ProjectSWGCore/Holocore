package resources.services;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

/**
 * Reads and stores configuration data from a file
 */
public class Config {
	
	private final Properties configData;
	private File file;
	
	/**
	 * Creates an empty config that isn't associated with a file
	 */
	public Config() {
		this.file = null;
		this.configData = new Properties();
	}
	
	/**
	 * Initilizes the Config and loads the data in the file
	 * @param filename the file to load
	 */
	public Config(String filename) {
		this(new File(filename));
	}
	
	/**
	 * Initilizes the Config and loads the data in the file
	 * @param file the file to load
	 */
	public Config(File file) {
		if (!file.exists() || !file.isFile())
			throw new IllegalArgumentException("Filepath does not point to a valid file!");
		this.file = file;
		this.configData = new Properties();
		load();
	}
	
	/**
	 * Determines whether or not the key-value pair exists in the config
	 * @param key the key to check
	 * @return TRUE if the key-value pair exists, FALSE otherwise
	 */
	public boolean containsKey(String key) {
		return configData.containsKey(key);
	}
	
	/**
	 * Gets the parameter with the specified key. If no such parameter exists,
	 * it returns the default
	 * @param key the key to get the value for
	 * @param def the default value
	 * @return the value represented by the key, or the default value
	 */
	public String getString(String key, String def) {
		if (!containsKey(key)) {
			setProperty(key, def);
			return def;
		}
		return configData.getProperty(key);
	}
	
	/**
	 * Gets the parameter with the specified key. If no such parameter exists,
	 * or if the value isn't an integer then it returns the default
	 * @param key the key to get the value for
	 * @param def the default value
	 * @return the value represented by the key, or the default value
	 */
	public int getInt(String key, int def) {
		try {
			return Integer.parseInt(getString(key, Integer.toString(def)));
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	/**
	 * Gets the parameter with the specified key. If no such parameter exists,
	 * or if the value isn't a double then it returns the default
	 * @param key the key to get the value for
	 * @param def the default value
	 * @return the value represented by the key, or the default value
	 */
	public double getDouble(String key, double def) {
		try {
			return Double.parseDouble(getString(key, Double.toString(def)));
		} catch (NumberFormatException e) {
			return def;
		}
	}
	
	/**
	 * Sets the property value for the specified key
	 * @param key the key of the value to set
	 * @param value the value to set
	 */
	public void setProperty(String key, String value) {
		configData.setProperty(key, value);
		save();
	}
	
	/**
	 * Sets the property value for the specified key
	 * @param key the key of the value to set
	 * @param value the value to set
	 */
	public void setProperty(String key, int value) {
		setProperty(key, Integer.toString(value));
	}
	
	/**
	 * Sets the property value for the specified key
	 * @param key the key of the value to set
	 * @param value the value to set
	 */
	public void setProperty(String key, double value) {
		setProperty(key, Double.toString(value));
	}
	
	/**
	 * Reloads the config data from the file
	 * @return TRUE if the data was successfully loaded, FALSE otherwise
	 */
	public boolean load() {
		if (file == null)
			return false;
		FileReader reader = null;
		try {
			reader = new FileReader(file);
			synchronized (reader) {
				configData.clear();
				configData.load(reader);
				reader.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
	
	/**
	 * Saves the config data to the file
	 * @return TRUE if the data was successfully saved, FALSE otherwise
	 */
	public boolean save() {
		if (file == null)
			return false;
		FileWriter writer = null;
		try {
			writer = new FileWriter(file);
			synchronized (writer) {
				configData.store(writer, null);
				writer.close();
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (writer != null)
					writer.close();
			} catch (IOException e) {
				return false;
			}
		}
		return false;
	}
	
}
