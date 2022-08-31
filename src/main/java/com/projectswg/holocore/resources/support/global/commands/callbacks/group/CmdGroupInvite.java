package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.gameplay.player.group.GroupEventInvite;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CmdGroupInvite implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		Player farAwayTarget = null;
		
		if (!args.isEmpty()) {
			farAwayTarget = CharacterLookupService.PlayerLookup.getPlayerByFirstName(args);
		}
		
		if (farAwayTarget != null) {
			new GroupEventInvite(player, farAwayTarget.getCreatureObject()).broadcast();
		} else {
			if (target instanceof CreatureObject) {
				new GroupEventInvite(player, (CreatureObject) target).broadcast();
			} else {
				SystemMessageIntent.broadcastPersonal(player, "@group:invite_no_target_self");
			}
		}
	}
	
}
