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
package com.projectswg.holocore.resources.support.npc.spawn

import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.PatrolFormation
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.SpawnerFlag
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior

class SimpleSpawnInfo private constructor() : SpawnInfo {
	override var id: String = ""
		private set
	override var terrain: Terrain = Terrain.SIMPLE
		private set
	override var x: Double = 0.0
		private set
	override var y: Double = 0.0
		private set
	override var z: Double = 0.0
		private set
	override var heading: Int = 0
		private set
	override var cellId: Int = 0
		private set
	override var spawnerType: String = ""
		private set
	override var npcId: String = ""
		private set
	override var spawnerFlag: SpawnerFlag = SpawnerFlag.INVULNERABLE
		private set
	override var difficulty: CreatureDifficulty = CreatureDifficulty.NORMAL
		private set
	override var minLevel: Int = 0
		private set
	override var maxLevel: Int = 0
		private set
	override var buildingId: String = ""
		private set
	override var mood: String = ""
		private set
	override var behavior: AIBehavior = AIBehavior.IDLE
		private set
	override var patrolId: String = ""
		private set
	override val patrolFormation: PatrolFormation = PatrolFormation.NONE
	override var loiterRadius: Int = 0
		private set
	override var minSpawnTime: Int = 0
		private set
	override var maxSpawnTime: Int = 0
		private set
	override var amount: Int = 0
		private set

	override val conversationId: String = ""

	override val equipmentId: Long?
		get() = null

	class Builder {
		private val info = SimpleSpawnInfo()

		init {
			info.id = "SPAWNER"
			info.behavior = AIBehavior.IDLE
			info.mood = ""
			info.spawnerFlag = SpawnerFlag.INVULNERABLE
			info.buildingId = ""
			info.amount = 1
			info.minSpawnTime = 0
			info.maxSpawnTime = 0
			info.loiterRadius = 15
		}

		fun withNpcId(npcId: String): Builder {
			info.npcId = npcId
			info.patrolId = ""

			return this
		}

		fun withLocation(location: Location): Builder {
			info.x = location.x
			info.y = location.y
			info.z = location.z
			info.terrain = location.terrain
			info.heading = location.yaw.toInt()

			return this
		}

		fun withBuildingId(buildingId: String): Builder {
			info.buildingId = buildingId

			return this
		}

		fun withCellId(cellId: Int): Builder {
			info.cellId = cellId

			return this
		}

		fun withDifficulty(difficulty: CreatureDifficulty): Builder {
			info.difficulty = difficulty

			return this
		}

		fun withMinLevel(minLevel: Int): Builder {
			info.minLevel = minLevel

			return this
		}

		fun withMaxLevel(maxLevel: Int): Builder {
			info.maxLevel = maxLevel

			return this
		}

		fun withSpawnerFlag(spawnerFlag: SpawnerFlag): Builder {
			info.spawnerFlag = spawnerFlag

			return this
		}

		fun withSpawnerType(spawnerType: SpawnerType): Builder {
			info.spawnerType = spawnerType.name

			return this
		}

		fun withAmount(amount: Int): Builder {
			info.amount = amount

			return this
		}

		fun withBehavior(behavior: AIBehavior): Builder {
			info.behavior = behavior

			return this
		}

		fun build(): SimpleSpawnInfo {
			return info
		}
	}

	companion object {
		fun builder(): Builder {
			return Builder()
		}
	}
}
