/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class ConvertQuestLists implements Converter {
	
	private static final File CLIENTDATA = new File("clientdata");
	private static final String OUTPUT_SDB_PATH = "serverdata/quests/questlist/quest.sdb";
	private static final List<String> COLUMN_NAMES = Arrays.asList("quest_name", "journal_entry", "journal_entry_title", "journal_entry_description", "category", "visible", "prerequisite_quests", "exclusion_quests", "allow_repeats", "target", "parameter", "complete_when_tasks_complete");

	public ConvertQuestLists() {
		
	}
	
	@Override
	public void convert() {
		String inputBasePath = "datatables/questlist";
		Path clientdataPath = CLIENTDATA.toPath();

		try(Stream<Path> pathStream = Files.walk(new File(CLIENTDATA, inputBasePath).toPath())) {
			List<Path> paths = pathStream.filter(path -> path.toString().endsWith(".iff"))
					.map(clientdataPath::relativize)
					.collect(Collectors.toList());

			convertDatatables(inputBasePath, paths);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void convertDatatables(String inputBasePath, Collection<Path> paths) {
		System.out.println("Converting " + paths.size() + " tables in " + inputBasePath + " to single, merged SDB " + OUTPUT_SDB_PATH + "...");

		try (SdbGenerator sdb = new SdbGenerator(new File(OUTPUT_SDB_PATH))) {
			sdb.writeColumnNames(COLUMN_NAMES.stream().map(columnName -> columnName.toLowerCase(Locale.ROOT)).toArray(String[]::new));

			for (Path path : paths) {
				System.out.println("|---Appending " + path + " to " + OUTPUT_SDB_PATH + "...");
				DatatableData datatable = (DatatableData) ClientFactory.getInfoFromFile(path.toString());
				Objects.requireNonNull(datatable, "Failed to load datatable");
				String questName = Paths.get("datatables", "questlist").relativize(path).toString().replace("\\", "/").replace(".iff", "");
				transferRows(sdb, datatable, questName);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void transferRows(SdbGenerator sdb, DatatableData datatable, String questName) throws IOException {
		int rowCount = datatable.getRowCount();

		for (int i = 0; i < rowCount; i++) {
			Object[] iffRow = datatable.getRow(i);
			Object[] sdbRow = new Object[COLUMN_NAMES.size()];

			for (int j = 0; j < sdbRow.length; j++) {
				String columnName = COLUMN_NAMES.get(j);
				
				if (columnName.equals("quest_name")) {
					sdbRow[j] = questName;
					continue;
				}
				int columnFromName = datatable.getColumnFromName(columnName.toUpperCase(Locale.ROOT));

				if (columnFromName == -1) {
					continue;
				}

				Object cellValue = iffRow[columnFromName];
				sdbRow[j] = cellValue;
			}

			sdb.writeLine(sdbRow);
		}
	}
}
