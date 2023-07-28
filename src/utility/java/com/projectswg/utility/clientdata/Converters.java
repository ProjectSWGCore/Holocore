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

import java.util.function.Supplier;

public enum Converters {
	BUILDOUT_BUILDING_LIST		(ConvertBuildingList::new),
	BUILDOUT_OBJECTS			(ConvertBuildouts::new),
	OBJECTS_OBJECT_APPEARANCE	(ConvertAppearances::new),
	OBJECTS_OBJECT_DATA			(ConvertObjectData::new),
	OBJECTS_BUILDING_CELLS		(ConvertBuildingCells::new),
	ABSTRACT_SLOT_DEFINITION	(ConvertSlotDefinition::new),
	ABSTRACT_SLOT_DESCRIPTORS	(ConvertSlotDescriptor::new),
	ABSTRACT_SLOT_ARRANGEMENT	(ConvertSlotArrangement::new),
	ROLES						(() -> new ConvertDatatable("datatables/role/role.iff", "serverdata/player/role.sdb", false)),
	COMMANDS_GLOBAL				(() -> new ConvertDatatable("datatables/command/command_table.iff", "serverdata/command/commands_global.sdb", false)),
	COMMANDS_GROUND				(() -> new ConvertDatatable("datatables/command/command_table_ground.iff", "serverdata/command/commands_ground.sdb", false)),
	COMMANDS_SPACE				(() -> new ConvertDatatable("datatables/command/command_table_space.iff", "serverdata/command/commands_space.sdb", false)),
	BUFFS						(() -> new ConvertDatatable("datatables/buff/buff.iff", "serverdata/buff/buff.sdb", true)),
	SKILLS						(() -> new ConvertDatatable("datatables/skill/skills.iff", "serverdata/skill/skills.sdb", true)),
	PROFESSION_TEMPLATES		(ConvertProfessionTemplates::new),
	APPEARANCE_TABLE			(() -> new ConvertDatatable("datatables/appearance/appearance_table.iff", "serverdata/appearance/appearance_table.sdb", true)),
	SCHEMATIC_GROUP				(() -> new ConvertDatatable("datatables/crafting/schematic_group.iff", "serverdata/crafting/schematic_group.sdb", true)),
	TERRAINS					(ConvertTerrain::new);
	
	private final Supplier<Converter> converter;
	
	Converters(Supplier<Converter> converter) {
		this.converter = converter;
	}
	
	public void load() {
		converter.get().convert();
	}
	
}
