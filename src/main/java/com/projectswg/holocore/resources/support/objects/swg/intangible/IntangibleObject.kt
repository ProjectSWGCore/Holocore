/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.objects.swg.intangible

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.NetBufferStream
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.BaselineDelegate
import com.projectswg.holocore.resources.support.objects.swg.SWGObject

open class IntangibleObject : SWGObject {
	
	var count: Int by BaselineDelegate(value = 0, page = 3, update = 4)
	
	constructor(objectId: Long) : super(objectId, BaselineType.ITNO)
	constructor(objectId: Long, objectType: BaselineType) : super(objectId, objectType)
	
	override fun createBaseline3(target: Player, bb: BaselineBuilder) {
		super.createBaseline3(target, bb) // 4 variables
		bb.addInt(count) // 4
		
		bb.incrementOperandCount(1)
	}
	
	override fun parseBaseline3(buffer: NetBuffer) {
		super.parseBaseline3(buffer)
		count = buffer.int
	}
	
	override fun saveMongo(data: MongoData) {
		super.saveMongo(data)
		run {
			val base3 = data.getDocument("base3")
			base3.putInteger("count", count)
		}
	}
	
	override fun readMongo(data: MongoData) {
		super.readMongo(data)
		run {
			val base3 = data.getDocument("base3")
			count = base3.getInteger("count", 0)
		}
	}
	
	companion object {
		const val COUNT_PCD_STORED = 0
		const val COUNT_PCD_CALLED = 1
	}
	
}
