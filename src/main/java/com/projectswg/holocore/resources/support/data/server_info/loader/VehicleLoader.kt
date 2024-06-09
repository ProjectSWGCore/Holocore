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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet
import java.io.File
import java.io.IOException

class VehicleLoader internal constructor() : DataLoader() {
	private val vehicleTemplates: MutableMap<String, VehicleInfo> = HashMap()
	private val pcdTemplates: MutableMap<String, VehicleInfo> = HashMap()

	fun getVehicleFromIff(vehicleIff: String?): VehicleInfo? {
		return vehicleTemplates[ClientFactory.formatToSharedFile(vehicleIff)]
	}

	fun getVehicleFromPcdIff(pcdIff: String?): VehicleInfo? {
		return pcdTemplates[ClientFactory.formatToSharedFile(pcdIff)]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/vehicles/vehicles.sdb")).use { set ->
			while (set.next()) {
				val vehicle = VehicleInfo(set)
				assert(!vehicleTemplates.containsKey(vehicle.objectTemplate)) { "vehicle template already exists in map" }
				assert(!pcdTemplates.containsKey(vehicle.pcdTemplate)) { "vehicle template already exists in map" }
				vehicleTemplates[ClientFactory.formatToSharedFile(vehicle.objectTemplate)] = vehicle
				pcdTemplates[ClientFactory.formatToSharedFile(vehicle.pcdTemplate)] = vehicle
			}
		}
	}

	class VehicleInfo(set: SdbResultSet) {
		val objectReference: String = set.getText("object_reference")
		val pcdTemplate: String = set.getText("pcd_template")
		val objectTemplate: String = set.getText("object_template")
		val decayRate: Int = set.getInt("decay_rate").toInt()
		val repairRate: Double = set.getReal("repair_rate")
		val isCanRepairDisabled: Boolean = set.getBoolean("can_repair_disabled")
		val minSpeed: Double = set.getReal("min_speed")
		val speed: Double = set.getReal("speed")
		val isStrafe: Boolean = set.getBoolean("strafe")
		val turnRate: Int = set.getInt("turn_rate").toInt()
		val turnRateMax: Int = set.getInt("turn_rate_max").toInt()
		val accelMin: Double = set.getReal("accel_min")
		val accelMax: Double = set.getReal("accel_max")
		val decel: Double = set.getReal("decel")
		val dampingRoll: Double = set.getReal("damping_roll")
		val dampingPitch: Double = set.getReal("damping_pitch")
		val dampingHeight: Double = set.getReal("damping_height")
		val glide: Double = set.getReal("glide")
		val bankingAngle: Double = set.getReal("banking_angle")
		val hoverHeight: Double = set.getReal("hover_height")
		val autoLevel: Double = set.getReal("auto_level")
	}
}
