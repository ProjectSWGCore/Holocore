package com.projectswg.holocore.services.support.global.chat;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatFriendsListUpdate;
import com.projectswg.holocore.intents.support.global.chat.ChatAvatarRequestIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
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
	private void handleChatAvatarRequestIntent(ChatAvatarRequestIntent cari) {
		switch (cari.getRequestType()) {
			case TARGET_STATUS:
				sendTargetAvatarStatus(cari.getPlayer(), new ChatAvatar(cari.getTarget()));
				break;
			case FRIEND_ADD_TARGET:
				handleAddFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_REMOVE_TARGET:
				handleRemoveFriend(cari.getPlayer(), cari.getTarget());
				break;
			case FRIEND_LIST:
				handleRequestFriendList(cari.getPlayer());
				break;
			case IGNORE_ADD_TARGET:
				handleAddIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_REMOVE_TARGET:
				handleRemoveIgnored(cari.getPlayer(), cari.getTarget());
				break;
			case IGNORE_LIST:
				handleRequestIgnoreList(cari.getPlayer());
				break;
		}
	}
	
	private void updateChatAvatarStatus(Player player, boolean online) {
		if (online) {
			for (String friend : player.getPlayerObject().getFriendsList()) {
				sendTargetAvatarStatus(player, new ChatAvatar(friend));
			}
		}
		
		String name = player.getCharacterFirstName().toLowerCase(Locale.US);
		new NotifyPlayersPacketIntent(new ChatFriendsListUpdate(new ChatAvatar(name), online), null, p -> p.getPlayerState() == PlayerState.ZONED_IN && p.getPlayerObject().isFriend(name)).broadcast();
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
