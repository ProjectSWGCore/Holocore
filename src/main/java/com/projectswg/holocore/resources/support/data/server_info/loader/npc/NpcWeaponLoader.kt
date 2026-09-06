/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.npc

import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.*
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import java.io.File
import java.io.IOException
import java.util.function.Consumer

class NpcWeaponLoader : DataLoader() {
	private val weapons: MutableMap<String, List<String>> = HashMap()
	private val weaponTypes: MutableMap<String, WeaponType> = HashMap()

	fun getWeapons(weaponId: String): List<String>? {
		return weapons[weaponId]
	}

	/**
	 * Gets the type of the specified weapon. Returns `null` if no weapon with that IFF is found
	 * @param weaponIff the weapon IFF
	 * @return the type of the specified weapon, or `null` on error
	 */
	fun getWeaponType(weaponIff: String?): WeaponType? {
		return weaponTypes[ClientFactory.formatToSharedFile(weaponIff)]
	}

	fun forEach(c: Consumer<List<String>>?) {
		weapons.values.forEach(c)
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/npc/npc_weapon.sdb")).use { set ->
			while (set.next()) {
				val templates = set.getText("weapons").split(";".toRegex()).dropLastWhile { it.isEmpty() }
				weapons[set.getText("weapon_id")] = templates

				val weaponType = WeaponType.valueOf(set.getText("weapon_type"))
				for (template in templates) {
					weaponTypes[ClientFactory.formatToSharedFile(template)] = weaponType
				}
			}
		}
	}
}
