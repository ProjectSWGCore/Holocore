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

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class SdbGenerator implements Closeable {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final File file;
	private BufferedWriter writer;
	
	public SdbGenerator(File file) {
		if (file == null)
			throw new NullPointerException("File cannot be null!");
		this.file = file;
		this.writer = null;
	}
	
	public void open() throws FileNotFoundException {
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), ASCII));
	}
	
	public void close() throws IOException {
		writer.close();
	}
	
	public void setColumnNames(String ... names) throws IOException {
		for (int i = 0; i < names.length; i++) {
			writer.write(names[i]);
			if (i+1 < names.length)
				writer.write('\t');
		}
	}
	
	public void setColumnTypes(String ... types) throws IOException {
		writer.newLine();
		for (int i = 0; i < types.length; i++) {
			writer.write(types[i]);
			if (i+1 < types.length)
				writer.write('\t');
		}
	}
	
	public void writeLine(Object ... line) throws IOException {
		writer.newLine();
		for (int i = 0; i < line.length; i++) {
			writer.write(line[i].toString());
			if (i+1 < line.length)
				writer.write('\t');
		}
	}
	
}
