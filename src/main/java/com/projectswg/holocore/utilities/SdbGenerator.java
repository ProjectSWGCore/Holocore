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
package com.projectswg.holocore.utilities;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class SdbGenerator implements Closeable, AutoCloseable {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private BufferedWriter writer;
	
	public SdbGenerator(File file) throws FileNotFoundException {
		if (file == null)
			throw new NullPointerException("File cannot be null!");
		this.writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), ASCII));
	}
	
	public void close() throws IOException {
		writer.close();
	}
	
	public void writeColumnNames(String ... names) throws IOException {
		for (int i = 0; i < names.length; i++) {
			writer.write(names[i]);
			if (i+1 < names.length)
				writer.write('\t');
		}
		writer.newLine();
		writer.newLine(); // No column types
	}
	
	public void writeColumnNames(List<String> names) throws IOException {
		boolean writeTab = false;
		for (String name : names) {
			if (writeTab)
				writer.write('\t');
			writer.write(name);
			writeTab = true;
		}
		writer.newLine();
		writer.newLine(); // No column types
	}
	
	public void writeLine(Object ... line) throws IOException {
		for (int i = 0; i < line.length; i++) {
			if (i != 0)
				writer.write('\t');
			writer.write(convertToString(line[i]));
		}
		writer.newLine();
	}
	
	public void writeAllLines(List<Object[]> lines) throws IOException {
		for (Object [] line : lines) {
			writeLine(line);
		}
	}
	
	private static String listToString(Collection<?> list) {
		StringBuilder str = new StringBuilder();
		int index = 0;
		for (Object o : list) {
			if (index++ != 0)
				str.append(';');
			str.append(convertToString(o));
		}
		return str.toString();
	}
	
	private static String mapToString(Map<String, ?> map) {
		StringBuilder str = new StringBuilder();
		int index = 0;
		for (Entry<String, ?> e : map.entrySet()) {
			if (index++ != 0)
				str.append(',');
			str.append(e.getKey());
			str.append('=');
			str.append(convertToString(e.getValue()));
		}
		return str.toString();
	}
	
	@SuppressWarnings("unchecked")
	private static String convertToString(Object o) {
		if (o instanceof Collection)
			return listToString((Collection) o);
		else if (o instanceof Map)
			return mapToString((Map<String, ?>) o);
		else
			return o == null ? "" : o.toString();
	}
	
}
