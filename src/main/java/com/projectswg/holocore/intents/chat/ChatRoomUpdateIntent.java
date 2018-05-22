/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.intents.chat;

import com.projectswg.common.data.encodables.chat.ChatAvatar;
import com.projectswg.holocore.resources.player.Player;
import me.joshlarson.jlcommon.control.Intent;

public class ChatRoomUpdateIntent extends Intent {
	
	private String path;
	private String title;
	private String target;
	private ChatAvatar avatar;
	private String message;
	private UpdateType updateType;
	private boolean isPublic;
	private Player player;
	private boolean ignoreInvitation;
	
	public ChatRoomUpdateIntent(String path, String title, String target, ChatAvatar avatar, String message, UpdateType updateType) {
		this.path = path;
		this.title = null;
		this.target = null;
		this.avatar = avatar;
		this.message = null;
		this.updateType = updateType;
		this.isPublic = false;
		this.player = null;
		this.ignoreInvitation = false;
		
		switch (updateType) {
			case DESTROY:
				break;
			case CREATE:
				this.title = title;
				break;
			case SEND_MESSAGE:
				this.message = message;
				break;
			default:
				this.target = target;
				break;
		}
	}
	
	public ChatRoomUpdateIntent(Player player, String path, String title, String target, String message, UpdateType updateType, boolean ignoreInvitation) {
		this(path, title, target, new ChatAvatar(player.getCharacterChatName()), message, updateType);
		this.player = player;
		this.ignoreInvitation = ignoreInvitation;
	}
	
	public ChatRoomUpdateIntent(Player player, ChatAvatar avatar, String path, String title, boolean isPublic) {
		this(path, title, null, avatar, null, UpdateType.CREATE);
		this.player = player;
		this.isPublic = isPublic;
	}
	
	public ChatRoomUpdateIntent(ChatAvatar avatar, String path, UpdateType updateType) {
		this(path, null, null, avatar, null, updateType);
	}
	
	public ChatRoomUpdateIntent(Player player, String path, UpdateType updateType) {
		this(new ChatAvatar(player.getCharacterChatName()), path, updateType);
		this.player = player;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getTitle() {
		return title;
	}
	
	public String getTarget() {
		return target;
	}
	
	public ChatAvatar getAvatar() {
		return avatar;
	}
	
	public String getMessage() {
		return message;
	}
	
	public UpdateType getUpdateType() {
		return updateType;
	}
	
	public boolean isPublic() {
		return isPublic;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public boolean isIgnoreInvitation() {
		return ignoreInvitation;
	}
	
	public enum UpdateType {
		CREATE,
		DESTROY,
		JOIN,
		LEAVE,
		MODERATORS_ADD_TARGET,
		MODERATORS_REMOVE_TARGET,
		BANNED_ADD_TARGET,
		BANNED_REMOVE_TARGET,
		INVITED_ADD_TARGET,
		INVITED_REMOVE_TARGET,
		SEND_MESSAGE
	}
}
