package resources.server_info;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
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
		return data.containsKey(key);
	}
	
	public String get(String key) {
		return data.get(key);
	}
	
	public String put(String key, String value) {
		return data.put(key, value);
	}
	
	public boolean load() {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			String line = reader.readLine();
			while (line != null) {
				loadLine(line);
				line = reader.readLine();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					
				}
			}
		}
	}
	
	public boolean save() {
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(file));
			writer.write("# "+FORMAT.format(System.currentTimeMillis()));
			writer.newLine();
			for (Entry <String, String> e : data.entrySet()) {
				writer.write(e.getKey() + "=" + e.getValue());
				writer.newLine();
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
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
	
	private void loadLine(String line) {
		String beforeComment = line;
		if (line.contains("#"))
			beforeComment = line.substring(0, line.indexOf('#'));
		if (!beforeComment.contains("="))
			return;
		String key = beforeComment.substring(0, beforeComment.indexOf('='));
		String val = beforeComment.substring(key.length()+1);
		data.put(key, val);
	}
	
}
