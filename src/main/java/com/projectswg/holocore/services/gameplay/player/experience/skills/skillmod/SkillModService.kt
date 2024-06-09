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
package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod

import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.intents.support.objects.ContainerTransferIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class SkillModService : Service() {

	@IntentHandler
	private fun handlehandleContainerTransferIntent(cti: ContainerTransferIntent) {
		val owner = cti.obj.owner ?: return
		val creature = owner.creatureObject

		val obj = cti.obj
		if (obj is TangibleObject) {
			val skillMods: Map<String, Int> = obj.skillMods

			for (skillMod in skillMods) {
				val modName = skillMod.key
				val modValue = skillMod.value

				if (isEquippingItem(cti.container, creature)) {
					SkillModIntent(modName, 0, modValue, creature).broadcast()
				} else if (isUnequippingItem(cti.oldContainer, creature)) {
					SkillModIntent(modName, 0, -modValue, creature).broadcast()
				}
			}
		}
	}

	private fun isEquippingItem(container: SWGObject?, creature: CreatureObject): Boolean {
		return container != null && container.objectId == creature.objectId
	}

	private fun isUnequippingItem(oldContainer: SWGObject?, creature: CreatureObject): Boolean {
		return oldContainer != null && oldContainer.objectId == creature.objectId
	}

	@IntentHandler
	private fun handleSkillModIntent(smi: SkillModIntent) {
		for (creature in smi.affectedCreatures) {
			val skillModName = smi.skillModName
			val adjustBase = smi.adjustBase
			val adjustModifier = smi.adjustModifier
			creature.adjustSkillmod(skillModName, adjustBase, adjustModifier)
		}
	}
}