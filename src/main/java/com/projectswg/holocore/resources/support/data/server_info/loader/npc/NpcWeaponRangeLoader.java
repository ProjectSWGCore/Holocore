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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class NpcWeaponRangeLoader extends DataLoader {
	
	private final Map<String, Integer> weapons;
	
	public NpcWeaponRangeLoader() {
		this.weapons = new HashMap<>();
	}
	
	/**
	 * Gets the range of the specified weapon. Returns -1 if no weapon with that IFF is found
	 * @param weaponIff the weapon IFF
	 * @return the range of the specified weapon, or -1 on error
	 */
	public int getWeaponRange(String weaponIff) {
		return weapons.getOrDefault(ClientFactory.formatToSharedFile(weaponIff), -1);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/npc/npc_weapon_range.sdb"))) {
			while (set.next()) {
				weapons.put(ClientFactory.formatToSharedFile(set.getText("weapons")), (int) set.getInt("weapon_range"));
			}
		}
	}
	
}
