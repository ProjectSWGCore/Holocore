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

import com.projectswg.common.data.location.Terrain
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.PatrolFormation
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.SpawnerFlag
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior

interface SpawnInfo {
	val id: String
	val terrain: Terrain
	val x: Double
	val y: Double
	val z: Double
	val heading: Int
	val cellId: Int
	val spawnerType: String
	val npcId: String
	val spawnerFlag: SpawnerFlag
	val difficulty: CreatureDifficulty
	val minLevel: Int
	val maxLevel: Int
	val buildingId: String
	val mood: String
	val behavior: AIBehavior
	val patrolId: String
	val patrolFormation: PatrolFormation
	val loiterRadius: Int
	val minSpawnTime: Int
	val maxSpawnTime: Int
	val amount: Int
	val conversationId: String
	val equipmentId: Long?
}
