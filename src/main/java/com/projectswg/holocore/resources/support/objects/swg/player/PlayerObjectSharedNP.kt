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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.objects.swg.IndirectBaselineDelegate

internal class PlayerObjectSharedNP(obj: PlayerObject) : MongoPersistable {

	var adminTag by IndirectBaselineDelegate(obj = obj, value = 0.toByte(), page = 6, update = 2)

	fun createBaseline6(bb: BaselineBuilder) {
		bb.addByte(adminTag.toInt())
		bb.incrementOperandCount(1)
	}

	fun parseBaseline6(buffer: NetBuffer) {
		adminTag = buffer.byte
	}

	override fun saveMongo(data: MongoData) {
		data.putInteger("adminTag", adminTag.toInt())
	}

	override fun readMongo(data: MongoData) {
		adminTag = data.getInteger("adminTag", adminTag.toInt()).toByte()
	}

}