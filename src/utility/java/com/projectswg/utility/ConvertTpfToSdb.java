package com.projectswg.utility;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConvertTpfToSdb {
	
	private static final Set<String> ATTRIBUTES = new HashSet<>();
	private static final Map<String, Map<String, Object>> OBJECTS = new HashMap<>();
	private static final File ROOT = new File("/home/josh/devel/ProjectSWG/sauce/whitengold/dsrc/sku.0/sys.shared/compiled/game/");
	
	public static void main(String [] args) {
		// dsrc/sku.0/sys.server/compiled/game/object/building/tatooine/housing_tatt_style01_large.tpf
		File root = new File(ROOT, "object");
		scan(root);
		System.out.println();
		System.out.println();
		System.out.println();
		System.out.println(ATTRIBUTES);
		System.out.println(OBJECTS.get("object/creature/player/shared_human_male.tpf"));
	}
	
	private static void scan(File directory) {
		File [] children = directory.listFiles();
		assert children != null;
		for (File child : children) {
			if (child.isDirectory()) {
				scan(child);
			} else if (child.getName().endsWith(".tpf")) {
				load(child);
			} else {
				System.out.println("Ignoring: " + child);
			}
		}
	}
	
	private static void load(File file) {
		String path = getPath(file);
		if (OBJECTS.containsKey(path))
			return;
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line;
			Map<String, Object> data = new HashMap<>();
			OBJECTS.put(path, data);
			while ((line = reader.readLine()) != null) {
				int commentIndex = line.indexOf("//");
				if (commentIndex != -1)
					line = line.substring(0, commentIndex);
				line = line.trim();
				if (line.isEmpty())
					continue;
				if (line.startsWith("@")) {
					if (line.startsWith("@base")) {
						String parent = line.split(" ", 2)[1];
						parent = parent.replace(".iff", ".tpf").trim();
						load(new File(ROOT, parent));
						if (OBJECTS.containsKey(parent))
							data.putAll(OBJECTS.get(parent));
						else
							System.err.println("No data for " + parent);
					}
					continue;
				}
				String [] parts = line.split("=", 2);
				if (parts.length == 1) {
					System.err.println("Invalid line: " + Arrays.toString(parts));
					continue;
				}
				String key = parts[0].trim();
				String value = parts[1].trim();
				if (value.startsWith("[") || value.startsWith("+")) {
					StringBuilder array = new StringBuilder(value);
					String arrayLine;
					while ((arrayLine = reader.readLine()) != null) {
						arrayLine = arrayLine.trim();
						array.append(arrayLine);
						int bracketCount = 0;
						boolean quote = false;
						for (int i = 0; i < array.length(); i++) {
							switch (array.charAt(i)) {
								case '"':
									quote = !quote;
									break;
								case '[':
									if (!quote)
										bracketCount++;
									break;
								case ']':
									if (!quote)
										bracketCount--;
									break;
							}
						}
						if (bracketCount == 0)
							break;
					}
					value = array.toString();
				}
				data.put(key, value);
			}
			ATTRIBUTES.addAll(data.keySet());
		} catch (IOException e) {
			System.err.println("Failed to read " + file + "  " + e.getMessage());
		}
	}
	
	private static String getPath(File file) {
		return file.getAbsolutePath().replace(ROOT.getAbsolutePath()+'/', "");
	}
	
}
