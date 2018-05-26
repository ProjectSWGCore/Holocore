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

package com.projectswg.holocore.resources.support.data.client_info;

import com.projectswg.common.data.swgfile.ClientData;
import com.projectswg.common.data.swgfile.DataFactory;
import com.projectswg.common.data.swgfile.SWGFile;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.data.swgfile.visitors.DatatableData.ColumnType;
import me.joshlarson.jlcommon.log.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * Created by Waverunner on 6/9/2015
 */
public final class ServerFactory extends DataFactory {
	
	private static final Object instanceMutex = new Object();
	private static ServerFactory instance;

	public static DatatableData getDatatable(String file) {
		ClientData data = getInstance().readFile(file);
		if (data == null)
			throw new RuntimeException(new FileNotFoundException(file));
		return (DatatableData) data;
	}

	public void updateServerIffs() throws IOException {
		File root = new File(getFolder());

		Files.walkFileTree(root.toPath(), new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes basicFileAttributes) {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) {
				if (path.toString().endsWith("sdf")) {
					String name = path.toString();
					name = name.substring(0, name.length() - 4) + ".iff";

					File iff = new File(name);

					if (!iff.exists()) {
						convertSdf(path, name);
						Log.i("Created Server Datatable: %s", name);
					} else {
						File sif = path.toFile();
						if (sif.lastModified() > iff.lastModified()) {
							convertSdf(path, name);
							Log.i("Updated Server Datatable: %s", name);
						}
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path path, IOException e) {
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path path, IOException e) {
				return FileVisitResult.CONTINUE;
			}
		});
	}

	private void convertSdf(Path sif, String newPath) {
		SWGFile swgFile = new SWGFile(newPath, "DTII");

		DatatableData data = (DatatableData) createDataObject(swgFile);

		ColumnType[] columnTypes = null;
		String[] columnNames = null;
		Object[][] table = null;

		List<String> defaultValues = new ArrayList<>();
		try {
			int lineNum = -1;

			List<String> rows = Files.readAllLines(sif);
			Iterator<String> itr = rows.iterator();
			while (itr.hasNext()) {
				String row = itr.next();
				if (row == null || row.isEmpty() || row.startsWith("#") || row.startsWith("//")) {
					itr.remove();
					continue;
				}

				// Don't break out of the loop, make sure we remove all the commented/null/empty rows
				if (!(lineNum <= 1))
					continue;

				lineNum++;
				if (lineNum == 0) {
					columnNames = row.split("\t");
					itr.remove();
				} else if (lineNum == 1) {
					String [] rawTypes = row.split("\t");
					columnTypes = new ColumnType[rawTypes.length];
					for (int i = 0; i < rawTypes.length; i++) {
						String columnType = rawTypes[i];
						columnTypes[i] = ColumnType.getForChar(columnType);
						if (columnType.contains("[")) {
							defaultValues.add(columnType.split("\\[")[1].replace("]", ""));
						} else {
							defaultValues.add("");
						}
					}
					itr.remove();
				}
			}

			if (columnNames == null || columnTypes == null) {
				Log.e("Failed to convert sdf " + sif.getFileName());
				return;
			}

			table = new Object[rows.size()][columnTypes.length];

			for (int i = 0; i < rows.size(); i++) {
				createDatatableRow(i, rows.get(i), columnTypes, table, defaultValues);
			}

		} catch (IOException e) {
			Log.e(e);
		}

		data.setColumnNames(columnNames);
		data.setColumnTypes(columnTypes);
		data.setTable(table);

		writeFile(swgFile, data);
	}

	private void createDatatableRow(int rowNum, String line, ColumnType[] columnTypes, Object[][] table, List<String> defValues) {
		String[] values = line.split("\t", -1);

		for (int t = 0; t < columnTypes.length; t++) {
			ColumnType type = columnTypes[t];
			String val = values[t];

			if (val.isEmpty() && !defValues.get(t).isEmpty())
				val = defValues.get(t);

			try {
				switch(type) {
					case BOOLEAN:	table[rowNum][t] = Boolean.valueOf(val); break;
					case CRC:
					case INTEGER:	table[rowNum][t] = Integer.valueOf(val); break;
					case FLOAT:		table[rowNum][t] = Float.valueOf(val); break;
					case STRING:	table[rowNum][t] = val; break;
					default: Log.e("Don't know how to parse type " + type); break;
				}
			} catch (NumberFormatException e) {
				Log.e("Cannot format string %s to a number", val);
				Log.e(e);
			}

		}
	}

	@Override
	protected ClientData createDataObject(String type) {
		switch(type) {
			case "DTII": return new DatatableData();
			default: return null;
		}
	}

	@Override
	protected String getFolder() {
		return "./serverdata/";
	}

	public static ServerFactory getInstance() {
		synchronized (instanceMutex) {
			if (instance == null)
				instance = new ServerFactory();
			return instance;
		}
	}
}
