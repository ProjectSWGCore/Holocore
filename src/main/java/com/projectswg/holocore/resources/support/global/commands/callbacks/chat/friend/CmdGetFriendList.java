package com.projectswg.holocore.resources.support.global.commands.callbacks.chat.friend;

import com.projectswg.holocore.intents.support.global.chat.ChatAvatarRequestIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public final class CmdGetFriendList implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		new ChatAvatarRequestIntent(player, null, ChatAvatarRequestIntent.RequestType.FRIEND_LIST).broadcast();
	}
	
}
