package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService;

public final class CmdGroupInvite implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		Player farAwayTarget = null;
		
		if (args != null) {
			farAwayTarget = CharacterLookupService.PlayerLookup.getPlayerByFirstName(args);
		}
		
		if (farAwayTarget != null) {
			new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_INVITE, player, farAwayTarget.getCreatureObject()).broadcast();
		} else {
			if (target instanceof CreatureObject) {
				new GroupEventIntent(GroupEventIntent.GroupEventType.GROUP_INVITE, player, (CreatureObject) target).broadcast();
			} else {
				SystemMessageIntent.broadcastPersonal(player, "@group:invite_no_target_self");
			}
		}
	}
	
}
