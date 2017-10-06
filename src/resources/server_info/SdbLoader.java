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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.debug.Log;

public class SdbLoader {
	
	private SdbLoader() {
		
	}
	
	private enum DataType {
		TEXT,
		INTEGER,
		REAL,
		BOOLEAN;
		
		public Object decode(String str) {
			switch (this) {
				case TEXT:
				default:
					return str;
				case INTEGER:
					return Long.parseLong(str);
				case REAL:
					return Double.parseDouble(str);
				case BOOLEAN:
					return str.equalsIgnoreCase("true") || str.equals("1");
			}
		}
	}
	
	public static SdbResultSet load(File file) throws IOException {
		String ext = getExtension(file);
		if (ext.equals("msdb")) {
			return MasterSdbResultSet.load(file);
		} else if (ext.equals("sdb")) {
			return SingleSdbResultSet.load(file);
		} else {
			throw new IllegalArgumentException("Invalid file! Expected either msdb or sdb");
		}
	}
	
	private static String getExtension(File file) {
		String ext = file.getName().toLowerCase(Locale.US);
		int lastPeriod = ext.lastIndexOf('.');
		if (lastPeriod == -1)
			return ext;
		return ext.substring(lastPeriod+1);
	}
	
	public interface SdbResultSet extends AutoCloseable {
		
		@Override
		void close() throws IOException;
		boolean next() throws IOException;
		List<String> getColumns();
		
		Object getObject(int index);
		
		String getText(int index);
		String getText(String columnName);
		
		long getInt(int index);
		long getInt(String columnName);
		
		double getReal(int index);
		double getReal(String columnName);
		
		boolean getBoolean(int index);
		boolean getBoolean(String columnName);
	}
	
	private static class MasterSdbResultSet implements SdbResultSet {
		
		private final Iterator<SdbResultSet> sdbs;
		private final AtomicReference<SdbResultSet> sdb;
		
		private MasterSdbResultSet(Iterator<SdbResultSet> sdbs) {
			this.sdbs = sdbs;
			this.sdb = new AtomicReference<>(sdbs.hasNext() ? sdbs.next() : null);
		}
		
		@Override
		public void close() throws IOException {
			SdbResultSet set = getResultSet();
			if (set != null)
				set.close();
		}
		
		@Override
		public boolean next() throws IOException {
			SdbResultSet set = getResultSet();
			if (set == null)
				return false;
			while (!set.next()) {
				set.close();
				if (!sdbs.hasNext()) {
					sdb.set(null);
					return false; // bummer
				}
				set = sdbs.next();
				sdb.set(set);
				if (set == null)
					return false; // shouldn't be possible anyways
			}
			return true;
		}
		
		@Override
		public List<String> getColumns() {
			return getResultSet().getColumns();
		}
		
		@Override
		public Object getObject(int index) {
			return getResultSet().getObject(index);
		}
		
		@Override
		public String getText(int index) {
			return getResultSet().getText(index);
		}
		
		@Override
		public String getText(String columnName) {
			return getResultSet().getText(columnName);
		}
		
		@Override
		public long getInt(int index) {
			return getResultSet().getInt(index);
		}
		
		@Override
		public long getInt(String columnName) {
			return getResultSet().getInt(columnName);
		}
		
		@Override
		public double getReal(int index) {
			return getResultSet().getReal(index);
		}
		
		@Override
		public double getReal(String columnName) {
			return getResultSet().getReal(columnName);
		}
		
		@Override
		public boolean getBoolean(int index) {
			return getResultSet().getBoolean(index);
		}
		
		@Override
		public boolean getBoolean(String columnName) {
			return getResultSet().getBoolean(columnName);
		}
		
		private SdbResultSet getResultSet() {
			return sdb.get();
		}
		
		private static MasterSdbResultSet load(File file) throws IOException {
			List<SdbResultSet> sets = new ArrayList<>();
			File parentFile = file.getParentFile();
			try (SingleSdbResultSet msdb = SingleSdbResultSet.load(file)) {
				while (msdb.next()) {
					if (msdb.getBoolean(1)) // is enabled
						sets.add(SdbLoader.load(new File(parentFile, msdb.getText(0)))); // relative file path
				}
			}
			return new MasterSdbResultSet(sets.iterator());
		}
		
	}
	
	private static class SingleSdbResultSet implements SdbResultSet {
		
		private final File file;
		private final Map<String, Integer> columnNames;
		private final AtomicLong lineNumber;
		private DataType [] columnTypes;
		private Object [] columnValues;
		private BufferedReader reader;
		
		private SingleSdbResultSet(File file) {
			this.file = file;
			this.columnNames = new HashMap<>();
			this.lineNumber = new AtomicLong(0);
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
		
		@Override
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
		
		@Override
		public List<String> getColumns() {
			return new ArrayList<>(columnNames.keySet());
		}
		
		@Override
		public Object getObject(int index) {
			return columnValues[index];
		}
		
		@Override
		public String getText(int index) {
			return (String) columnValues[index];
		}
		
		@Override
		public String getText(String columnName) {
			return getText(columnNames.get(columnName));
		}
		
		@Override
		public long getInt(int index) {
			return (Long) columnValues[index];
		}
		
		@Override
		public long getInt(String columnName) {
			return getInt(columnNames.get(columnName));
		}
		
		@Override
		public double getReal(int index) {
			return (Double) columnValues[index];
		}
		
		@Override
		public double getReal(String columnName) {
			return getReal(columnNames.get(columnName));
		}
		
		@Override
		public boolean getBoolean(int index) {
			return (Boolean) columnValues[index];
		}
		
		@Override
		public boolean getBoolean(String columnName) {
			return getBoolean(columnNames.get(columnName));
		}
		
		private void readNextLine(String line) {
			long lineNum = lineNumber.incrementAndGet();
			int prevIndex = 0;
			int nextIndex = 0;
			int i = 0;
			try {
				for (i = 0; i < columnTypes.length-1; i++) {
					nextIndex = line.indexOf('\t', prevIndex);
					columnValues[i] = columnTypes[i].decode(line.substring(prevIndex, nextIndex));
					prevIndex = nextIndex+1;
				}
				columnValues[i] = columnTypes[i].decode(line.substring(prevIndex));
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNum + " in column " + (i+1));
			}
		}
		
		private static SingleSdbResultSet load(File file) throws IOException {
			SingleSdbResultSet sdb = new SingleSdbResultSet(file);
			sdb.load();
			return sdb;
		}
		
		private void load() throws IOException {
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
			lineNumber.set(2);
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
			if (type.equalsIgnoreCase("BOOL") || type.equalsIgnoreCase("BOOLEAN"))
				return DataType.BOOLEAN;
			Log.e("Unknown column type: %s for file %s", type, file);
			return DataType.TEXT;
		}
		
	}
	
}
