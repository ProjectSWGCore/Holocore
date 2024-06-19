/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatFriendsListUpdate;
import com.projectswg.holocore.intents.support.global.chat.*;
import com.projectswg.holocore.intents.support.global.zone.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Locale;

public class ChatFriendService extends Service {
	
	public ChatFriendService() {
		
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				updateChatAvatarStatus(pei.getPlayer(), true);
				break;
			case PE_LOGGED_OUT:
				updateChatAvatarStatus(pei.getPlayer(), false);
				break;
			default:
				break;
		}
	}

	@IntentHandler
	private void handleChatAvatarGetTargetStatusIntent(ChatAvatarGetTargetStatusIntent cai) {
		sendTargetAvatarStatus(cai.getPlayer(), new ChatAvatar(cai.getTarget()));
	}

	@IntentHandler
	private void handleChatAvatarGetFriendListIntent(ChatAvatarGetFriendListIntent cai) {
		handleRequestFriendList(cai.getPlayer());
	}

	@IntentHandler
	private void handleChatAvatarAddFriendIntent(ChatAvatarAddFriendIntent cai) {
		handleAddFriend(cai.getPlayer(), cai.getTarget());
	}

	@IntentHandler
	private void handleChatAvatarRemoveFriendIntent(ChatAvatarRemoveFriendIntent cai) {
		handleRemoveFriend(cai.getPlayer(), cai.getTarget());
	}

	@IntentHandler
	private void handleChatAvatarGetIgnoreListIntent(ChatAvatarGetIgnoreListIntent cai) {
		handleRequestIgnoreList(cai.getPlayer());
	}

	@IntentHandler
	private void handleChatAvatarAddIgnoreIntent(ChatAvatarAddIgnoreIntent cai) {
		handleAddIgnored(cai.getPlayer(), cai.getTarget());
	}

	@IntentHandler
	private void handleChatAvatarRemoveIgnoreIntent(ChatAvatarRemoveIgnoreIntent cai) {
		handleRemoveIgnored(cai.getPlayer(), cai.getTarget());
	}
	
	private void updateChatAvatarStatus(Player player, boolean online) {
		if (online) {
			for (String friend : player.getPlayerObject().getFriendsList()) {
				sendTargetAvatarStatus(player, new ChatAvatar(friend));
			}
		}
		
		String name = player.getCharacterFirstName().toLowerCase(Locale.US);
		new NotifyPlayersPacketIntent(new ChatFriendsListUpdate(new ChatAvatar(name), online), p -> p.getPlayerState() == PlayerState.ZONED_IN && p.getPlayerObject().isFriend(name), null).broadcast();
	}
	
	private void sendTargetAvatarStatus(Player player, ChatAvatar target) {
		Player targetPlayer = PlayerLookup.getPlayerByFirstName(target.getName());
		
		player.sendPacket(new ChatFriendsListUpdate(target, targetPlayer != null && targetPlayer.getPlayerState() == PlayerState.ZONED_IN));
	}
	
	private void handleAddFriend(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterFirstName()))
			return;
		
		PlayerObject ghost = player.getPlayerObject();
		if (ghost == null)
			return;
		
		if (ghost.isIgnored(target)) {
			sendSystemMessage(player, "@cmnty:friend_fail_is_ignored", target);
			return;
		}
		
		if (!PlayerLookup.doesCharacterExistByFirstName(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}
		
		if (!ghost.addFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_duplicate", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:friend_added", target);
		sendTargetAvatarStatus(player, new ChatAvatar(target));
	}
	
	private void handleRemoveFriend(Player player, String target) {
		if (!player.getPlayerObject().removeFriend(target)) {
			sendSystemMessage(player, "@cmnty:friend_not_found", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:friend_removed", target);
	}
	
	private void handleRequestFriendList(Player player) {
		player.getPlayerObject().sendFriendList();
	}
	
	/* Ignore List */
	
	private void handleAddIgnored(Player player, String target) {
		if (target.equalsIgnoreCase(player.getCharacterFirstName()))
			return;
		
		if (!PlayerLookup.doesCharacterExistByFirstName(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}
		
		player.getPlayerObject().removeFriend(target);
		
		if (!player.getPlayerObject().addIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_duplicate", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:ignore_added", target);
	}
	
	private void handleRemoveIgnored(Player player, String target) {
		if (!player.getPlayerObject().removeIgnored(target)) {
			sendSystemMessage(player, "@cmnty:ignore_not_found", target);
			return;
		}
		
		sendSystemMessage(player, "@cmnty:ignore_removed", target);
	}
	
	private void handleRequestIgnoreList(Player player) {
		player.getPlayerObject().sendIgnoreList();
	}
	
	private void sendSystemMessage(Player player, String stringId, String target) {
		new SystemMessageIntent(player, new ProsePackage("StringId", stringId, "TT", target)).broadcast();
	}
	
}
