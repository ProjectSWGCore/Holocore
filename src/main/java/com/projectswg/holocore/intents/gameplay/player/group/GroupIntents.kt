package com.projectswg.holocore.intents.gameplay.player.group

import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.Intent

class GroupEventInvite(val player: Player, val target: CreatureObject): Intent()
class GroupEventUninvite(val player: Player, val target: CreatureObject): Intent()
class GroupEventMakeLeader(val player: Player, val target: CreatureObject): Intent()
class GroupEventKick(val player: Player, val target: CreatureObject): Intent()
class GroupEventMakeMasterLooter(val player: Player, val target: CreatureObject): Intent()
class GroupEventJoin(val player: Player): Intent()
class GroupEventDecline(val player: Player): Intent()
class GroupEventDisband(val player: Player): Intent()
class GroupEventLeave(val player: Player): Intent()
class GroupEventLoot(val player: Player): Intent()
