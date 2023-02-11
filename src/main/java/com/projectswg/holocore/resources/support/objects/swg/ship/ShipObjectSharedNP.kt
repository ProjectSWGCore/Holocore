package com.projectswg.holocore.resources.support.objects.swg.ship

import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGBitSet
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder

class ShipObjectSharedNP(private val obj: ShipObject) {
	var shipId = 0.toUShort()
		set(value) {
			field = value
			sendDelta(8, value)
		}
	
	var shipActualAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(9, value)
		}
	
	var shipActualDecelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(10, value)
		}
	
	var shipActualPitchAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(11, value)
		}
	
	var shipActualYawAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(12, value)
		}
	
	var shipActualRollAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(13, value)
		}
	
	var shipActualPitchRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(14, value)
		}
	
	var shipActualYawRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(15, value)
		}
	
	var shipActualRollRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(16, value)
		}
	
	var shipActualSpeedMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(17, value)
		}
	
	var pilotLookAtTarget = 0L // NetworkId
		set(value) {
			field = value
			sendDelta(18, value)
		}
	
	var pilotLookAtTargetSlot = 0
		set(value) {
			field = value
			sendDelta(19, value)
		}
	
	var targetableSlotBitfield = SWGBitSet(6, 14)
		set(value) {
			field = value
			sendDelta(20, value)
		}
	
	var componentCrcForClient = SWGMap<Int,UInt>(6, 15)
		set(value) {
			field = value
			sendDelta(21, value)
		}
	
	var wingName = "" // StringType.ASCII
		set(value) {
			field = value
			sendDelta(22, value)
		}
	
	var typeName = "" // StringType.ASCII
		set(value) {
			field = value
			sendDelta(23, value)
		}
	
	var difficulty = "" // StringType.ASCII
		set(value) {
			field = value
			sendDelta(24, value)
		}
	
	var faction = "" // StringType.ASCII
		set(value) {
			field = value
			sendDelta(25, value)
		}
	
	var shieldHitpointsFrontCurrent = 0.0f
		set(value) {
			field = value
			sendDelta(26, value)
		}
	
	var shieldHitpointsBackCurrent = 0.0f
		set(value) {
			field = value
			sendDelta(27, value)
		}
	
	var guildId = 0
		set(value) {
			field = value
			sendDelta(28, value)
		}
	
	fun createBaseline6(bb: BaselineBuilder) {
		bb.addShort(shipId.toInt())
		bb.addFloat(shipActualAccelerationRate)
		bb.addFloat(shipActualDecelerationRate)
		bb.addFloat(shipActualPitchAccelerationRate)
		bb.addFloat(shipActualYawAccelerationRate)
		bb.addFloat(shipActualRollAccelerationRate)
		bb.addFloat(shipActualPitchRateMaximum)
		bb.addFloat(shipActualYawRateMaximum)
		bb.addFloat(shipActualRollRateMaximum)
		bb.addFloat(shipActualSpeedMaximum)
		bb.addLong(pilotLookAtTarget)
		bb.addInt(pilotLookAtTargetSlot)
		bb.addObject(targetableSlotBitfield)
		bb.addObject(componentCrcForClient)
		bb.addAscii(wingName)
		bb.addAscii(typeName)
		bb.addAscii(difficulty)
		bb.addAscii(faction)
		bb.addFloat(shieldHitpointsFrontCurrent)
		bb.addFloat(shieldHitpointsBackCurrent)
		bb.addInt(guildId)
	}
	
	fun parseBaseline6(buffer: NetBuffer) {
		shipId = buffer.short.toUShort()
		shipActualAccelerationRate = buffer.float
		shipActualDecelerationRate = buffer.float
		shipActualPitchAccelerationRate = buffer.float
		shipActualYawAccelerationRate = buffer.float
		shipActualRollAccelerationRate = buffer.float
		shipActualPitchRateMaximum = buffer.float
		shipActualYawRateMaximum = buffer.float
		shipActualRollRateMaximum = buffer.float
		shipActualSpeedMaximum = buffer.float
		pilotLookAtTarget = buffer.long
		pilotLookAtTargetSlot = buffer.int
		targetableSlotBitfield.decode(buffer)
		componentCrcForClient.putAll(SWGMap.getSwgMap(buffer, 6, 15, Int::class.java, UInt::class.java))
		wingName = buffer.ascii
		typeName = buffer.ascii
		difficulty = buffer.ascii
		faction = buffer.ascii
		shieldHitpointsFrontCurrent = buffer.float
		shieldHitpointsBackCurrent = buffer.float
		guildId = buffer.int
	}
	
	private fun sendDelta(update: Int, o: Any) {
		obj.sendDelta(6, update, o)
	}
}
