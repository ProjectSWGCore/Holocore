package com.projectswg.holocore.resources.support.objects.swg.ship

import com.projectswg.common.encoding.StringType
import com.projectswg.common.network.NetBuffer
import com.projectswg.holocore.resources.support.data.collections.SWGMap
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder

class ShipObjectClientServer(private val obj: ShipObject) {
	var componentEfficiencyGeneral = SWGMap<Int,Float>(1, 2)
		set(value) {
			field = value
			sendDelta(2, value)
		}
	
	var componentEfficiencyEnergy = SWGMap<Int,Float>(1, 3)
		set(value) {
			field = value
			sendDelta(3, value)
		}
	
	var componentEnergyMaintenanceRequirement = SWGMap<Int,Float>(1, 4)
		set(value) {
			field = value
			sendDelta(4, value)
		}
	
	var componentMass = SWGMap<Int,Float>(1, 5)
		set(value) {
			field = value
			sendDelta(5, value)
		}
	
	var componentNames = SWGMap<Int,String>(1, 6, StringType.UNICODE)
		set(value) {
			field = value
			sendDelta(6, value)
		}
	
	var componentCreators = SWGMap<Int,Long>(1, 7)
		set(value) {
			field = value
			sendDelta(7, value)
		}
	
	var weaponDamageMaximum = SWGMap<Int,Float>(1, 8)
		set(value) {
			field = value
			sendDelta(8, value)
		}
	
	var weaponDamageMinimum = SWGMap<Int,Float>(1, 9)
		set(value) {
			field = value
			sendDelta(9, value)
		}
	
	var weaponEffectivenessShields = SWGMap<Int,Float>(1, 10)
		set(value) {
			field = value
			sendDelta(10, value)
		}
	
	var weaponEffectivenessArmor = SWGMap<Int,Float>(1, 11)
		set(value) {
			field = value
			sendDelta(11, value)
		}
	
	var weaponEnergyPerShot = SWGMap<Int,Float>(1, 12)
		set(value) {
			field = value
			sendDelta(12, value)
		}
	
	var weaponRefireRate = SWGMap<Int,Float>(1, 13)
		set(value) {
			field = value
			sendDelta(13, value)
		}
	
	var weaponAmmoCurrent = SWGMap<Int,Int>(1, 14)
		set(value) {
			field = value
			sendDelta(14, value)
		}
	
	var weaponAmmoMaximum = SWGMap<Int,Int>(1, 15)
		set(value) {
			field = value
			sendDelta(15, value)
		}
	
	var weaponAmmoType = SWGMap<Int,UInt>(1, 16)
		set(value) {
			field = value
			sendDelta(16, value)
		}
	
