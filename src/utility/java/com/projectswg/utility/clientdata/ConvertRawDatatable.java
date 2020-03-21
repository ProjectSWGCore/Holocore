package com.projectswg.utility.clientdata;

import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

public abstract class ConvertRawDatatable implements Converter {
	
	public ConvertRawDatatable() {
		
	}
	
	protected void convert(File input, File output) {
		try (BufferedReader in = new BufferedReader(new FileReader(input))) {
			String [] columnNames = in.readLine().split("\t");
			String [] columnTypes = in.readLine().split("\t");
			System.out.println(List.of(columnNames));
			System.out.println(List.of(columnTypes));
			try (SdbGenerator gen = new SdbGenerator(output)) {
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
