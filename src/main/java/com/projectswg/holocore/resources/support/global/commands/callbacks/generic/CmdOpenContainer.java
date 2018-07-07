package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

public final class CmdOpenContainer implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (!target.isVisible(player.getCreatureObject())) {
			SystemMessageIntent.broadcastPersonal(player, "@container_error_message:container08");
			return;
		}
		
		Log.d("Opening Container %s", target);
		player.sendPacket(new ClientOpenContainerMessage(target.getObjectId(), ""));
	}
	
}
