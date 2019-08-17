/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.utility.clientdata_printer;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.data.swgfile.visitors.DatatableData.ColumnType;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.VehicleLoader.VehicleInfo;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

public class ClientdataPrinterDatatable {
	
	private static final Pattern COLUMN_SPLITTER = Pattern.compile("_|(?<=[a-z])(?=[A-Z])");
	
	public static void main(String [] args) throws IOException {
		Log.addWrapper(new ConsoleLogWrapper());
		printTable("path/to/iff.iff");
	}
	
	@SuppressWarnings("UseOfSystemOutOrSystemErr")
	private static void printTable(String table) throws IOException {
		DatatableData data = (DatatableData) ClientFactory.getInfoFromFile(table);
		File outputFile = new File("serverdata/", table.replace("datatables/", "").replace(".iff", ".sdb"));
		try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8))) {
			for (int col = 0; col < data.getColumnCount(); col++) {
				if (col > 0)
					out.write('\t');
				out.write(toSdbColumn(data.getColumnName(col)));
			}
			out.newLine();
			for (int col = 0; col < data.getColumnCount(); col++) {
				if (col > 0)
					out.write('\t');
				out.write(data.getColumnType(col).toString());
			}
			out.newLine();
			for (int row = 0; row < data.getRowCount(); row++) {
				for (int col = 0; col < data.getColumnCount(); col++) {
					if (col > 0)
						out.write('\t');
					out.write(String.valueOf(data.getCell(row, col)));
				}
				out.newLine();
			}
		}
		
		String type = toJavaClass(table.substring(table.lastIndexOf('/')+1).replace(".iff", ""));
		
		System.out.printf("public final class %sLoader extends DataLoader {%n\t%n"+
				"\t%sLoader() {%n"+
				"\t\t%n"+
				"\t}%n", type, type);
		System.out.println("\t");
		
		// load() method
		System.out.printf("\t@Override%n"+
				"\tpublic final void load() throws IOException {%n"+
				"\t\ttry (SdbResultSet set = SdbLoader.load(new File(\"%s\"))) {%n"+
				"\t\t\twhile (set.next()) {%n"+
				"\t\t\t\t%sInfo %s = new %sInfo(set);%n"+
				"\t\t\t\t// TODO: Store information%n"+
				"\t\t\t}%n"+
				"\t\t}%n"+
				"\t}%n", outputFile, type, Character.toLowerCase(type.charAt(0))+type.substring(1), type);
		System.out.println("\t");
		
		System.out.printf("\tpublic static class %sInfo {%n", type);
		System.out.println("\t\t");
		for (int i = 0; i < data.getColumnCount(); i++) {
			System.out.printf("\t\tprivate final %s %s;%n", toJavaType(data.getColumnType(i)), toJavaVariable(data.getColumnName(i)));
		}
		System.out.println("\t\t");
		System.out.printf("\t\tpublic %sInfo(SdbResultSet set) {%n", type);
		for (int i = 0; i < data.getColumnCount(); i++) {
			String sdbCol = toSdbColumn(data.getColumnName(i));
			String rhs;
			switch (data.getColumnType(i)) {
				case BOOLEAN:
					rhs = "set.getBoolean(\""+sdbCol+"\")";
					break;
				case FLOAT:
					rhs = "set.getReal(\""+sdbCol+"\")";
					break;
				case CRC:
					rhs = "new CRC((int) set.getInt(\""+sdbCol+"\"))";
					break;
				case INTEGER:
					rhs = "(int) set.getInt(\""+sdbCol+"\")";
					break;
				case STRING:
				case ENUM:
				case DATATABLE_ENUM:
				case NONE:
				default:
					rhs ="set.getText(\""+sdbCol+"\")";
					break;
			}
			System.out.printf("\t\t\tthis.%s = %s;%n", toJavaVariable(data.getColumnName(i)), rhs);
		}
		System.out.println("\t\t}");
		System.out.println("\t}");
		System.out.println("}");
	}
	
	private static String toJavaType(ColumnType type) {
		switch (type) {
			case BOOLEAN:
				return "boolean";
			case FLOAT:
				return "double";
			case CRC:
				return "CRC";
			case INTEGER:
				return "int";
			case STRING:
			case ENUM:
			case DATATABLE_ENUM:
			case NONE:
			default:
				return "String";
		}
	}
	
	private static String toJavaVariable(String col) {
		String [] parts = COLUMN_SPLITTER.split(col);
		StringBuilder str = new StringBuilder();
		boolean first = true;
		for (String part : parts) {
			if (first)
				str.append(part.toLowerCase(Locale.US));
			else if (str.length() > 0)
				str.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.US));
			first = false;
		}
		return str.toString();
	}
	
	private static String toJavaClass(String col) {
		String [] parts = COLUMN_SPLITTER.split(col);
		StringBuilder str = new StringBuilder();
		for (String part : parts) {
			str.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase(Locale.US));
		}
		return str.toString();
	}
	
	private static String toSdbColumn(String col) {
		String [] parts = COLUMN_SPLITTER.split(col);
		return String.join("_", parts).toLowerCase(Locale.US);
	}
	
}
