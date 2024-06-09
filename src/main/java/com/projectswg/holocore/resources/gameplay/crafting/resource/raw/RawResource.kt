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
package com.projectswg.holocore.resources.gameplay.crafting.resource.raw

import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.RawResourceType.Companion.getRawResourceType
import me.joshlarson.jlcommon.utilities.Arguments
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList

class RawResource private constructor(builder: RawResourceBuilder) {
	val id: Long = builder.id
	val name: String
	val parent: RawResource?
	val resourceType: RawResourceType
	val crateTemplate: String?
	val minTypes: Int
	val maxTypes: Int
	val minPools: Int
	val maxPools: Int
	val isRecycled: Boolean
	val isAttrColdResistance: Boolean
	val isAttrConductivity: Boolean
	val isAttrDecayResistance: Boolean
	val isAttrEntangleResistance: Boolean
	val isAttrFlavor: Boolean
	val isAttrHeatResistance: Boolean
	val isAttrMalleability: Boolean
	val isAttrOverallQuality: Boolean
	val isAttrPotentialEnergy: Boolean
	val isAttrShockResistance: Boolean
	val isAttrUnitToughness: Boolean
	private val _children: MutableList<RawResource> = CopyOnWriteArrayList()
	val children: List<RawResource>
		get() { return Collections.unmodifiableList(_children) }

	init {
		this.parent = builder.parent
		this.crateTemplate = builder.crateTemplate ?: throw NullPointerException("crateTemplate")
		this.name = builder.name ?: throw NullPointerException("name")
		this.minTypes = builder.minTypes
		this.maxTypes = builder.maxTypes
		this.minPools = builder.minPools
		this.maxPools = builder.maxPools
		this.isRecycled = builder.recycled
		this.isAttrColdResistance = builder.attrColdResistance
		this.isAttrConductivity = builder.attrConductivity
		this.isAttrDecayResistance = builder.attrDecayResistance
		this.isAttrEntangleResistance = builder.attrEntangleResistance
		this.isAttrFlavor = builder.attrFlavor
		this.isAttrHeatResistance = builder.attrHeatResistance
		this.isAttrMalleability = builder.attrMalleability
		this.isAttrOverallQuality = builder.attrOverallQuality
		this.isAttrPotentialEnergy = builder.attrPotentialEnergy
		this.isAttrShockResistance = builder.attrShockResistance
		this.isAttrUnitToughness = builder.attrUnitToughness

		Arguments.validate(minTypes != -1, "minTypes must be initialized")
		Arguments.validate(maxTypes != -1, "maxTypes must be initialized")
		Arguments.validate(minPools != -1, "minPools must be initialized")
		Arguments.validate(maxPools != -1, "maxPools must be initialized")
		this.resourceType = getRawResourceType(this)
		parent?._children?.add(this)
	}

	override fun toString(): String {
		return "RawResource[id=$id name=$name type=$resourceType]"
	}

	class RawResourceBuilder(val id: Long) {
		var name: String? = null
			private set
		var parent: RawResource? = null
			private set
		var crateTemplate: String? = null
			private set
		var minTypes: Int = -1
			private set
		var maxTypes: Int = -1
			private set
		var minPools: Int = -1
			private set
		var maxPools: Int = -1
			private set
		var attrColdResistance: Boolean = false
			private set
		var attrConductivity: Boolean = false
			private set
		var attrDecayResistance: Boolean = false
			private set
		var attrEntangleResistance: Boolean = false
			private set
		var attrFlavor: Boolean = false
			private set
		var attrHeatResistance: Boolean = false
			private set
		var attrMalleability: Boolean = false
			private set
		var attrOverallQuality: Boolean = false
			private set
		var attrPotentialEnergy: Boolean = false
			private set
		var attrShockResistance: Boolean = false
			private set
		var attrUnitToughness: Boolean = false
			private set
		var recycled: Boolean = false
			private set

		fun setName(name: String?): RawResourceBuilder {
			this.name = name
			return this
		}

		fun setParent(parent: RawResource?): RawResourceBuilder {
			this.parent = parent
			return this
		}

		fun setCrateTemplate(crateTemplate: String?): RawResourceBuilder {
			this.crateTemplate = crateTemplate
			return this
		}

		fun setMinTypes(minTypes: Int): RawResourceBuilder {
			this.minTypes = minTypes
			return this
		}

		fun setMaxTypes(maxTypes: Int): RawResourceBuilder {
			this.maxTypes = maxTypes
			return this
		}

		fun setMinPools(minPools: Int): RawResourceBuilder {
			this.minPools = minPools
			return this
		}

		fun setMaxPools(maxPools: Int): RawResourceBuilder {
			this.maxPools = maxPools
			return this
		}

		fun setRecycled(recycled: Boolean): RawResourceBuilder {
			this.recycled = recycled
			return this
		}

		fun setAttrColdResistance(attrColdResistance: Boolean): RawResourceBuilder {
			this.attrColdResistance = attrColdResistance
			return this
		}

		fun setAttrConductivity(attrConductivity: Boolean): RawResourceBuilder {
			this.attrConductivity = attrConductivity
			return this
		}

		fun setAttrDecayResistance(attrDecayResistance: Boolean): RawResourceBuilder {
			this.attrDecayResistance = attrDecayResistance
			return this
		}

		fun setAttrEntangleResistance(attrEntangleResistance: Boolean): RawResourceBuilder {
			this.attrEntangleResistance = attrEntangleResistance
			return this
		}

		fun setAttrFlavor(attrFlavor: Boolean): RawResourceBuilder {
			this.attrFlavor = attrFlavor
			return this
		}

		fun setAttrHeatResistance(attrHeatResistance: Boolean): RawResourceBuilder {
			this.attrHeatResistance = attrHeatResistance
			return this
		}

		fun setAttrMalleability(attrMalleability: Boolean): RawResourceBuilder {
			this.attrMalleability = attrMalleability
			return this
		}

		fun setAttrOverallQuality(attrOverallQuality: Boolean): RawResourceBuilder {
			this.attrOverallQuality = attrOverallQuality
			return this
		}

		fun setAttrPotentialEnergy(attrPotentialEnergy: Boolean): RawResourceBuilder {
			this.attrPotentialEnergy = attrPotentialEnergy
			return this
		}

		fun setAttrShockResistance(attrShockResistance: Boolean): RawResourceBuilder {
			this.attrShockResistance = attrShockResistance
			return this
		}

		fun setAttrUnitToughness(attrUnitToughness: Boolean): RawResourceBuilder {
			this.attrUnitToughness = attrUnitToughness
			return this
		}

		fun build(): RawResource {
			return RawResource(this)
		}
	}

	companion object {
		fun builder(id: Long): RawResourceBuilder {
			return RawResourceBuilder(id)
		}
	}
}