	var chassisComponentMassMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(17, value)
		}
	
	var shieldRechargeRate = 0.0f
		set(value) {
			field = value
			sendDelta(18, value)
		}
	
	var capacitorEnergyMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(19, value)
		}
	
	var capacitorEnergyRechargeRate = 0.0f
		set(value) {
			field = value
			sendDelta(20, value)
		}
	
	var engineAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(21, value)
		}
	
	var engineDecelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(22, value)
		}
	
	var enginePitchAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(23, value)
		}
	
	var engineYawAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(24, value)
		}
	
	var engineRollAccelerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(25, value)
		}
	
	var enginePitchRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(26, value)
		}
	
	var engineYawRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(27, value)
		}
	
	var engineRollRateMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(28, value)
		}
	
	var engineSpeedMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(29, value)
		}
	
	var reactorEnergyGenerationRate = 0.0f
		set(value) {
			field = value
			sendDelta(30, value)
		}
	
	var boosterEnergyMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(31, value)
		}
	
	var boosterEnergyRechargeRate = 0.0f
		set(value) {
			field = value
			sendDelta(32, value)
		}
	
	var boosterEnergyConsumptionRate = 0.0f
		set(value) {
			field = value
			sendDelta(33, value)
		}
	
	var boosterAcceleration = 0.0f
		set(value) {
			field = value
			sendDelta(34, value)
		}
	
	var boosterSpeedMaximum = 0.0f
		set(value) {
			field = value
			sendDelta(35, value)
		}
	
	var droidInterfaceCommandSpeed = 0.0f
		set(value) {
			field = value
			sendDelta(36, value)
		}
	
	var installedDroidControlDevice = 0L
		set(value) {
			field = value
			sendDelta(37, value)
		}
	
	var cargoHoldContentsMaximum = 0
		set(value) {
			field = value
			sendDelta(38, value)
		}
	
	var cargoHoldContentsCurrent = 0
		set(value) {
			field = value
			sendDelta(39, value)
		}
	
	var cargoHoldContents = SWGMap<Long,Int>(1, 40)
		set(value) {
			field = value
			sendDelta(40, value)
		}
	
	fun createBaseline1(bb: BaselineBuilder) {
		bb.addObject(componentEfficiencyGeneral)
		bb.addObject(componentEfficiencyEnergy)
		bb.addObject(componentEnergyMaintenanceRequirement)
		bb.addObject(componentMass)
		bb.addObject(componentNames)
		bb.addObject(componentCreators)
		bb.addObject(weaponDamageMaximum)
		bb.addObject(weaponDamageMinimum)
		bb.addObject(weaponEffectivenessShields)
		bb.addObject(weaponEffectivenessArmor)
		bb.addObject(weaponEnergyPerShot)
		bb.addObject(weaponRefireRate)
		bb.addObject(weaponAmmoCurrent)
		bb.addObject(weaponAmmoMaximum)
		bb.addObject(weaponAmmoType)
		bb.addFloat(chassisComponentMassMaximum)
		bb.addFloat(shieldRechargeRate)
		bb.addFloat(capacitorEnergyMaximum)
		bb.addFloat(capacitorEnergyRechargeRate)
		bb.addFloat(engineAccelerationRate)
		bb.addFloat(engineDecelerationRate)
		bb.addFloat(enginePitchAccelerationRate)
		bb.addFloat(engineYawAccelerationRate)
		bb.addFloat(engineRollAccelerationRate)
		bb.addFloat(enginePitchRateMaximum)
		bb.addFloat(engineYawRateMaximum)
		bb.addFloat(engineRollRateMaximum)
		bb.addFloat(engineSpeedMaximum)
		bb.addFloat(reactorEnergyGenerationRate)
		bb.addFloat(boosterEnergyMaximum)
		bb.addFloat(boosterEnergyRechargeRate)
		bb.addFloat(boosterEnergyConsumptionRate)
		bb.addFloat(boosterAcceleration)
		bb.addFloat(boosterSpeedMaximum)
		bb.addFloat(droidInterfaceCommandSpeed)
		bb.addLong(installedDroidControlDevice)
		bb.addInt(cargoHoldContentsMaximum)
		bb.addInt(cargoHoldContentsCurrent)
		bb.addObject(cargoHoldContents)
	}
	
	fun parseBaseline1(buffer: NetBuffer) {
		componentEfficiencyGeneral.putAll(SWGMap.getSwgMap(buffer, 1, 2, Int::class.java, Float::class.java))
		componentEfficiencyEnergy.putAll(SWGMap.getSwgMap(buffer, 1, 3, Int::class.java, Float::class.java))
		componentEnergyMaintenanceRequirement.putAll(SWGMap.getSwgMap(buffer, 1, 4, Int::class.java, Float::class.java))
		componentMass.putAll(SWGMap.getSwgMap(buffer, 1, 5, Int::class.java, Float::class.java))
		componentNames.putAll(SWGMap.getSwgMap(buffer, 1, 6, Int::class.java, StringType.UNICODE))
		componentCreators.putAll(SWGMap.getSwgMap(buffer, 1, 7, Int::class.java, Long::class.java))
		weaponDamageMaximum.putAll(SWGMap.getSwgMap(buffer, 1, 8, Int::class.java, Float::class.java))
		weaponDamageMinimum.putAll(SWGMap.getSwgMap(buffer, 1, 9, Int::class.java, Float::class.java))
		weaponEffectivenessShields.putAll(SWGMap.getSwgMap(buffer, 1, 10, Int::class.java, Float::class.java))
		weaponEffectivenessArmor.putAll(SWGMap.getSwgMap(buffer, 1, 11, Int::class.java, Float::class.java))
		weaponEnergyPerShot.putAll(SWGMap.getSwgMap(buffer, 1, 12, Int::class.java, Float::class.java))
		weaponRefireRate.putAll(SWGMap.getSwgMap(buffer, 1, 13, Int::class.java, Float::class.java))
		weaponAmmoCurrent.putAll(SWGMap.getSwgMap(buffer, 1, 14, Int::class.java, Int::class.java))
		weaponAmmoMaximum.putAll(SWGMap.getSwgMap(buffer, 1, 15, Int::class.java, Int::class.java))
		weaponAmmoType.putAll(SWGMap.getSwgMap(buffer, 1, 16, Int::class.java, UInt::class.java))
		chassisComponentMassMaximum = buffer.float
		shieldRechargeRate = buffer.float
		capacitorEnergyMaximum = buffer.float
		capacitorEnergyRechargeRate = buffer.float
		engineAccelerationRate = buffer.float
		engineDecelerationRate = buffer.float
		enginePitchAccelerationRate = buffer.float
		engineYawAccelerationRate = buffer.float
		engineRollAccelerationRate = buffer.float
		enginePitchRateMaximum = buffer.float
		engineYawRateMaximum = buffer.float
		engineRollRateMaximum = buffer.float
		engineSpeedMaximum = buffer.float
		reactorEnergyGenerationRate = buffer.float
		boosterEnergyMaximum = buffer.float
		boosterEnergyRechargeRate = buffer.float
		boosterEnergyConsumptionRate = buffer.float
		boosterAcceleration = buffer.float
		boosterSpeedMaximum = buffer.float
		droidInterfaceCommandSpeed = buffer.float
		installedDroidControlDevice = buffer.long
		cargoHoldContentsMaximum = buffer.int
		cargoHoldContentsCurrent = buffer.int
		cargoHoldContents.putAll(SWGMap.getSwgMap(buffer, 1, 40, Long::class.java, Int::class.java))
	}
	
	private fun sendDelta(update: Int, o: Any) {
		obj.sendDelta(1, update, o)
	}
}
