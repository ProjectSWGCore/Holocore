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
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class SdbColumnArraySet {
	
	private final Pattern pattern;
	private final Map<Integer, Integer> mappedColumns;
	private final AtomicReference<SdbResultSet> set;
	private final AtomicInteger arraySize;
	
	private SdbColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
		this.pattern = Pattern.compile(regex);
		this.mappedColumns = new HashMap<>();
		this.set = new AtomicReference<>(set);
		this.arraySize = new AtomicInteger(0);
		
		loadColumnInfo(set);
	}
	
	public int size() {
		return arraySize.get();
	}
	
	protected SdbResultSet getResultSet() {
		return set.get();
	}
	
	protected Collection<Entry<Integer, Integer>> getMappedEntries() {
		return mappedColumns.entrySet();
	}
	
	void loadColumnInfo(@Nullable SdbResultSet set) {
		mappedColumns.clear();
		arraySize.set(0);
		this.set.getAndSet(set);
		if (set == null)
			return;
		
		int columnIndex = 0;
		for (String column : set.getColumns()) {
			Matcher matcher = pattern.matcher(column);
			boolean match = matcher.matches();
			if (match && matcher.groupCount() == 1) {
				String arrayIndexStr = matcher.group(1);
				try {
					int arrayIndex = Integer.parseUnsignedInt(arrayIndexStr);
					arraySize.updateAndGet(prevIndex -> arrayIndex >= prevIndex ? arrayIndex + 1 : prevIndex);
					mappedColumns.put(arrayIndex, columnIndex);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("invalid pattern. The first capturing group must be only digits");
				}
			} else if (match) {
				throw new IllegalArgumentException("invalid pattern. Regex must have capturing group for array index");
			}
			columnIndex++;
		}
	}
	
	public static class SdbTextColumnArraySet extends SdbColumnArraySet {
		
		private String [] cachedArray;
		
		SdbTextColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
			super(set, regex);
			this.cachedArray = null;
		}
		
		public String [] getArray() {
			SdbResultSet set = getResultSet();
			String [] cachedArray = this.cachedArray;
			if (cachedArray == null || cachedArray.length != size()) {
				cachedArray = new String[size()];
				this.cachedArray = cachedArray;
			}
			
			Arrays.fill(cachedArray, null);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries()) {
					cachedArray[e.getKey()] = set.getText(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbIntegerColumnArraySet extends SdbColumnArraySet {
		
		private int [] cachedArray;
		
		SdbIntegerColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
			super(set, regex);
			this.cachedArray = null;
		}
		
		public int [] getArray() {
			SdbResultSet set = getResultSet();
			int [] cachedArray = this.cachedArray;
			if (cachedArray == null || cachedArray.length != size()) {
				cachedArray = new int[size()];
				this.cachedArray = cachedArray;
			}
			
			Arrays.fill(cachedArray, Integer.MAX_VALUE);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries()) {
					cachedArray[e.getKey()] = (int) set.getInt(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbLongColumnArraySet extends SdbColumnArraySet {
		
		private long [] cachedArray;
		
		SdbLongColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
			super(set, regex);
			this.cachedArray = null;
		}
		
		public long [] getArray() {
			SdbResultSet set = getResultSet();
			long [] cachedArray = this.cachedArray;
			if (cachedArray == null || cachedArray.length != size()) {
				cachedArray = new long[size()];
				this.cachedArray = cachedArray;
			}
			
			Arrays.fill(cachedArray, Long.MAX_VALUE);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries()) {
					cachedArray[e.getKey()] = set.getInt(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbRealColumnArraySet extends SdbColumnArraySet {
		
		private double [] cachedArray;
		
		SdbRealColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
			super(set, regex);
			this.cachedArray = null;
		}
		
		public double [] getArray() {
			SdbResultSet set = getResultSet();
			double [] cachedArray = this.cachedArray;
			if (cachedArray == null || cachedArray.length != size()) {
				cachedArray = new double[size()];
				this.cachedArray = cachedArray;
			}
			
			Arrays.fill(cachedArray, Double.NaN);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries()) {
					cachedArray[e.getKey()] = set.getReal(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
	public static class SdbBooleanColumnArraySet extends SdbColumnArraySet {
		
		private boolean [] cachedArray;
		
		SdbBooleanColumnArraySet(@Nullable SdbResultSet set, @Language("RegExp") String regex) {
			super(set, regex);
			this.cachedArray = null;
		}
		
		public boolean [] getArray() {
			SdbResultSet set = getResultSet();
			boolean [] cachedArray = this.cachedArray;
			if (cachedArray == null || cachedArray.length != size()) {
				cachedArray = new boolean[size()];
				this.cachedArray = cachedArray;
			}
			
			Arrays.fill(cachedArray, false);
			if (set != null) {
				for (Entry<Integer, Integer> e : getMappedEntries()) {
					cachedArray[e.getKey()] = set.getBoolean(e.getValue());
				}
			}
			return cachedArray;
		}
		
	}
	
}
