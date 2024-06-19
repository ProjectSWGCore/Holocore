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
package com.projectswg.holocore.resources.support.objects.swg.player

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGFlag
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder
import com.projectswg.holocore.resources.support.global.player.PlayerFlags
import com.projectswg.holocore.resources.support.objects.swg.IndirectBaselineDelegate

/**
 * PLAY 3
 */
internal class PlayerObjectShared(obj: PlayerObject) : MongoPersistable {

	private val _flags = SWGFlag(3, 5)
	val flags = _flags.wrapper(obj) { it: PlayerFlags -> it.flag }

	private val _profileFlags = SWGFlag(3, 6)
	val profileFlags = _profileFlags.wrapper(obj) { it: PlayerFlags -> it.flag }

	var title by IndirectBaselineDelegate(obj = obj, value = "", page = 3, update = 7, stringType = StringType.ASCII)
	var bornDate by IndirectBaselineDelegate(obj = obj, value = 0, page = 3, update = 8)
	var playTime by IndirectBaselineDelegate(obj = obj, value = 0, page = 3, update = 9)
	var professionIcon by IndirectBaselineDelegate(obj = obj, value = 0, page = 3, update = 10)

	fun incrementPlayTime(playTime: Int) {
		this.playTime += playTime
	}

	fun createBaseline3(bb: BaselineBuilder) {
		bb.addObject(_flags) // 5
		bb.addObject(_profileFlags) // 6
		bb.addAscii(title) // 7
		bb.addInt(bornDate) // 8
		bb.addInt(playTime) // 9
		bb.addInt(professionIcon) // 10
		bb.incrementOperandCount(6)
	}

	fun parseBaseline3(buffer: NetBuffer) {
		_flags.decode(buffer) // 5
		_profileFlags.decode(buffer) // 6
		title = buffer.ascii // 7
		bornDate = buffer.int // 8
		playTime = buffer.int // 9
		professionIcon = buffer.int // 10
	}

	override fun saveMongo(data: MongoData) {
		data.putString("title", title)
		data.putInteger("bornDate", bornDate)
		data.putInteger("playTime", playTime)
		data.putInteger("professionIcon", professionIcon)
	}

	override fun readMongo(data: MongoData) {
		title = data.getString("title", title)
		bornDate = data.getInteger("bornDate", bornDate)
		playTime = data.getInteger("playTime", playTime)
		professionIcon = data.getInteger("professionIcon", professionIcon)
	}
}
