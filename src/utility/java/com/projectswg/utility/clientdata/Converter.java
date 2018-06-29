package com.projectswg.utility.clientdata;

import com.projectswg.utility.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.function.Predicate;

interface Converter {
	
	File HOLOCORE = new File("serverdata");
	File RAW = new File("/home/josh/devel/ProjectSWG/sauce/whitengold/dsrc/sku.0/sys.shared/compiled/game/");
	
	void convert();
	default void convertFile(SdbGenerator sdb, File file) throws IOException {}
	
	static void traverseFiles(Converter converter, File directory, SdbGenerator sdb, Predicate<File> filter) {
		System.out.println(directory);
		File [] children = directory.listFiles();
		assert children != null;
		for (File child : children) {
			if (child.isDirectory()) {
				traverseFiles(converter, child, sdb, filter);
			} else if (filter.test(child)) {
				try {
					converter.convertFile(sdb, child);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Ignoring " + child);
			}
		}
	}
	
}
