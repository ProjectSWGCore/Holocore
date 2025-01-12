/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.services.gameplay.combat.command

import com.projectswg.common.data.combat.DelayAttackEggPosition
import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus
import com.projectswg.holocore.resources.support.global.commands.CombatCommand
import com.projectswg.holocore.resources.support.global.commands.Command
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import kotlinx.coroutines.*
import me.joshlarson.jlcommon.log.Log

internal class CombatCommandDelayAttack(private val combatCommandAttack: CombatCommandAttack) : CombatCommandHitType {
	private val coroutineScope = CoroutineScope(context = Dispatchers.Default)

	override fun terminate() {
		coroutineScope.cancel()
	}

	override fun handle(source: CreatureObject, target: SWGObject?, command: Command, combatCommand: CombatCommand, arguments: String): CombatStatus {
		val argSplit = arguments.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
		val eggLocation: Location
		val eggParent: SWGObject?

		when (combatCommand.eggPosition) {
			DelayAttackEggPosition.LOCATION -> {
				eggLocation = if (argSplit[0] == "a" || argSplit[0] == "c") {    // is "c" in free-targeting mode
					source.location
				} else {
					Location(argSplit[0].toFloat().toDouble(), argSplit[1].toFloat().toDouble(), argSplit[2].toFloat().toDouble(), source.terrain)
				}

				eggParent = source.parent
			}

			DelayAttackEggPosition.SELF     -> {
				eggLocation = source.location
				eggParent = source.parent
			}

			DelayAttackEggPosition.TARGET   -> {
				eggLocation = target!!.location
				eggParent = target.parent
			}

			else                            -> {
				Log.w("Unrecognised delay egg position %s from command %s - defaulting to SELF", combatCommand.eggPosition, combatCommand.name)
				eggLocation = source.location
				eggParent = source.parent
			}
		}

		// Spawn delay egg object
		val eggTemplate = combatCommand.delayAttackEggTemplate
		val delayEgg = if (eggTemplate.endsWith("generic_egg_small.iff")) null else ObjectCreator.createObjectFromTemplate(eggTemplate)

		if (delayEgg != null) {
			delayEgg.moveToContainer(eggParent, eggLocation)
			ObjectCreatedIntent(delayEgg).broadcast()
		}

		coroutineScope.launch { delayEggLoop(delayEgg, source, target, combatCommand) }
		return CombatStatus.SUCCESS
	}

	private suspend fun delayEggLoop(delayEgg: SWGObject?, source: CreatureObject, target: SWGObject?, combatCommand: CombatCommand) {
		val delayAttackParticle = combatCommand.delayAttackParticle
		try {
			for (currentLoop in 1..combatCommand.delayAttackLoops) {
				delay((combatCommand.delayAttackInterval * 1000).toLong())

				// Show particle effect to everyone observing the delay egg, if one is defined
				if (delayEgg != null && delayAttackParticle.isNotEmpty()) {
					delayEgg.sendObservers(PlayClientEffectObjectMessage(delayAttackParticle, "", delayEgg.objectId, ""))
				}

				// Handle the attack of this loop
				combatCommandAttack.handle(source, target, delayEgg, combatCommand)
			}
		} finally {
			if (delayEgg != null) DestroyObjectIntent(delayEgg).broadcast()
		}
	}
}
