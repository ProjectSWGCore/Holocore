/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File

class MovementLoader : DataLoader() {

	private val movements: MutableMap<String, MovementInfo> = mutableMapOf()

	fun getMovement(movementId: String): MovementInfo? {
		return movements[movementId]
	}

	override fun load() {
		SdbLoader.load(File("serverdata/player/movement.sdb")).use { set ->
			while (set.next()) {
				val movementInfo = MovementInfo(set)
				movements[movementInfo.movementId] = movementInfo
			}
		}
	}

	class MovementInfo(set: SdbResultSet) {
		val movementId = set.getText("movement_id")
		val type = MovementType.valueOf(set.getText("type").uppercase())
		val strength = set.getInt("strength").toInt()
		val affectsOnfoot = set.getBoolean("affects_onfoot")
		val affectsVehicle = set.getBoolean("affects_vehicle")
		val affectsMount = set.getBoolean("affects_mount")
	}

	enum class MovementType(val slowing: Boolean) {
		PERMASNARE(true),
		SNARE(true),
		BOOST(false),
		PERMABOOST(false),
		ROOT(true),
	}
}