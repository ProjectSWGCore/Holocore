package com.projectswg.holocore.resources.gameplay.conversation.events

import com.projectswg.holocore.resources.gameplay.conversation.model.Event
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.services.gameplay.training.AvailableSkillsWindow

class ShowAvailableSkillsEvent(private val professionName: String) : Event {
	override fun trigger(player: Player, npc: AIObject) {
		val availableSkillsWindow = AvailableSkillsWindow()

		availableSkillsWindow.show(professionName, player)
	}
}
