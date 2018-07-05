package com.projectswg.holocore.resources.support.global.commands.callbacks.group;

import com.projectswg.holocore.intents.support.global.chat.ChatRoomMessageIntent;
import com.projectswg.holocore.intents.support.global.chat.ChatRoomUpdateIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import org.jetbrains.annotations.NotNull;

public final class CmdGroupChat implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null)
			return;
		
		long groupId = creature.getGroupId();
		if (groupId == 0)
			return;
		
		GroupObject group = (GroupObject) ObjectLookup.getObjectById(groupId);
		assert group != null : "groupId is set but no GroupObject exists";
		
		ChatRoomMessageIntent.broadcast(player, group.getChatRoomPath(), args);
	}
	
}
