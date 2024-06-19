/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.resources.support.global.commands.callbacks.admin

import com.projectswg.holocore.intents.gameplay.world.CreateSpawnIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.npc.spawn.SimpleSpawnInfo
import com.projectswg.holocore.resources.support.npc.spawn.SpawnerType
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty

class CmdCreateNpc : ICmdCallback {
	override fun execute(player: Player, target: SWGObject?, args: String) {
		val commandArgs = args.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val npcId = determineNpcId(commandArgs)
		val difficulty = determineDifficulty(commandArgs)
		val combatLevel = determineCombatLevel(commandArgs)

		spawnNPC(player, npcId, difficulty, combatLevel)
	}

	private fun determineNpcId(commandArgs: Array<String>): String {
		return commandArgs[0]
	}

	private fun determineDifficulty(commandArgs: Array<String>): CreatureDifficulty {
		val arg = commandArgs[1]

		return when (arg) {
			"b"  -> CreatureDifficulty.BOSS
			"e"  -> CreatureDifficulty.ELITE
			"n"  -> CreatureDifficulty.NORMAL
			else -> CreatureDifficulty.NORMAL
		}
	}

	private fun determineCombatLevel(commandArgs: Array<String>): Int {
		return commandArgs[2].toInt()
	}

	private fun spawnNPC(player: Player, npcId: String, difficulty: CreatureDifficulty, combatLevel: Int) {
		val cell = player.creatureObject.parent as CellObject?
		val spawnInfo = SimpleSpawnInfo.builder().withNpcId(npcId).withDifficulty(difficulty).withMinLevel(combatLevel).withMaxLevel(combatLevel).withLocation(player.creatureObject.location).withSpawnerFlag(NpcStaticSpawnLoader.SpawnerFlag.ATTACKABLE).withSpawnerType(SpawnerType.EGG).withBuildingId(determineBuildoutTag(cell)).withCellId(determineCellId(cell)).build()

		CreateSpawnIntent(spawnInfo).broadcast()
	}

	private fun determineBuildoutTag(cell: CellObject?): String {
		if (cell == null) {
			return ""
		}

		val building = cell.parent ?: return ""

		return building.buildoutTag
	}

	private fun determineCellId(cell: CellObject?): Int {
		if (cell == null) {
			return 0
		}


		return cell.number
	}
}
