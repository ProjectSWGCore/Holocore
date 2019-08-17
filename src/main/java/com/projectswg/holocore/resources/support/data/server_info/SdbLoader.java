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
package com.projectswg.holocore.resources.support.data.server_info;

import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.*;
import me.joshlarson.jlcommon.log.Log;
import org.intellij.lang.annotations.Language;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;
import java.util.Spliterators.AbstractSpliterator;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class SdbLoader {
	
	private SdbLoader() {
		
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
	
	public interface SdbResultSet extends Closeable, AutoCloseable {
		
		@Override
		void close() throws IOException;
		boolean next() throws IOException;
		List<String> getColumns();
		File getFile();
		int getLine();
		
		/**
		 * Returns a sequential stream after applying the specified transformation to the SdbResultSet
		 * @param transform the transformation to apply
		 * @param <T> the type of the returned stream
		 * @return a new sequential stream to iterate over the SDB
		 */
		<T> Stream<T> stream(Function<SdbResultSet, T> transform);
		/**
		 * Returns a parallel stream after applying the specified transformation to the SdbResultSet
		 * @param transform the transformation to apply
		 * @param <T> the type of the returned stream
		 * @return a new parallel stream to iterate over the SDB
		 */
		<T> Stream<T> parallelStream(Function<SdbResultSet, T> transform);
		
		SdbTextColumnArraySet getTextArrayParser(@Language("RegExp") String regex);
		SdbIntegerColumnArraySet getIntegerArrayParser(@Language("RegExp") String regex);
		SdbLongColumnArraySet getLongArrayParser(@Language("RegExp") String regex);
		SdbRealColumnArraySet getRealArrayParser(@Language("RegExp") String regex);
		SdbBooleanColumnArraySet getBooleanArrayParser(@Language("RegExp") String regex);
		
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
		
		private final List<SdbResultSet> sdbList;
		private final Iterator<SdbResultSet> sdbs;
		private final AtomicReference<SdbResultSet> sdb;
		
		private MasterSdbResultSet(List<SdbResultSet> sdbs) {
			this.sdbList = sdbs;
			this.sdbs = sdbs.iterator();
			this.sdb = new AtomicReference<>(this.sdbs.hasNext() ? this.sdbs.next() : null);
		}
		
		@Override
		public void close() throws IOException {
			SdbResultSet set = getResultSet();
			if (set != null)
				set.close();
		}
		
		@Override
		public <T> Stream<T> stream(Function<SdbResultSet, T> transform) {
			return stream(transform, false);
		}
		
		@Override
		public <T> Stream<T> parallelStream(Function<SdbResultSet, T> transform) {
			return stream(transform, true);
		}
		
		public <T> Stream<T> stream(Function<SdbResultSet, T> transform, boolean parallel) {
			return StreamSupport.stream(Spliterators.spliterator(sdbList, Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.DISTINCT), parallel)
					.flatMap(sdb -> sdb.stream(transform));
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
		public File getFile() {
			return getResultSet().getFile();
		}
		
		@Override
		public int getLine() {
			return getResultSet().getLine();
		}
		
		@Override
		public SdbTextColumnArraySet getTextArrayParser(@Language("RegExp") String regex) {
			return new SdbTextColumnArraySet(regex);
		}
		
		@Override
		public SdbIntegerColumnArraySet getIntegerArrayParser(String regex) {
			return new SdbIntegerColumnArraySet(regex);
		}
		
		@Override
		public SdbLongColumnArraySet getLongArrayParser(String regex) {
			return new SdbLongColumnArraySet(regex);
		}
		
		@Override
		public SdbRealColumnArraySet getRealArrayParser(String regex) {
			return new SdbRealColumnArraySet(regex);
		}
		
		@Override
		public SdbBooleanColumnArraySet getBooleanArrayParser(String regex) {
			return new SdbBooleanColumnArraySet(regex);
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
			return new MasterSdbResultSet(sets);
		}
		
	}
	
	private static class SingleSdbResultSet implements SdbResultSet {
		
		private final File file;
		private final Map<String, Integer> columnIndices;
		private final AtomicLong lineNumber;
		private String [] columnNames;
		private String [] columnValues;
		private BufferedReader input;
		
		private SingleSdbResultSet(File file) {
			this.file = file;
			this.columnIndices = new HashMap<>();
			this.lineNumber = new AtomicLong(0);
			this.columnNames = null;
			this.columnValues = null;
			this.input = null;
		}
		
		@Override
		public void close() throws IOException {
			input.close();
		}
		
		@Override
		public <T> Stream<T> stream(Function<SdbResultSet, T> transform) {
			return stream(transform, false);
		}
		
		@Override
		public <T> Stream<T> parallelStream(Function<SdbResultSet, T> transform) {
			return stream(transform, true);
		}
		
		public <T> Stream<T> stream(Function<SdbResultSet, T> transform, boolean parallel) {
			ThreadLocal<ParallelSdbResultSet> resultSet = ThreadLocal.withInitial(() -> new ParallelSdbResultSet(file, columnIndices, columnNames));
			return StreamSupport.stream(new SdbSpliterator(file, input, lineNumber), parallel).map(e -> {
				ParallelSdbResultSet set = resultSet.get();
				set.load(e.getValue(), e.getKey());
				return transform.apply(set);
			});
		}
		
		@Override
		public boolean next() {
			String line;
			do {
				lineNumber.incrementAndGet();
				try {
					line = input.readLine();
					if (line == null)
						return false;
				} catch (IOException e) {
					return false;
				}
			} while (line.isEmpty());
			int index = 0;
			final int columnCount = columnValues.length;
			for (int column = 0; column < columnCount; column++) {
				int nextIndex = line.indexOf('\t', index);
				if (nextIndex == -1) {
					if (column +1 < columnCount) {
						Log.e("Invalid entry in sdb: %s on line %d - invalid number of columns!", file, lineNumber.get(), column + 1);
						return false;
					}
					nextIndex = line.length();
				}
				columnValues[column] = line.substring(index, nextIndex);
				index = nextIndex+1;
			}
			return true;
		}
		
		@Override
		public List<String> getColumns() {
			return Arrays.asList(columnNames);
		}
		
		@Override
		public File getFile() {
			return file;
		}
		
		@Override
		public int getLine() {
			return (int) lineNumber.get();
		}
		
		@Override
		public SdbTextColumnArraySet getTextArrayParser(@Language("RegExp") String regex) {
			return new SdbTextColumnArraySet(regex);
		}
		
		@Override
		public SdbIntegerColumnArraySet getIntegerArrayParser(String regex) {
			return new SdbIntegerColumnArraySet(regex);
		}
		
		@Override
		public SdbLongColumnArraySet getLongArrayParser(String regex) {
			return new SdbLongColumnArraySet(regex);
		}
		
		@Override
		public SdbRealColumnArraySet getRealArrayParser(String regex) {
			return new SdbRealColumnArraySet(regex);
		}
		
		@Override
		public SdbBooleanColumnArraySet getBooleanArrayParser(String regex) {
			return new SdbBooleanColumnArraySet(regex);
		}
		
		@Override
		public String getText(int index) {
			return columnValues[index];
		}
		
		@Override
		public String getText(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist in sdb " + file;
			return columnValues[columnIndices.get(columnName)];
		}
		
		@Override
		public long getInt(int index) {
			try {
				return Long.parseLong(columnValues[index]);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index+1));
			}
		}
		
		@Override
		public long getInt(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist in sdb " + file;
			return getInt(columnIndices.get(columnName));
		}
		
		@Override
		public double getReal(int index) {
			try {
				return Double.parseDouble(columnValues[index]);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index+1));
			}
		}
		
		@Override
		public double getReal(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist in sdb " + file;
			return getReal(columnIndices.get(columnName));
		}
		
		@Override
		public boolean getBoolean(int index) {
			return columnValues[index].equalsIgnoreCase("true");
		}
		
		@Override
		public boolean getBoolean(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist in sdb " + file;
			return getBoolean(columnIndices.get(columnName));
		}
		
		private static SingleSdbResultSet load(File file) throws IOException {
			SingleSdbResultSet sdb = new SingleSdbResultSet(file);
			sdb.load();
			return sdb;
		}
		
		private void load() throws IOException {
			//noinspection ImplicitDefaultCharsetUsage - it doesn't use the default charset...
			input = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8), 128*1024);
			loadHeader(input.readLine());
			input.readLine();
		}
		
		private void loadHeader(String columnsStr) {
			if (columnsStr == null) {
				Log.e("Invalid SDB header: %s - nonexistent", file);
				return;
			}
			columnNames = columnsStr.split("\t");
			columnValues = new String[columnNames.length];
			for (int i = 0; i < columnNames.length; i++) {
				columnIndices.put(columnNames[i], i);
			}
			lineNumber.set(2);
		}
		
	}
	
	private static class ParallelSdbResultSet implements SdbResultSet {
		
		private final File file;
		private final Map<String, Integer> columnIndices;
		private final AtomicLong lineNumber;
		private final String [] columnNames;
		private String [] columnValues;
		
		public ParallelSdbResultSet(File file, Map<String, Integer> columnIndices, String [] columnNames) {
			this.file = file;
			this.columnIndices = columnIndices;
			this.lineNumber = new AtomicLong(0);
			this.columnNames = columnNames;
			this.columnValues = new String[columnIndices.size()];
		}
		
		@Override
		public void close() {
			throw new UnsupportedOperationException("Cannot close a parallel sdb");
			
		}
		
		@Override
		public boolean next() {
			throw new UnsupportedOperationException("Cannot iterate a parallel sdb");
		}
		
		@Override
		public <T> Stream<T> stream(Function<SdbResultSet, T> transform) {
			throw new UnsupportedOperationException("Cannot iterate a parallel sdb");
		}
		
		@Override
		public <T> Stream<T> parallelStream(Function<SdbResultSet, T> transform) {
			throw new UnsupportedOperationException("Cannot iterate a parallel sdb");
		}
		
		public void load(String line, long lineNumber) {
			int index = 0;
			final int columnCount = columnValues.length;
			for (int column = 0; column < columnCount; column++) {
				int nextIndex = line.indexOf('\t', index);
				if (nextIndex == -1) {
					if (column +1 < columnCount) {
						Log.e("Invalid entry in sdb: %s on line %d - invalid number of columns!", file, lineNumber, column + 1);
						return;
					}
					nextIndex = line.length();
				}
				columnValues[column] = line.substring(index, nextIndex);
				index = nextIndex+1;
			}
		}
		
		@Override
		public List<String> getColumns() {
			return Arrays.asList(columnNames);
		}
		
		@Override
		public File getFile() {
			return file;
		}
		
		@Override
		public int getLine() {
			return (int) lineNumber.get();
		}
		
		@Override
		public SdbTextColumnArraySet getTextArrayParser(@Language("RegExp") String regex) {
			return new SdbTextColumnArraySet(regex);
		}
		
		@Override
		public SdbIntegerColumnArraySet getIntegerArrayParser(String regex) {
			return new SdbIntegerColumnArraySet(regex);
		}
		
		@Override
		public SdbLongColumnArraySet getLongArrayParser(String regex) {
			return new SdbLongColumnArraySet(regex);
		}
		
		@Override
		public SdbRealColumnArraySet getRealArrayParser(String regex) {
			return new SdbRealColumnArraySet(regex);
		}
		
		@Override
		public SdbBooleanColumnArraySet getBooleanArrayParser(String regex) {
			return new SdbBooleanColumnArraySet(regex);
		}
		
		@Override
		public String getText(int index) {
			return columnValues[index];
		}
		
		@Override
		public String getText(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist";
			return columnValues[columnIndices.get(columnName)];
		}
		
		@Override
		public long getInt(int index) {
			try {
				return Long.parseLong(columnValues[index]);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index+1));
			}
		}
		
		@Override
		public long getInt(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist";
			return getInt(columnIndices.get(columnName));
		}
		
		@Override
		public double getReal(int index) {
			try {
				return Double.parseDouble(columnValues[index]);
			} catch (NumberFormatException e) {
				throw new NumberFormatException("Failed to parse value in sdb: " + file + " on line " + lineNumber.get() + " in column " + (index+1));
			}
		}
		
		@Override
		public double getReal(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist";
			return getReal(columnIndices.get(columnName));
		}
		
		@Override
		public boolean getBoolean(int index) {
			return columnValues[index].equalsIgnoreCase("true");
		}
		
		@Override
		public boolean getBoolean(String columnName) {
			assert columnIndices.containsKey(columnName) : "column " + columnName + " does not exist";
			return getBoolean(columnIndices.get(columnName));
		}
		
	}
	
	private static class SdbSpliterator extends AbstractSpliterator<Entry<Long, String>> {
		
		private final File file;
		private final BufferedReader input;
		private final AtomicLong lineNumber;
		
		public SdbSpliterator(File file, BufferedReader input, AtomicLong lineNumber) {
			super(Long.MAX_VALUE, Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.IMMUTABLE | Spliterator.DISTINCT);
			this.file = file;
			this.input = input;
			this.lineNumber = lineNumber;
		}
		
		@Override
		public synchronized boolean tryAdvance(Consumer<? super Entry<Long, String>> action) {
			String line;
			long lineNumber = -1;
			try {
				synchronized (input) {
					lineNumber = this.lineNumber.incrementAndGet();
					line = input.readLine();
				}
				if (line == null)
					return false;
				if (line.isEmpty())
					return true;
				action.accept(Map.entry(lineNumber, line));
				return true;
			} catch (IOException e) {
				throw new RuntimeException(e);
			} catch (Throwable t) {
				if (t.getCause() == null) {
					Log.e("Failed to load line %d on SDB %s due to %s: %s", lineNumber, file, t.getClass().getName(), t.getMessage());
				} else {
					Log.e("Failed to load line %d on SDB %s", lineNumber, file);
					while (t != null) {
						Log.e("    %s: %s", t.getClass().getName(), t.getMessage());
						t = t.getCause();
					}
				}
				return true;
			}
		}
		
	}
	
}
