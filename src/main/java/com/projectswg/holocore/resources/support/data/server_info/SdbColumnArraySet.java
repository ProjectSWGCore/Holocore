/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.intellij.lang.annotations.Language;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SdbColumnArraySet {
	
	private final Pattern pattern;
	private final Map<File, MappedInfo> mappedInfo;
	
	private SdbColumnArraySet(@Language("RegExp") String regex) {
		this.pattern = Pattern.compile(regex);
		this.mappedInfo = new ConcurrentHashMap<>();
	}
	
	public int size(SdbResultSet set) {
		return mappedInfo.computeIfAbsent(set.getFile(), f -> new MappedInfo(set)).getSize();
	}
	
	protected Collection<Entry<Integer, Integer>> getMappedEntries(SdbResultSet set) {
		return mappedInfo.computeIfAbsent(set.getFile(), f -> new MappedInfo(set)).getMappedColumns().entrySet();
	}
	
	public static class SdbTextColumnArraySet extends SdbColumnArraySet {
		
		private final ThreadLocal<String []> cachedArray;
		
		SdbTextColumnArraySet(@Language("RegExp") String regex) {
			super(regex);
			this.cachedArray = new ThreadLocal<>();
		}
		
		public String [] getArray(SdbResultSet set) {
			String [] cachedArray = this.cachedArray.get();
			int size = size(set);
			if (cachedArray == null || cachedArray.length != size) {
				cachedArray = new String[size];
				this.cachedArray.set(cachedArray);
			}
			
			Arrays.fill(cachedArray, null);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries(set)) {
					cachedArray[e.getKey()] = set.getText(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbIntegerColumnArraySet extends SdbColumnArraySet {
		
		private final ThreadLocal<int []> cachedArray;
		
		SdbIntegerColumnArraySet(@Language("RegExp") String regex) {
			super(regex);
			this.cachedArray = new ThreadLocal<>();
		}
		
		public int [] getArray(SdbResultSet set) {
			int [] cachedArray = this.cachedArray.get();
			int size = size(set);
			if (cachedArray == null || cachedArray.length != size) {
				cachedArray = new int[size];
				this.cachedArray.set(cachedArray);
			}
			
			Arrays.fill(cachedArray, Integer.MAX_VALUE);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries(set)) {
					cachedArray[e.getKey()] = (int) set.getInt(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbLongColumnArraySet extends SdbColumnArraySet {
		
		private final ThreadLocal<long []> cachedArray;
		
		SdbLongColumnArraySet(@Language("RegExp") String regex) {
			super(regex);
			this.cachedArray = new ThreadLocal<>();
		}
		
		public long [] getArray(SdbResultSet set) {
			long [] cachedArray = this.cachedArray.get();
			int size = size(set);
			if (cachedArray == null || cachedArray.length != size) {
				cachedArray = new long[size];
				this.cachedArray.set(cachedArray);
			}
			
			Arrays.fill(cachedArray, Long.MAX_VALUE);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries(set)) {
					cachedArray[e.getKey()] = set.getInt(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbRealColumnArraySet extends SdbColumnArraySet {
		
		private final ThreadLocal<double []> cachedArray;
		
		SdbRealColumnArraySet(@Language("RegExp") String regex) {
			super(regex);
			this.cachedArray = new ThreadLocal<>();
		}
		
		public double [] getArray(SdbResultSet set) {
			double [] cachedArray = this.cachedArray.get();
			int size = size(set);
			if (cachedArray == null || cachedArray.length != size) {
				cachedArray = new double[size];
				this.cachedArray.set(cachedArray);
			}
			
			Arrays.fill(cachedArray, Double.NaN);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries(set)) {
					cachedArray[e.getKey()] = set.getReal(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbBooleanColumnArraySet extends SdbColumnArraySet {
		
		private final ThreadLocal<boolean []> cachedArray;
		
		SdbBooleanColumnArraySet(@Language("RegExp") String regex) {
			super(regex);
			this.cachedArray = new ThreadLocal<>();
		}
		
		public boolean [] getArray(SdbResultSet set) {
			boolean [] cachedArray = this.cachedArray.get();
			int size = size(set);
			if (cachedArray == null || cachedArray.length != size) {
				cachedArray = new boolean[size];
				this.cachedArray.set(cachedArray);
			}
			
			Arrays.fill(cachedArray, false);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries(set)) {
					cachedArray[e.getKey()] = set.getBoolean(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	private class MappedInfo {
		
		private final Map<Integer, Integer> mappedColumns;
		private final int size;
		
		public MappedInfo(SdbResultSet set) {
			Map<Integer, Integer> mappedColumns = new HashMap<>();
			int size = 0;
			int columnIndex = 0;
			for (String column : set.getColumns()) {
				Matcher matcher = pattern.matcher(column);
				boolean match = matcher.matches();
				if (match && matcher.groupCount() == 1) {
					String arrayIndexStr = matcher.group(1);
					try {
						int arrayIndex = Integer.parseUnsignedInt(arrayIndexStr);
						if (arrayIndex >= size)
							size = arrayIndex+1;
						mappedColumns.put(arrayIndex, columnIndex);
					} catch (NumberFormatException e) {
						throw new IllegalArgumentException("invalid pattern. The first capturing group must be only digits");
					}
				} else if (match) {
					throw new IllegalArgumentException("invalid pattern. Regex must have capturing group for array index");
				}
				columnIndex++;
			}
			
			this.mappedColumns = Collections.unmodifiableMap(mappedColumns);
			this.size = size;
		}
		
		public Map<Integer, Integer> getMappedColumns() {
			return mappedColumns;
		}
		
		public int getSize() {
			return size;
		}
	}
	
}
