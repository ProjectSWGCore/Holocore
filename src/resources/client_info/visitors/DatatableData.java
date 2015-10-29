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
package resources.client_info.visitors;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

import java.util.Arrays;
import java.util.Locale;

public class DatatableData extends ClientData {
	
	private String[] columnNames;
	private String[] columnTypes;
	private Object[][] table;
	//private Map<String, Integer> enums;
	
	@Override
	public void readIff(SWGFile iff) {
		iff.enterNextForm(); // version form

		IffNode chunk;
		while((chunk = iff.enterNextChunk()) != null) {
			switch(chunk.getTag()) {
				case "COLS":
					columnNames = new String[chunk.readInt()];
					for (int i = 0; i < columnNames.length; i++) {
						columnNames[i] = chunk.readString();
					}
					break;
				case "TYPE":
					parseTypes(chunk);
					break;
				case "ROWS":
					parseRows(chunk);
					break;
				default: break;
			}
		}
	}

	@Override
	public void writeIff(SWGFile iff) {
		iff.addForm("0001");
		writeColumns(iff.addChunk("COLS"));
		writeTypes(iff.addChunk("TYPE"));
		writeRows(iff.addChunk("ROWS"));
	}

	private void writeColumns(IffNode chunk) {
		int size = getTableStringSize(columnNames);

		chunk.initWriteBuffer(size + 4);

		chunk.writeInt(columnNames.length);
		for (String columnName : columnNames) {
			chunk.writeString(columnName);
		}

		chunk.updateChunk();
	}

	private void writeTypes(IffNode chunk) {
		int size = getTableStringSize(columnTypes);

		chunk.initWriteBuffer(size);

		for (String columnType : columnTypes) {
			chunk.writeString(columnType);
		}

		chunk.updateChunk();
	}

	private void writeRows(IffNode chunk) {
		int size = 0;
		int rows = table.length;

		for (int i = 0; i < columnTypes.length; i++) {
			String type = columnTypes[i];
			switch(type) {
				case "b":
				case "f":
				case "h":
				case "i": size += 4 * rows; break;
				case "s": {
					for (int r = 0; r < rows; r++) {
						size += ((String) table[r][i]).length() + 1;
					}
					break;
				}
				default: System.err.println("Cannot write row type " + type);
			}
		}
		chunk.initWriteBuffer(size + 4);

		chunk.writeInt(rows);

		for (int r = 0; r < rows; r++) {
			for (int t = 0; t < columnTypes.length; t++) {
				String type = columnTypes[t];
				switch(type) {
					case "b":
					case "h":
					case "i": chunk.writeInt((Integer) table[r][t]); break;
					case "f": chunk.writeFloat((Float) table[r][t]); break;
					case "s": chunk.writeString((String) table[r][t]); break;
					default: System.err.println("Cannot write datatable to type " + type);
				}
			}
		}
		chunk.updateChunk();
	}

	private void parseTypes(IffNode chunk) {
		columnTypes = new String[columnNames.length];

		for (int t = 0; t < columnTypes.length; t++) {
			String type = chunk.readString();
			
			if (type.contains("["))
				type = type.split("\\[")[0];
			
			if (type.contains("(")) // relevant only to enums
				type = type.split("\\(")[0];
			
			columnTypes[t] = type.toLowerCase(Locale.ENGLISH);
/* TODO: Need to come up with a better way of doing enums. Example of what this type looks like:
* e(RIFLE=0,CARBINE=1,PISTOL=2,HEAVY=3,1HAND_MELEE=4,2HAND_MELEE=5,UNARMED=6,POLEARM=7,THROWN=8,1HAND_LIGHTSABER=9,2HAND_LIGHTSABER=10,POLEARM_LIGHTSABER=11)
			if (type.startsWith("e(")) {
				// Cleanup string and put enum inside the enums map
				String enumEntries = type;
				enumEntries = type.replace("e(", "").replace(")", "");
			}
*/
		}
	}
	
	private void parseRows(IffNode chunk) {
		int rows = chunk.readInt();
		table = new Object[rows][columnTypes.length];
		
		for (int r = 0; r < rows; r++) {
			// Cell within row
			for (int t = 0; t < columnTypes.length; t++) {
				String type = columnTypes[t];
				
				switch(type) {
				
				case "b": // Boolean
					table[r][t] = (chunk.readInt() == 1);
					break;
					
				case "e": // Enumerator
					table[r][t] = chunk.readInt();
					break;
					
				case "f": // Float
					table[r][t] = chunk.readFloat();
					break;
				
				case "h": // CRC
					table[r][t] = chunk.readUInt();
					break;
					
				case "i": // Integer
					table[r][t] = chunk.readInt();
					break;
				
				case "s": // String
					if (chunk.readByte() != 0) {
						chunk.skip(-1);
						table[r][t] = chunk.readString();
					} else {
						table[r][t] = "";
					}
					
					break;
					
				case "v":
					// TODO: Datatable Enums
					chunk.readInt();
					break;
				default:
					System.err.println("FATAL: Don't know how to decode type " + type + " in row " + r + " column " + t);
					break;
				}
			}
		}
	}
	
	public int getRowCount() {
		return table == null ? 0 : table.length;
	}
	
	public int getColumnCount() {
		if(table == null)
			return 0;
		return columnNames.length;
	}
	
	public Object[] getRowsByColumnName(String columnName) {
		
		for(int i = 0; i < table.length; i++) {
			for(int j=0; j < columnTypes.length; ++j) {
				String currentName = columnNames[j];
				if(currentName.equals(columnName))
					return table[i];
			}
		}
		return null;
	}
	
	public Object [] getRow(int row) {
		return table[row];
	}
	
	public Object getCell(int row, int column) {
		return table[row][column];
	}
	
	public Object getCell(String columnName, int rowIndex) {
		for(int j=0; j < columnTypes.length; ++j) {
			String currentName = columnNames[j];
			if(currentName.equals(columnName))
				return table[rowIndex][j];
		}
		return null;
	}
	
	public String getColumnName(int column) {
		if (column < 0 || column >= columnNames.length)
			return null;
		return columnNames[column];
	}
	
	public String getColumnType(int column) {
		if (column < 0 || column >= columnTypes.length)
			return null;
		return columnTypes[column];
	}

	public int getColumnFromName(String name){
		return Arrays.asList(columnNames).indexOf(name);
	}

	public void handleRows(DatatableRowHandler handler) {
		for (int r = 0; r < getRowCount(); r++) {
			handler.handleRow(r);
		}
	}

	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
	}

	public void setColumnTypes(String[] columnTypes) {
		this.columnTypes = columnTypes;
	}

	public void setTable(Object[][] table) {
		this.table = table;
	}

	private int getTableStringSize(String[] table) {
		int size = 0;
		for (String s : table) {
			size += s.length() + 1;
		}
		return size;
	}

	public interface DatatableRowHandler {
		void handleRow(int row);
	}
}
