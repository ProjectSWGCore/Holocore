/************************************************************************************
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
package resources.server_info;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.debug.Log;

public class SdbLoader {
	
	public SdbLoader() {
		
	}
	
	public SdbResultSet load(File file) throws IOException {
		SdbResultSet set = new SdbResultSet(file);
		set.load();
		return set;
	}
	
	private enum DataType {
		TEXT,
		INTEGER,
		REAL;
		
		public Object decode(String str) {
			switch (this) {
				case TEXT:
				default:
					return str;
				case INTEGER:
					return Long.parseLong(str);
				case REAL:
					return Double.parseDouble(str);
			}
		}
	}
	
	public static class SdbResultSet implements AutoCloseable {
		
		private final File file;
		private final Map<String, Integer> columnNames;
		private DataType [] columnTypes;
		private Object [] columnValues;
		private BufferedReader reader;
		
		private SdbResultSet(File file) {
			this.file = file;
			this.columnNames = new HashMap<>();
			this.columnTypes = null;
			this.columnValues = null;
			this.reader = null;
		}
		
		@Override
		public void close() throws IOException {
			if (reader != null)
				reader.close();
			reader = null;
		}
		
		public boolean next() throws IOException {
			String line;
			do {
				line = reader.readLine();
			} while (line != null && line.isEmpty());
			
			if (line != null) {
				readNextLine(line);
				return true;
			}
			return false;
		}
		
		public List<String> getColumns() {
			return new ArrayList<>(columnNames.keySet());
		}
		
		public Object getObject(int index) {
			return columnValues[index];
		}
		
		public String getText(int index) {
			return (String) columnValues[index];
		}
		
		public long getInt(int index) {
			return (Long) columnValues[index];
		}
		
		public double getReal(int index) {
			return (Double) columnValues[index];
		}
		
		public String getText(String columnName) {
			return getText(columnNames.get(columnName));
		}
		
		public long getInt(String columnName) {
			return getInt(columnNames.get(columnName));
		}
		
		public double getReal(String columnName) {
			return getReal(columnNames.get(columnName));
		}
		
		private void readNextLine(String line) {
			int prevIndex = 0;
			int nextIndex = 0;
			int i = 0;
			for (i = 0; i < columnTypes.length-1; i++) {
				nextIndex = line.indexOf('\t', prevIndex);
				columnValues[i] = columnTypes[i].decode(line.substring(prevIndex, nextIndex));
				prevIndex = nextIndex+1;
			}
			columnValues[i] = columnTypes[i].decode(line.substring(prevIndex));
		}
		
		protected void load() throws IOException {
			reader = new BufferedReader(new FileReader(file));
			if (!loadHeader(reader.readLine(), reader.readLine()))
				return;
		}
		
		private boolean loadHeader(String columnsStr, String typesStr) {
			if (columnsStr == null || typesStr == null) {
				Log.e("Invalid SDB header: %s - nonexistent", file);
				return false;
			}
			String [] columns = columnsStr.split("\t");
			String [] types = typesStr.split("\t");
			if (columns.length != types.length) {
				Log.e("Invalid SDB header: %s - invalid lengths!", file);
				return false;
			}
			columnTypes = new DataType[columns.length];
			columnValues = new Object[columns.length];
			for (int i = 0; i < columns.length; i++) {
				columnTypes[i] = parseColumnType(types[i]);
				columnNames.put(columns[i], i);
			}
			return true;
		}
		
		private DataType parseColumnType(String type) {
			if (type.indexOf(' ') != -1)
				type = type.substring(0, type.indexOf(' '));
			if (type.equalsIgnoreCase("TEXT"))
				return DataType.TEXT;
			if (type.equalsIgnoreCase("INTEGER"))
				return DataType.INTEGER;
			if (type.equalsIgnoreCase("REAL"))
				return DataType.REAL;
			Log.e("Unknown column type: %s for file %s", type, file);
			return DataType.TEXT;
		}
		
	}
	
}
