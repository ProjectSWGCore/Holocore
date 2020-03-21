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

import me.joshlarson.jlcommon.log.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class BasicLogStream {
	
	private final Object streamMutex;
	private final PrintStream stream;
	private final DateFormat dateFormat;
	
	public BasicLogStream(File file) {
		PrintStream s = null;
		setupDirectories(file);
		try {
			s = new PrintStream(new FileOutputStream(file, false), true, StandardCharsets.US_ASCII.name());
		} catch (UnsupportedEncodingException | FileNotFoundException e) {
			Log.e(e);
		}
		this.streamMutex = new Object();
		this.stream = s;
		this.dateFormat = new SimpleDateFormat("dd-MM-yy HH:mm:ss.SSS");
	}
	
	public void close() {
		stream.close();
	}
	
	public void log(String format, Object ... objects) {
		synchronized (streamMutex) {
			String date = dateFormat.format(System.currentTimeMillis());
			String str;
			if (objects.length == 0)
				str = date + ' ' + format;
			else
				str = date + ' ' + String.format(format, objects);
			stream.println(str);
		}
	}
	
	private void setupDirectories(File file) {
		File parentFile = file.getParentFile();
		if (parentFile.isDirectory())
			return;
		parentFile.mkdirs();
	}
	
}
