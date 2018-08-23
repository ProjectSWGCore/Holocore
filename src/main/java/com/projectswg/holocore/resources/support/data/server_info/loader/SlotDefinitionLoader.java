/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class SlotDefinitionLoader extends DataLoader {
	
	private final Map<String, SlotDefinition> buildingMap;
	
	SlotDefinitionLoader() {
		this.buildingMap = new HashMap<>();
	}
	
	public SlotDefinition getSlotDefinition(String slotName) {
		return buildingMap.get(slotName);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/abstract/slot_definitions.sdb"))) {
			while (set.next()) {
				SlotDefinition def = new SlotDefinition(set);
				buildingMap.put(def.getName(), def);
			}
		}
	}
	
	public static class SlotDefinition {
		
		private final String name;
		private final boolean global;
		private final boolean modifiable;
		private final boolean observeWithParent;
		private final boolean exposeToWorld;
		
		private SlotDefinition(SdbResultSet set) {
			// slotName	global	modifiable	observeWithParent	exposeToWorld
			this.name = set.getText("slotName");
			this.global = set.getBoolean("global");
			this.modifiable = set.getBoolean("modifiable");
			this.observeWithParent = set.getBoolean("observeWithParent");
			this.exposeToWorld = set.getBoolean("exposeToWorld");
		}
		
		public String getName() {
			return name;
		}
		
		public boolean isGlobal() {
			return global;
		}
		
		public boolean isModifiable() {
			return modifiable;
		}
		
		public boolean isObserveWithParent() {
			return observeWithParent;
		}
		
		public boolean isExposeToWorld() {
			return exposeToWorld;
		}
		
		@Override
		public int hashCode() {
			return name.hashCode();
		}
		
		@Override
		public boolean equals(Object o) {
			return o instanceof SlotDefinition && ((SlotDefinition) o).name.equals(name);
		}
		
	}
	
}
