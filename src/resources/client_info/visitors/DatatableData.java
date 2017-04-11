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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

import com.projectswg.common.debug.Log;

public class DatatableData extends ClientData {
	
	private final Map<String, Integer> nameToIndex;
	private String [] columnNames;
	private ColumnType [] columnTypes;
	private Object [][] table;
	
	public DatatableData() {
		this.nameToIndex = new HashMap<>();
		this.columnNames = null;
		this.columnTypes = null;
		this.table = null;
	}
	
	@Override
	public void readIff(SWGFile iff) {
		iff.enterNextForm(); // version form
		
		IffNode chunk;
		while ((chunk = iff.enterNextChunk()) != null) {
			switch (chunk.getTag()) {
				case "COLS": {
					String[] columnNames = new String[chunk.readInt()];
					for (int i = 0; i < columnNames.length; i++) {
						columnNames[i] = chunk.readString();
					}
					setColumnNames(columnNames);
					break;
				}
				case "TYPE":
					parseTypes(chunk);
					break;
				case "ROWS":
					parseRows(chunk);
					break;
				default:
					break;
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
		int size = getColumnCount() * 2; // single char + \0
		
		chunk.initWriteBuffer(size);
		
		for (ColumnType columnType : columnTypes) {
			chunk.writeString(columnType.getString());
		}
		
		chunk.updateChunk();
	}
	
	private void writeRows(IffNode chunk) {
		int rows = getRowCount();
		int cols = getColumnCount();
		
		chunk.initWriteBuffer(getTableContentSize());
		chunk.writeInt(rows);
		
		for (int r = 0; r < rows; ++r) {
			for (int c = 0; c < cols; ++c) {
				switch (getColumnType(c)) {
					case BOOLEAN:
					case CRC:
					case INTEGER:
					case DATATABLE_ENUM:
					case ENUM:
						chunk.writeInt((Integer) getCell(r, c));
						break;
					case FLOAT:
						chunk.writeFloat((Float) getCell(r, c));
						break;
					case STRING:
						chunk.writeString((String) getCell(r, c));
						break;
					case NONE:
						break;
				}
			}
		}
		chunk.updateChunk();
	}
	
	private void parseTypes(IffNode chunk) {
		int cols = getColumnCount();
		ColumnType [] columnTypes = new ColumnType[cols];
		
		for (int c = 0; c < cols; c++) {
			columnTypes[c] = ColumnType.getForChar(chunk.readString());
		}
		
		setColumnTypes(columnTypes);
	}
	
	private void parseRows(IffNode chunk) {
		int rows = chunk.readInt();
		int cols = getColumnCount();
		
		Object [][] table = new Object[rows][getColumnCount()];
		for (int r = 0; r < rows; ++r) {
			for (int t = 0; t < cols; ++t) {
				table[r][t] = parseObject(chunk, getColumnType(t));
			}
		}
		setTable(table);
	}
	
	private Object parseObject(IffNode chunk, ColumnType type) {
		switch (type) {
			case BOOLEAN: // Boolean
				return chunk.readInt() == 1;
			case FLOAT: // Float
				return chunk.readFloat();
			case ENUM: // Enumerator
			case CRC: // CRC
			case INTEGER: // Integer
			case DATATABLE_ENUM: // TODO: Datatable Enums
				return chunk.readInt();
			case STRING: // String
				return chunk.readString();
			case NONE:
				return null;
		}
		return null;
	}
	
	public int getRowCount() {
		return table == null ? 0 : table.length;
	}
	
	public int getColumnCount() {
		return columnNames == null ? 0 : columnNames.length;
	}
	
	public Object[] getRow(int row) {
		return table[row];
	}
	
	public Object getCell(int row, int column) {
		return table[row][column];
	}
	
	public Object getCell(int row, String columnName) {
		return getCell(row, nameToIndex.get(columnName).intValue());
	}
	
	public String getString(int row, String columnName) {
		return (String) getCell(row, nameToIndex.get(columnName).intValue());
	}
	
	public float getFloat(int row, String columnName) {
		return (float) getCell(row, nameToIndex.get(columnName).intValue());
	}
	
	public int getInt(int row, String columnName) {
		return (int) getCell(row, nameToIndex.get(columnName).intValue());
	}
	
	public String getColumnName(int column) {
		if (column < 0 || column >= getColumnCount())
			return null;
		return columnNames[column];
	}
	
	public ColumnType getColumnType(int column) {
		if (column < 0 || column >= getColumnCount())
			return ColumnType.NONE;
		return columnTypes[column];
	}
	
	public int getColumnFromName(String columnName) {
		Integer column = nameToIndex.get(columnName);
		return column == null ? -1 : column;
	}
	
	public void handleRows(DatatableRowHandler handler) {
		for (int r = 0; r < getRowCount(); r++) {
			handler.handleRow(r);
		}
	}
	
	public void setColumnNames(String[] columnNames) {
		this.columnNames = columnNames;
		nameToIndex.clear();
		for (int i = 0; i < columnNames.length; i++) {
			nameToIndex.put(columnNames[i], i);
		}
	}
	
	public void setColumnTypes(String [] columnTypes) {
		ColumnType [] newTypes = new ColumnType[columnTypes.length];
		for (int i = 0; i < columnTypes.length; i++) {
			newTypes[i] = ColumnType.getForChar(columnTypes[i]);
		}
		setColumnTypes(newTypes);
	}
	
	public void setColumnTypes(ColumnType [] columnTypes) {
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
	
	private int getTableContentSize() {
		int size = 0;
		int rows = getRowCount();
		for (int i = 0; i < getColumnCount(); i++) {
			switch (getColumnType(i)) {
				case BOOLEAN:
				case FLOAT:
				case CRC:
				case INTEGER:
				case DATATABLE_ENUM:
				case ENUM:
					size += 4 * rows;
					break;
				case STRING:
					for (int r = 0; r < rows; r++) {
						size += ((String) table[r][i]).length() + 1;
					}
					break;
				case NONE:
					Log.e("Cannot write row type %s", getColumnType(i));
					break;
			}
		}
		return size + 4;
	}
	
	public interface DatatableRowHandler {
		void handleRow(int row);
	}
	
	public enum ColumnType {
		BOOLEAN			('b'),
		FLOAT			('f'),
		CRC				('h'),
		INTEGER			('i'),
		STRING			('s'),
		ENUM			('e'),
		DATATABLE_ENUM	('v'),
		NONE			(' ');
		
		private static final ColumnType [] VALUES = values();
		
		private final char c;
		
		ColumnType(char c) {
			this.c = c;
		}
		
		public char getChar() {
			return c;
		}
		
		public String getString() {
			return Character.toString(c);
		}
		
		public static ColumnType getForChar(String str) {
			str = str.toLowerCase(Locale.ENGLISH);
			
			if (str.indexOf('[') != -1)
				str = str.substring(0, str.indexOf('['));
			
			if (str.indexOf('(') != -1) // relevant only to enums
				str = str.substring(0, str.indexOf('('));
			
			if (str.length() != 1)
				return NONE;
			return getForChar(str.charAt(0));
		}
		
		public static ColumnType getForChar(char c) {
			for (ColumnType type : VALUES) {
				if (type.getChar() == c)
					return type;
			}
			return NONE;
		}
	}
}
