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

import com.projectswg.utility.clientdata.Converters;

import java.io.IOException;

public class ClientdataConvertAll {
	
	public static void main(String [] args) throws IOException {
		Converters.OBJECTS_OBJECT_DATA.load();

		Converters.ABSTRACT_SLOT_DEFINITION.load();
		Converters.ABSTRACT_SLOT_ARRANGEMENT.load();
		Converters.ABSTRACT_SLOT_DESCRIPTORS.load();

		Converters.OBJECTS_BUILDING_CELLS.load();
		Converters.BUILDOUT_OBJECTS.load();

		Converters.PROFESSION_TEMPLATES.load();

		Converters.ROLES.load();
		Converters.COMMANDS_GLOBAL.load();
		Converters.COMMANDS_GROUND.load();
		Converters.COMMANDS_SPACE.load();
		Converters.BUFFS.load();
		Converters.SKILLS.load();
		Converters.APPEARANCE_TABLE.load();
		Converters.QUESTLIST.load();
		Converters.QUESTTASK.load();
	}
	
}
