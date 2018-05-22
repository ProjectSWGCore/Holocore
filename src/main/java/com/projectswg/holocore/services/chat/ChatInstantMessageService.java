package com.projectswg.holocore.services.chat;

import com.projectswg.common.data.encodables.chat.ChatResult;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToCharacter;
import com.projectswg.common.network.packets.swg.zone.chat.ChatInstantMessageToClient;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnSendInstantMessage;
import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.PlayerState;
import com.projectswg.holocore.services.player.PlayerManager.PlayerLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Locale;

public class ChatInstantMessageService extends Service {
	
	public ChatInstantMessageService() {
		
	}
	
	@IntentHandler
	private void handleGalacticPacketIntent(GalacticPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		switch (packet.getPacketType()) {
			case CHAT_INSTANT_MESSAGE_TO_CHARACTER:
				if (packet instanceof ChatInstantMessageToCharacter)
					handleInstantMessage(gpi.getPlayer(), (ChatInstantMessageToCharacter) packet);
				break;
			default:
				break;
		}
	}
	
	private void handleInstantMessage(Player sender, ChatInstantMessageToCharacter request) {
		String strReceiver = request.getCharacter().toLowerCase(Locale.ENGLISH);
		String strSender = sender.getCharacterFirstName();
		
		Player receiver = PlayerLookup.getPlayerByFirstName(strReceiver);
		
		ChatResult result = ChatResult.SUCCESS;
		if (receiver == null || receiver.getPlayerState() != PlayerState.ZONED_IN)
			result = ChatResult.TARGET_AVATAR_DOESNT_EXIST;
		else if (receiver.getPlayerObject().isIgnored(strSender))
			result = ChatResult.IGNORED;
		
		sender.sendPacket(new ChatOnSendInstantMessage(result.getCode(), request.getSequence()));
		
		if (result != ChatResult.SUCCESS)
			return;
		
		receiver.sendPacket(new ChatInstantMessageToClient(request.getGalaxy(), strSender, request.getMessage()));
	}
	
}
