package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class ConvertAppearances implements Converter {
	
	private static final File CLIENTDATA = new File("clientdata");
	
	public ConvertAppearances() {
		
	}
	
	@Override
	public void convert() {
		System.out.println("Converting object data...");
		try (SdbGenerator sdb = new SdbGenerator(new File("serverdata/objects/object_appearances.sdb"))) {
			{
				List<String> columns = new ArrayList<>();
				columns.add("file");
				columns.add("collision");
				sdb.writeColumnNames(columns);
			}
			Converter.traverseFiles(this, new File(CLIENTDATA, "appearance"), sdb, f -> true);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) throws IOException {
		System.out.println(file);
		System.out.println(ClientFactory.getInfoFromFile(file));
//		AppearanceTemplateData appearance = (AppearanceTemplateData) ClientFactory.getInfoFromFile(file);
//		if (appearance == null) {
//			System.err.println("Failed to load appearance: " + file);
//			return;
//		}
	}
	
}
