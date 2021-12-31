package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.utilities.SdbGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Generic converter that can convert .iff -> .sdb
 */
public class ConvertDatatable implements Converter {
	
	private final String inputDatatablePath;
	private final String outputSdbPath;
	
	public ConvertDatatable(String inputDatatablePath, String outputSdbPath) {
		this.inputDatatablePath = inputDatatablePath;
		this.outputSdbPath = outputSdbPath;
	}
	
	@Override
	public void convert() {
		System.out.println("Converting " + inputDatatablePath + " to " + outputSdbPath + "...");
		
		try (SdbGenerator sdb = new SdbGenerator(new File(outputSdbPath))) {
			DatatableData datatable = (DatatableData) ClientFactory.getInfoFromFile(inputDatatablePath);
			Objects.requireNonNull(datatable, "Failed to load datatable");
			
			transferColumnNames(sdb, datatable);
			transferRows(sdb, datatable);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void transferRows(SdbGenerator sdb, DatatableData datatable) throws IOException {
		int rowCount = datatable.getRowCount();
		
		for (int i = 0; i < rowCount; i++) {
			Object[] row = datatable.getRow(i);
			
			sdb.writeLine(row);
		}
	}
	
	private void transferColumnNames(SdbGenerator sdb, DatatableData datatable) throws IOException {
		List<String> columnNames = getColumnNamesFromDatatable(datatable);
		
		sdb.writeColumnNames(columnNames);
	}
	
	@NotNull
	private List<String> getColumnNamesFromDatatable(DatatableData datatable) {
		int columnCount = datatable.getColumnCount();
		
		List<String> columnNames = new ArrayList<>();
		
		for (int i = 0; i < columnCount; i++) {
			String columnName = datatable.getColumnName(i);
			
			columnNames.add(columnName);
		}
		return columnNames;
	}
}
