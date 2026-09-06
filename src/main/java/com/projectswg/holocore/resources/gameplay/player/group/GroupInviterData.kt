/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.player.group

import com.projectswg.common.encoding.Encodable
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.global.player.Player

class GroupInviterData(var senderId: Long, var sender: Player?, private var counter: Long, var groupId: Long) : Encodable {
	override fun decode(data: NetBuffer): Boolean {
		senderId = data.long
		counter = data.long
		return true
	}

	override fun encode(): ByteArray {
		val data = NetBuffer.allocate(length)
		data.addLong(senderId)
		data.addLong(counter)
		return data.array()
	}

	override val length = 16

	fun getCounter() = counter

	fun incrementCounter() {
		counter++
	}
}