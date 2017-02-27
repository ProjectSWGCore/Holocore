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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

class ConfigData {
	
	private final DateFormat FORMAT = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss zzz");
	private final Map<String, String> data;
	private final File file;
	
	public ConfigData(File file) {
		this.data = new TreeMap<>();
		this.file = file;
	}
	
	public boolean containsKey(String key) {
		synchronized (data) {
			return data.containsKey(key);
		}
	}
	
	public String get(String key) {
		synchronized (data) {
			return data.get(key);
		}
	}
	
	public String put(String key, String value) {
		synchronized (data) {
			return data.put(key, value);
		}
	}
	
	/**
	 * @return null on an I/O failure, an empty {@code Map} on the first load and
	 * a populated {@code Map} when called afterwards
	 */
	public Map<String, String> load() {
		Map<String, String> delta = new HashMap<>();
		BufferedReader reader = null;
		
		synchronized (data) {
			delta.putAll(data);	// Copy the current data
			
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
				String line = reader.readLine();
				while (line != null) {
					loadLine(line);
					line = reader.readLine();
				}
			} catch (IOException e) {
				Log.e(e);
				return null;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						return null;
					}
				}
			}
			for(Entry<String, String> entry : data.entrySet())
				delta.remove(entry.getKey(), entry.getValue());
		}
		
		return delta;
	}
	
	public boolean save() {
		BufferedWriter writer = null;
		synchronized (data) {
			try {
				writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
				writer.write("# "+FORMAT.format(System.currentTimeMillis()));
				writer.newLine();
				for (Entry <String, String> e : data.entrySet()) {
					writer.write(e.getKey() + "=" + e.getValue());
					writer.newLine();
				}
				return true;
			} catch (IOException e) {
				Log.e(e);
				return false;
			} finally {
				if (writer != null) {
					try {
						writer.close();
					} catch (IOException e) {
						
					}
				}
			}
		}
	}
	
	private void loadLine(String line) {
		String beforeComment = line;
		if (line.contains("#"))
			beforeComment = line.substring(0, line.indexOf('#'));
		if (!beforeComment.contains("="))
			return;
		String key = beforeComment.substring(0, beforeComment.indexOf('='));
		String val = beforeComment.substring(key.length()+1);
		synchronized (data) {
			data.put(key, val);
		}
	}
	
}
