package com.projectswg.holocore.resources.support.objects.swg.ship

import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder

class ShipObjectShared(private val obj: ShipObject) {
	var slideDampener = 0.0f
		set(value) {
			field = value
			sendDelta(13, value)
		}
	
	var currentChassisHitPoints = 0.0f
		set(value) {
			field = value
			sendDelta(14, value)
		}
	
	var maximumChassisHitPoints = 0.0f
		set(value) {
			field = value
			sendDelta(15, value)
		}
	
	var chassisType = 0.toUInt()
		set(value) {
			field = value
			sendDelta(16, value)
		}
	
	var componentArmorHitpointsMaximum = SWGMap<Int,Float>(3, 8)
		set(value) {
			field = value
			sendDelta(17, value)
		}
	
	var componentArmorHitpointsCurrent = SWGMap<Int,Float>(3, 9)
		set(value) {
			field = value
			sendDelta(18, value)
		}
	
	var componentHitpointsCurrent = SWGMap<Int,Float>(3, 10)
		set(value) {
			field = value
			sendDelta(19, value)
		}
	
	var componentHitpointsMaximum = SWGMap<Int,Float>(3, 11)
		set(value) {
			field = value
			sendDelta(20, value)
		}
	
	var componentFlags = SWGMap<Int,Int>(3, 12)
		set(value) {
			field = value
			sendDelta(21, value)
		}
	
	var shieldHitpointsFrontMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(22, value)
		}
	
	var shieldHitpointsBackMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(23, value)
		}
	
	fun createBaseline3(bb: BaselineBuilder) {
		bb.addFloat(slideDampener)
		bb.addFloat(currentChassisHitPoints)
		bb.addFloat(maximumChassisHitPoints)
		bb.addInt(chassisType.toInt())
		bb.addObject(componentArmorHitpointsMaximum)
		bb.addObject(componentArmorHitpointsCurrent)
		bb.addObject(componentHitpointsCurrent)
		bb.addObject(componentHitpointsMaximum)
		bb.addObject(componentFlags)
		bb.addFloat(shieldHitpointsFrontMaximum)
		bb.addFloat(shieldHitpointsBackMaximum)
	}
	
	fun parseBaseline3(buffer: NetBuffer) {
		slideDampener = buffer.float
		currentChassisHitPoints = buffer.float
		maximumChassisHitPoints = buffer.float
		chassisType = buffer.int.toUInt()
		componentArmorHitpointsMaximum.putAll(SWGMap.getSwgMap(buffer, 3, 8, Int::class.java, Float::class.java))
		componentArmorHitpointsCurrent.putAll(SWGMap.getSwgMap(buffer, 3, 9, Int::class.java, Float::class.java))
		componentHitpointsCurrent.putAll(SWGMap.getSwgMap(buffer, 3, 10, Int::class.java, Float::class.java))
		componentHitpointsMaximum.putAll(SWGMap.getSwgMap(buffer, 3, 11, Int::class.java, Float::class.java))
		componentFlags.putAll(SWGMap.getSwgMap(buffer, 3, 12, Int::class.java, Int::class.java))
		shieldHitpointsFrontMaximum = buffer.float
		shieldHitpointsBackMaximum = buffer.float
	}
	
	private fun sendDelta(update: Int, o: Any) {
		obj.sendDelta(3, update, o)
	}
}
