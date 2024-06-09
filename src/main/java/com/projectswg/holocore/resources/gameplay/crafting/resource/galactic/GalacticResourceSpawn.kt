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
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.*

class GalacticResourceSpawn() : MongoPersistable {
	// Resource-based
	var resourceId: Long = 0
		private set
	var minConcentration: Int = 0
		private set
	var maxConcentration: Int = 0
		private set

	// Location-based
	var terrain: Terrain = Terrain.DEV_AREA
		private set
	private var x = 0
	private var z = 0
	var radius: Int = 0
		private set

	// Time-based
	var startTime: Instant = Instant.EPOCH
		private set
	var endTime: Instant = Instant.EPOCH
		private set

	constructor(resourceId: Long, terrain: Terrain) : this() {
		this.resourceId = resourceId
		this.terrain = terrain
	}

	fun setRandomValues(terrain: Terrain) {
		val random = ThreadLocalRandom.current()

		this.minConcentration = random.nextInt(50)
		this.maxConcentration = calculateRandomMaxConcentration(random, minConcentration)

		this.terrain = terrain
		setPosition(random, terrain)
		this.radius = calculateRandomRadius(random)

		val minSpawnTime = minSpawnTime
		val maxSpawnTime = maxSpawnTime
		this.startTime = Instant.now()
		this.endTime = startTime.plus(random.nextInt(minSpawnTime, maxSpawnTime).toLong(), ChronoUnit.DAYS)
	}

	fun getX(): Double {
		return x.toDouble()
	}

	fun getZ(): Double {
		return z.toDouble()
	}

	fun getConcentration(terrain: Terrain, x: Double, z: Double): Int {
		val distance = getDistance(terrain, x, z)
		if (distance > radius) return 0
		var factor = (1 - distance / radius)
		factor *= factor // creates a more serious dropoff of concentration
		return (factor * (maxConcentration - minConcentration) + minConcentration).toInt()
	}

	val isExpired: Boolean
		get() = Instant.now().isAfter(endTime)

	private val minSpawnTime: Int
		get() = config.getInt(this, "resourceMinSpawnTime", 7)

	private val maxSpawnTime: Int
		get() = config.getInt(this, "resourceMaxSpawnTime", 21)

	private val minRadius: Int
		get() = config.getInt(this, "resourceMinSpawnRadius", 200)

	private val maxRadius: Int
		get() = config.getInt(this, "resourceMaxSpawnRadius", 500)

	private fun calculateRandomMaxConcentration(random: Random, min: Int): Int {
		var x: Double
		do {
			x = random.nextDouble()
			x *= x * 100
		} while (x <= min)
		return x.toInt()
	}

	private fun setPosition(random: Random, terrain: Terrain) {
		val angle = random.nextDouble() * 6.283185307
		var distance = max(-1.0, min(1.0, random.nextGaussian() / 6 + 0.5))
		distance *= when (terrain) {
			Terrain.MUSTAFAR      -> POSITION_MUST_GAUSSIAN_FACTOR
			Terrain.KASHYYYK_MAIN -> POSITION_KASH_GAUSSIAN_FACTOR
			else                  -> POSITION_GAUSSIAN_FACTOR
		}
		this.x = capPosition((cos(angle) * distance).toInt())
		this.z = capPosition((sin(angle) * distance).toInt())
		if (terrain == Terrain.MUSTAFAR) {
			x += -2880
			z += 2976
		}
	}

	private fun calculateRandomRadius(random: Random): Int {
		var x = random.nextDouble()
		x = sqrt(x)
		return (x * (maxRadius - minRadius) + minRadius).toInt()
	}

	private fun capPosition(x: Int): Int {
		return max((-MAP_SIZE / 2).toDouble(), min((MAP_SIZE / 2).toDouble(), x.toDouble())).toInt()
	}

	override fun readMongo(data: MongoData) {
		run {
			val resource = data.getDocument("resource")
			resourceId = resource.getLong("id", resourceId)
			minConcentration = resource.getInteger("minConcentration", minConcentration)
			maxConcentration = resource.getInteger("maxConcentration", maxConcentration)
		}
		run {
			val location = data.getDocument("location")
			terrain = Terrain.valueOf(location.getString("terrain", Terrain.TATOOINE.name))
			x = location.getInteger("x", x)
			z = location.getInteger("z", z)
			radius = location.getInteger("radius", radius)
		}
		run {
			val time = data.getDocument("time")
			startTime = time.getDate("start", Instant.EPOCH)
			endTime = time.getDate("end", Instant.EPOCH)
		}
	}

	override fun saveMongo(data: MongoData) {
		run {
			val resource = data.getDocument("resource")
			resource.putLong("id", resourceId)
			resource.putInteger("minConcentration", minConcentration)
			resource.putInteger("maxConcentration", maxConcentration)
		}
		run {
			val location = data.getDocument("location")
			location.putString("terrain", terrain.name)
			location.putInteger("x", x)
			location.putInteger("z", z)
			location.putInteger("radius", radius)
		}
		run {
			val time = data.getDocument("time")
			time.putDate("start", startTime)
			time.putDate("end", endTime)
		}
	}

	private fun getDistance(terrain: Terrain, x: Double, z: Double): Double {
		if (this.terrain != terrain) return Double.MAX_VALUE
		return sqrt(square(this.x - x) + square(this.z - z))
	}

	private fun square(x: Double): Double {
		return x * x
	}

	override fun toString(): String {
		return "GalacticResourceSpawn[Resource ID=$resourceId  Concentration=($minConcentration, $maxConcentration)  Position=($x, $z, $terrain]"
	}

	override fun equals(other: Any?): Boolean {
		if (other !is GalacticResourceSpawn) return false
		if (other.resourceId != resourceId) return false
		if (other.minConcentration != minConcentration || other.maxConcentration != maxConcentration) return false
		if (other.terrain != terrain || other.x != x || other.z != z || other.radius != radius) return false
		return other.startTime === startTime && other.endTime === endTime
	}

	override fun hashCode(): Int {
		return java.lang.Long.hashCode(resourceId) * 7 + x * 23 + z * 89
	}

	companion object {
		private const val MAP_SIZE = 16384
		private const val MUST_MAP_SIZE = 8000
		private const val KASH_MAP_SIZE = 8192
		private val POSITION_GAUSSIAN_FACTOR = MAP_SIZE / 2.0 * sqrt(2.0)
		private val POSITION_MUST_GAUSSIAN_FACTOR = MUST_MAP_SIZE / 2.0 * sqrt(2.0)
		private val POSITION_KASH_GAUSSIAN_FACTOR = KASH_MAP_SIZE / 2.0 * sqrt(2.0)
	}
}
