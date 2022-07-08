package com.projectswg.holocore.intents.gameplay.player.group

import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.Intent

class GroupEventIntent @JvmOverloads constructor(val eventType: GroupEventType, val player: Player, val target: CreatureObject? = null) : Intent() {

	enum class GroupEventType {
		INVITE,
		UNINVITE,
		JOIN,
		DECLINE,
		DISBAND,
		LEAVE,
		MAKE_LEADER,
		KICK,
		MAKE_MASTER_LOOTER,
		LOOT
	}
}