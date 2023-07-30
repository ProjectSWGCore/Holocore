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
package com.projectswg.utility;

import com.projectswg.common.data.CrcDatabase;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

public class AddQuestsToCrcDatabase {
	public static void main(String[] args) throws IOException {
		Path questListPath = Paths.get("clientdata", "datatables", "questlist");
		Path questPath = Paths.get(questListPath.toString(), "quest");
		
		Collection<String> all = new ArrayList<>();
		all.addAll(getNames(questPath, "quest"));
		all.addAll(getNames(questPath, "groundquest"));
		all.addAll(getNames(questPath, "spacequest"));
		
		CrcDatabase instance = CrcDatabase.getInstance();
		
		for (String name : all) {
			instance.addCrc(name);
		}
		
		instance.saveStrings(new FileOutputStream("crc_database.csv"));
	}
	
	private static Collection<String> getNames(Path questPath, String subFolderName) {
		String[] list = questPath.toFile().list();
		Collection<String> names = new ArrayList<>();
		
		for (String s : list) {
			names.add(subFolderName + "/" + s.replace(".iff", ""));
		}
		
		return names;
	}
}
