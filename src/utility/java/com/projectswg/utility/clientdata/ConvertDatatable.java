/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.utilities.SdbGenerator;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Generic converter that can convert a single .iff -> .sdb
 */
public class ConvertDatatable implements Converter {
	
	private final String inputDatatablePath;
	private final String outputSdbPath;
	private final boolean lowercaseColumnNames;
	
	public ConvertDatatable(String inputDatatablePath, String outputSdbPath, boolean lowercaseColumnNames) {
		this.inputDatatablePath = inputDatatablePath;
		this.outputSdbPath = outputSdbPath;
		this.lowercaseColumnNames = lowercaseColumnNames;
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
			
			if (lowercaseColumnNames) {
				columnName = columnName.toLowerCase(Locale.ROOT);
			}
			
			columnNames.add(columnName);
		}
		return columnNames;
	}
}
