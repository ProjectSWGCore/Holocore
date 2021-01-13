package com.projectswg.holocore.utilities;

import com.projectswg.common.data.CrcDatabase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

public class AddQuestsToCrcDatabase {
	public static void main(String[] args) throws IOException {
		Path questListPath = Paths.get("clientdata", "datatables", "questlist");
		Path questPath = Paths.get(questListPath.toString(), "quest");
		
		Collection<String> all = new ArrayList<>();
		all.addAll(getNames(questPath, "quest"));
		all.addAll(getNames(questPath, "groundquest"));
		all.addAll(getNames(questPath, "spacequest"));
		
		CrcDatabase instance = CrcDatabase.getInstance();
		
		for (String name : all) {
			instance.addCrc(name);
		}
		
		instance.saveStrings(new FileOutputStream("crc_database.csv"));
	}
	
	private static Collection<String> getNames(Path questPath, String subFolderName) {
		String[] list = questPath.toFile().list();
		Collection<String> names = new ArrayList<>();
		
		for (String s : list) {
			names.add(subFolderName + "/" + s.replace(".iff", ""));
		}
		
		return names;
	}
}
