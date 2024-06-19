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
package com.projectswg.holocore.resources.support.objects.swg.creature

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.NetBuffer
import com.projectswg.common.network.packets.swg.zone.object_controller.PostureUpdate
import com.projectswg.holocore.resources.support.data.collections.SWGList
import com.projectswg.holocore.resources.support.data.collections.SWGList.Companion.createIntList
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder

class CreatureObjectShared(private val obj: CreatureObject) : MongoPersistable {

	var posture = Posture.UPRIGHT
		set(posture) {
			field = posture
			obj.sendObservers(PostureUpdate(obj.objectId, posture))
			sendDelta(11, posture.id)
		}

	var factionRank = 0.toByte()
		set(factionRank) {
			field = factionRank
			sendDelta(12, factionRank)
		}
	
	var ownerId = 0L
		set(ownerId) {
			field = ownerId
			sendDelta(13, ownerId)
		}

	var height = 0.0
		set(height) {
			field = height
			sendDelta(14, height)
		}
	
	var statesBitmask = 0L
		private set

	var battleFatigue = 0
		set(battleFatigue) {
			field = battleFatigue
			sendDelta(15, battleFatigue)
		}

	val wounds = createWoundsList()
	
	private fun createWoundsList(): SWGList<Int> {
		val wounds = createIntList(3, 17)
		populateWounds(wounds)
		return wounds
	}

	fun getHealthWounds(): Int {
		return wounds[0]
	}

	fun setHealthWounds(healthWounds: Int) {
		wounds[0] = healthWounds
		wounds.sendDeltaMessage(obj)
		if (obj.health > obj.maxHealth - obj.healthWounds) {
			obj.health -= healthWounds
		}
	}

	fun isStatesBitmask(vararg states: CreatureState): Boolean {
		for (state in states) {
			if (statesBitmask and state.bitmask == 0L) {
				return false
			}
		}
		return true
	}

	fun setStatesBitmask(vararg states: CreatureState) {
		for (state in states) {
			statesBitmask = statesBitmask or state.bitmask
		}
		sendDelta(16, statesBitmask)
	}

	fun toggleStatesBitmask(vararg states: CreatureState) {
		for (state in states) {
			statesBitmask = statesBitmask xor state.bitmask
		}
		sendDelta(16, statesBitmask)
	}

	fun clearStatesBitmask(vararg states: CreatureState) {
		for (state in states) {
			statesBitmask = statesBitmask and state.bitmask.inv()
		}
		sendDelta(16, statesBitmask)
	}

	fun clearAllStatesBitmask() {
		statesBitmask = 0
		sendDelta(16, statesBitmask)
	}

	override fun readMongo(data: MongoData) {
		wounds.clear()

		posture = Posture.valueOf(data.getString("posture", posture.name))
		height = data.getDouble("height", height)
		battleFatigue = data.getInteger("battleFatigue", battleFatigue)
		ownerId = data.getLong("ownerId", ownerId)
		statesBitmask = data.getLong("statesBitmask", statesBitmask)
		factionRank = data.getInteger("factionRank", factionRank.toInt()).toByte()
		wounds.addAll(data.getArray("wounds", Int::class.java))
		if (wounds.isEmpty()) {
			// Can happen with old data, before we started persisting wounds
			populateWounds(wounds)
		}
	}

	private fun populateWounds(wounds: SWGList<Int>) {
		wounds.add(0, 0)	// CU only has health wounds, so we'll just add that for now
	}

	override fun saveMongo(data: MongoData) {
		data.putString("posture", posture.name)
		data.putDouble("height", height)
		data.putInteger("battleFatigue", battleFatigue)
		data.putLong("ownerId", ownerId)
		data.putLong("statesBitmask", statesBitmask)
		data.putInteger("factionRank", factionRank.toInt())
		data.putArray("wounds", wounds)
	}

	fun createBaseline3(bb: BaselineBuilder) {
		bb.addByte(posture.id.toInt()) // 11
		bb.addByte(factionRank.toInt()) // 12
		bb.addLong(ownerId) // 13
		bb.addFloat(height.toFloat()) // 14
		bb.addInt(battleFatigue) // 15
		bb.addLong(statesBitmask) // 16
		bb.addObject(wounds) // 17
		bb.incrementOperandCount(7)
	}

	fun parseBaseline3(buffer: NetBuffer) {
		posture = Posture.getFromId(buffer.byte)
		factionRank = buffer.byte
		ownerId = buffer.long
		height = buffer.float.toDouble()
		battleFatigue = buffer.int
		statesBitmask = buffer.long
	}

	private fun sendDelta(update: Int, o: Any) {
		obj.sendDelta(3, update, o)
	}
}