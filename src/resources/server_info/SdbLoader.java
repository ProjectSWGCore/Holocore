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

import com.projectswg.common.debug.Log;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class SdbLoader {
	
	private SdbLoader() {
		
	}
	
	private enum DataType {
		TEXT	(str -> str),
		INTEGER	(Long::valueOf),
		REAL	(Double::valueOf),
		BOOLEAN	(Boolean::valueOf);
		
		private final Function<String, Object> transformer;
		
		DataType(Function<String, Object> transformer) {
			this.transformer = transformer;
		}
		
		public Function<String, Object> getTransformer() {
			return transformer;
		}
	}
	
	public static SdbResultSet load(File file) throws IOException {
		String ext = getExtension(file);
		switch (ext) {
			case "msdb":
				return MasterSdbResultSet.load(file);
			case "sdb":
				return SingleSdbResultSet.load(file);
			default:
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
		
		<T> T getObject(int index);
		
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
		public <T> T getObject(int index) {
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
		private final BasicStringBuilder lineBuffer;
		private Function<String, Object> [] columnParsers;
		private String [] columnValues;
		private ByteBuffer input;
		
		private SingleSdbResultSet(File file) {
			this.file = file;
			this.columnNames = new HashMap<>();
			this.lineNumber = new AtomicLong(0);
			this.lineBuffer = new BasicStringBuilder(256);
			this.columnParsers = null;
			this.columnValues = null;
			this.input = null;
		}
		
		@Override
		public void close() {
			
		}
		
		@Override
		public boolean next() {
			lineNumber.incrementAndGet();
			int i = 0;
			try {
				for (i = 0; i < columnValues.length-1; i++) {
					columnValues[i] = fetchValue(false);
				}
				columnValues[i] = fetchValue(true);
				return true;
			} catch (EOFException e) {
				if (i > 0 && i < columnValues.length-1)
					Log.e("Invalid entry in sdb: %s on line %d - invalid number of columns!", file, lineNumber.get(), i+1);
				return false;
			}
		}
		
		@Override
		public List<String> getColumns() {
			return new ArrayList<>(columnNames.keySet());
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getObject(int index) {
			try {
				return (T) columnParsers[index].apply(columnValues[index]);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index+1));
			}
		}
		
		@Override
		public String getText(int index) {
			return columnValues[index];
		}
		
		@Override
		public String getText(String columnName) {
			return getText(columnNames.get(columnName));
		}
		
		@Override
		public long getInt(int index) {
			return getObject(index);
		}
		
		@Override
		public long getInt(String columnName) {
			return getInt(columnNames.get(columnName));
		}
		
		@Override
		public double getReal(int index) {
			return getObject(index);
		}
		
		@Override
		public double getReal(String columnName) {
			return getReal(columnNames.get(columnName));
		}
		
		@Override
		public boolean getBoolean(int index) {
			return getObject(index);
		}
		
		@Override
		public boolean getBoolean(String columnName) {
			return getBoolean(columnNames.get(columnName));
		}
		
		private String fetchValue(boolean endsNewline) throws EOFException {
			lineBuffer.clear();
			
			try {
				byte b;
				while (!isValueSeparator(b = input.get())) {
					lineBuffer.pushBack(b);
				}
				
				if (isLineSeparator(b)) {
					readNewLine();
					if (!endsNewline)
						throw new EOFException();
				}
			} catch (BufferUnderflowException | IndexOutOfBoundsException e) {
				if (lineBuffer.isEmpty())
					throw new EOFException();
			}
			
			return lineBuffer.toString();
		}
		
		private String fetchLine() {
			lineBuffer.clear();
			try {
				byte b;
				while (!isLineSeparator(b = input.get())) {
					lineBuffer.pushBack(b);
				}
				readNewLine();
			} catch (BufferUnderflowException | IndexOutOfBoundsException e) {
				if (lineBuffer.isEmpty())
					return null;
			}
			
			return lineBuffer.toString();
		}
		
		private void readNewLine() throws BufferUnderflowException, IndexOutOfBoundsException {
			int position = input.position();
			int limit = input.limit();
			while (position < limit && isLineSeparator(input.get(position))) {
				position++;
			}
			input.position(position);
		}
		
		private static SingleSdbResultSet load(File file) throws IOException {
			SingleSdbResultSet sdb = new SingleSdbResultSet(file);
			sdb.load();
			return sdb;
		}
		
		private void load() throws IOException {
			input = FileChannel.open(file.toPath(), StandardOpenOption.READ).map(MapMode.READ_ONLY, 0, file.length());
			loadHeader(fetchLine(), fetchLine());
		}
		
		@SuppressWarnings("unchecked") // stupid java not supporting generic arrays
		private void loadHeader(String columnsStr, String typesStr) {
			if (columnsStr == null || typesStr == null) {
				Log.e("Invalid SDB header: %s - nonexistent", file);
				return;
			}
			String [] columns = columnsStr.split("\t");
			String [] types = typesStr.split("\t");
			if (columns.length != types.length) {
				Log.e("Invalid SDB header: %s - invalid lengths!", file);
				return;
			}
			columnParsers = new Function[columns.length];
			columnValues = new String[columns.length];
			for (int i = 0; i < columns.length; i++) {
				columnParsers[i] = parseColumnType(types[i]).getTransformer();
				columnNames.put(columns[i], i);
			}
			lineNumber.set(2);
		}
		
		private DataType parseColumnType(String type) {
			if (type.indexOf(' ') != -1)
				type = type.substring(0, type.indexOf(' '));
			type = type.toLowerCase(Locale.US);
			
			if (type.equals("text"))
				return DataType.TEXT;
			if (type.equals("integer"))
				return DataType.INTEGER;
			if (type.equals("real"))
				return DataType.REAL;
			if (type.equals("bool") || type.equals("boolean"))
				return DataType.BOOLEAN;
			
			Log.e("Unknown column type: %s for file %s", type, file);
			return DataType.TEXT;
		}
		
		private static boolean isValueSeparator(byte c) {
			return c == '\r' || c == '\n' || c == '\t';
		}
		
		private static boolean isLineSeparator(byte c) {
			return c == '\r' || c == '\n';
		}
		
	}
	
	private static class BasicStringBuilder {
		
		private char [] data;
		private int length;
		
		public BasicStringBuilder(int size) {
			this.data = new char[size];
			this.length = 0;
		}
		
		public boolean isEmpty() {
			return length == 0;
		}
		
		public void clear() {
			this.length = 0;
		}
		
		public void pushBack(byte b) {
			if (length == data.length)
				data = Arrays.copyOf(data, length*2);
			data[length++] = (char) b;
		}
		
		@Override
		public String toString() {
			return new String(data, 0, length);
		}
		
	}
	
}
