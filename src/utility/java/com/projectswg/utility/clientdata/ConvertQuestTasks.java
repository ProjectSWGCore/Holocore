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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Quest-specific converter that can convert multiple .iff -> multiple .sdb
 */
public class ConvertQuestTasks implements Converter {
	
	private static final String INPUT_BASE_PATH = "datatables/questtask";
	private static final String OUTPUT_BASE_PATH = "serverdata/quests/questtask";
	private static final boolean LOWERCASE_COLUMN_NAMES = true;
	
	@Override
	public void convert() {
		File clientdataFolder = new File("clientdata");
		Path clientdataPath = clientdataFolder.toPath();
		
		try(Stream<Path> pathStream = Files.walk(new File(clientdataFolder, INPUT_BASE_PATH).toPath())) {
			List<Path> paths = pathStream.filter(path -> path.toString().endsWith(".iff"))
					.map(clientdataPath::relativize)
					.collect(Collectors.toList());
			
			convertDatatables(paths);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void convertDatatables(Collection<Path> paths) {
		System.out.println("Converting " +  paths.size() + " tables in " + ConvertQuestTasks.INPUT_BASE_PATH + " to multiple SDBs in " + OUTPUT_BASE_PATH + "...");
		for (Path path : paths) {
			convertDatatable(path);
		}
	}

	private void convertDatatable(Path path) {
		String inputDatatablePath = path.toString().replace("\\", "/");
		String outputSdbPath = inputDatatablePath.replace(ConvertQuestTasks.INPUT_BASE_PATH, OUTPUT_BASE_PATH).replace(".iff", ".sdb");
		ConvertDatatable converter = new ConvertDatatable(inputDatatablePath, outputSdbPath, LOWERCASE_COLUMN_NAMES);
		System.out.print("|---");
		converter.convert();
	}

}
