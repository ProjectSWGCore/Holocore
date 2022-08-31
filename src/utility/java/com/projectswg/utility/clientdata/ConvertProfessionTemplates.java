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

package com.projectswg.utility.clientdata;

import com.projectswg.common.data.swgiff.parsers.SWGParser;
import com.projectswg.common.data.swgiff.parsers.creation.CombinedProfessionTemplateParser;
import com.projectswg.common.data.swgiff.parsers.creation.ProfessionTemplateParser;
import com.projectswg.holocore.utilities.SdbGenerator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

class ConvertProfessionTemplates implements Converter {
	
	@Override
	public void convert() {
		System.out.println("Converting profession templates...");
		
		try (SdbGenerator sdb = new SdbGenerator(new File("serverdata/player/start_clothing.sdb"))) {
			sdb.writeColumnNames("race", "combat_brawler", "combat_marksman", "crafting_artisan", "jedi", "outdoors_scout", "science_medic", "social_entertainer");
			convertFile(sdb, new File("clientdata/creation/profession_defaults.iff"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void convertFile(SdbGenerator sdb, File file) throws IOException {
		CombinedProfessionTemplateParser templates = SWGParser.parse(file);
		Objects.requireNonNull(templates, "Failed to load clientdata");
		
		Map<String, Map<String, String>> items = new TreeMap<>();
		for (Entry<String, ProfessionTemplateParser> template : templates.getTemplates().entrySet()) {
			for (Entry<String, List<String>> raceItems : template.getValue().getRaceItems().entrySet()) {
				items.computeIfAbsent(raceItems.getKey(), i -> new HashMap<>()).put(template.getKey(), String.join(";", raceItems.getValue()));
			}
		}
		
		for (Entry<String, Map<String, String>> raceItems : items.entrySet()) {
			// object/creature/player/shared_<NAME>.iff
			String race = raceItems.getKey().substring(30, raceItems.getKey().length()-4);
			sdb.writeLine(race,
					raceItems.getValue().get("combat_brawler"),
					raceItems.getValue().get("combat_marksman"),
					raceItems.getValue().get("crafting_artisan"),
					raceItems.getValue().get("jedi"),
					raceItems.getValue().get("outdoors_scout"),
					raceItems.getValue().get("science_medic"),
					raceItems.getValue().get("social_entertainer"));
		}
	}
	
}
