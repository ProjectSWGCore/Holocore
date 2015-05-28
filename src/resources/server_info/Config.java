/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package resources.server_info;

import java.io.File;

/**
 * Reads and stores configuration data from a file
 */
public class Config {
	
	private final ConfigData configData;
	
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
		configData = new ConfigData(file);
		load();
		save();
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
		return configData.get(key);
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
	 * Gets the parameter with the specified key. If no such parameter exists,
	 * or if the value isn't a boolean then it returns the default
	 * @param key the key to get the value for
	 * @param def the default value
	 * @return the value represented by the key, or the default value
	 */
	public boolean getBoolean(String key, boolean def) {
		String val = getString(key, def?"true":"false");
		if (val.equalsIgnoreCase("true") || val.equals("1"))
			return true;
		if (val.equalsIgnoreCase("false") || val.equals("0"))
			return false;
		return def;
	}
	
	/**
	 * Sets the property value for the specified key
	 * @param key the key of the value to set
	 * @param value the value to set
	 */
	public void setProperty(String key, String value) {
		configData.put(key, value);
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
	public void setProperty(String key, boolean value) {
		setProperty(key, value ? "true" : "false");
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
		return configData.load();
	}
	
	/**
	 * Saves the config data to the file
	 * @return TRUE if the data was successfully saved, FALSE otherwise
	 */
	public boolean save() {
		return configData.save();
	}
	
}
