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

	private val wounds = createWoundsList()
	
	private fun createWoundsList(): SWGList<Int> {
		val wounds = createIntList(3, 17)
		wounds.add(0, 0) // CU only has health wounds :-)
		
		return wounds
	}

	fun getHealthWounds(): Int {
		return wounds[0]
	}

	fun setHealthWounds(healthWounds: Int) {
		wounds[0] = healthWounds
		wounds.sendDeltaMessage(obj)
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
		val relevantMongoData = getRelevantMongoData(data)
		posture = Posture.valueOf(relevantMongoData.getString("posture", posture.name))
		height = relevantMongoData.getDouble("height", height)
		battleFatigue = relevantMongoData.getInteger("battleFatigue", battleFatigue)
		ownerId = relevantMongoData.getLong("ownerId", ownerId)
		statesBitmask = relevantMongoData.getLong("statesBitmask", statesBitmask)
		factionRank = relevantMongoData.getInteger("factionRank", factionRank.toInt()).toByte()
	}
	
	fun getRelevantMongoData(data: MongoData): MongoData {
		// To support migrating data from the existing document to the new, dedicated base3 document
		return if (data.containsKey("height")) {
			data
		} else {
			data.getDocument("base3")
		}
	}

	override fun saveMongo(data: MongoData) {
		data.putString("posture", posture.name)
		data.putDouble("height", height)
		data.putInteger("battleFatigue", battleFatigue)
		data.putLong("ownerId", ownerId)
		data.putLong("statesBitmask", statesBitmask)
		data.putInteger("factionRank", factionRank.toInt())
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