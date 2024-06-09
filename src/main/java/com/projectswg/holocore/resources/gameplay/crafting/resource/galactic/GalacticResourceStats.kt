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
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic

import com.projectswg.common.data.encodables.mongo.MongoData
import com.projectswg.common.data.encodables.mongo.MongoPersistable
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource
import java.util.*
import kotlin.math.pow

class GalacticResourceStats : MongoPersistable {
	var coldResistance: Int = 0
	var conductivity: Int = 0
	var decayResistance: Int = 0
	var entangleResistance: Int = 0
	var flavor: Int = 0
	var heatResistance: Int = 0
	var malleability: Int = 0
	var overallQuality: Int = 0
	var potentialEnergy: Int = 0
	var shockResistance: Int = 0
	var unitToughness: Int = 0

	fun generateRandomStats(resource: RawResource) {
		val random = Random()
		this.coldResistance = if (resource.isAttrColdResistance) generateRandomNumber(random) else 0
		this.conductivity = if (resource.isAttrConductivity) generateRandomNumber(random) else 0
		this.decayResistance = if (resource.isAttrDecayResistance) generateRandomNumber(random) else 0
		this.entangleResistance = if (resource.isAttrEntangleResistance) generateRandomNumber(random) else 0
		this.flavor = if (resource.isAttrFlavor) generateRandomNumber(random) else 0
		this.heatResistance = if (resource.isAttrHeatResistance) generateRandomNumber(random) else 0
		this.malleability = if (resource.isAttrMalleability) generateRandomNumber(random) else 0
		this.overallQuality = if (resource.isAttrOverallQuality) generateRandomNumber(random) else 0
		this.potentialEnergy = if (resource.isAttrPotentialEnergy) generateRandomNumber(random) else 0
		this.shockResistance = if (resource.isAttrShockResistance) generateRandomNumber(random) else 0
		this.unitToughness = if (resource.isAttrUnitToughness) generateRandomNumber(random) else 0
	}

	override fun readMongo(data: MongoData) {
		coldResistance = data.getInteger("coldResistance", coldResistance)
		conductivity = data.getInteger("conductivity", conductivity)
		decayResistance = data.getInteger("decayResistance", decayResistance)
		entangleResistance = data.getInteger("entangleResistance", entangleResistance)
		flavor = data.getInteger("flavor", flavor)
		heatResistance = data.getInteger("heatResistance", heatResistance)
		malleability = data.getInteger("malleability", malleability)
		overallQuality = data.getInteger("overallQuality", overallQuality)
		potentialEnergy = data.getInteger("potentialEnergy", potentialEnergy)
		shockResistance = data.getInteger("shockResistance", shockResistance)
		unitToughness = data.getInteger("unitToughness", unitToughness)
	}

	override fun saveMongo(data: MongoData) {
		data.putInteger("coldResistance", coldResistance)
		data.putInteger("conductivity", conductivity)
		data.putInteger("decayResistance", decayResistance)
		data.putInteger("entangleResistance", entangleResistance)
		data.putInteger("flavor", flavor)
		data.putInteger("heatResistance", heatResistance)
		data.putInteger("malleability", malleability)
		data.putInteger("overallQuality", overallQuality)
		data.putInteger("potentialEnergy", potentialEnergy)
		data.putInteger("shockResistance", shockResistance)
		data.putInteger("unitToughness", unitToughness)
	}

	private fun generateRandomNumber(random: Random): Int {
		val x = random.nextDouble()
		return ((0.5 * x.pow(3.0) + 0.125 * (x + 1).pow(2.0)) * 1000).toInt()
	}
}
