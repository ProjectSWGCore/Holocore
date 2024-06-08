package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod

import com.projectswg.holocore.intents.gameplay.player.experience.SkillModIntent
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service

class SkillModService : Service() {

	@IntentHandler
	private fun handlehandleContainerTransferIntent(cti: ContainerTransferIntent) {
		val owner = cti.`object`.owner ?: return
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