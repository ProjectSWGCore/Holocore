package com.projectswg.holocore.resources.support.objects.swg.ship

import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder

class ShipObjectClientServerNP(private val obj: ShipObject) {
	var chassisComponentMassCurrent = 0.0f
		set(value) {
			field = value
			sendDelta(0, value)
		}
	
	var chassisSpeedMaximumModifier = 0.0f
		set(value) {
			field = value
			sendDelta(1, value)
		}
	
	var capacitorEnergyCurrent = 0.0f
		set(value) {
			field = value
			sendDelta(2, value)
		}
	
	var boosterEnergyCurrent = 0.0f
		set(value) {
			field = value
			sendDelta(3, value)
		}
	
	var weaponEfficiencyRefireRate = SWGMap<Int,Float>(4, 4)
		set(value) {
			field = value
			sendDelta(4, value)
		}
	
	var cargoHoldContentsResourceTypeInfo = SWGMap<Int, Int>(4, 5)  // UNKNOWN TYPES
		set(value) {
			field = value
			sendDelta(5, value)
		}
	
	fun createBaseline4(bb: BaselineBuilder) {
		bb.addFloat(chassisComponentMassCurrent)
		bb.addFloat(chassisSpeedMaximumModifier)
		bb.addFloat(capacitorEnergyCurrent)
		bb.addFloat(boosterEnergyCurrent)
		bb.addObject(weaponEfficiencyRefireRate)
		bb.addObject(cargoHoldContentsResourceTypeInfo)
	}
	
	fun parseBaseline4(buffer: NetBuffer) {
		chassisComponentMassCurrent = buffer.float
		chassisSpeedMaximumModifier = buffer.float
		capacitorEnergyCurrent = buffer.float
		boosterEnergyCurrent = buffer.float
		weaponEfficiencyRefireRate.putAll(SWGMap.getSwgMap(buffer, 4, 4, Int::class.java, Float::class.java))
		cargoHoldContentsResourceTypeInfo.putAll(SWGMap.getSwgMap(buffer, 4, 5, Int::class.java, Int::class.java)) // UNKNOWN
	}
	
	private fun sendDelta(update: Int, o: Any) {
		obj.sendDelta(4, update, o)
	}
}
