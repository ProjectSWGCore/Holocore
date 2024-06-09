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
package com.projectswg.holocore.intents.support.global.chat

import com.projectswg.common.data.encodables.chat.ChatAvatar
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.control.Intent

class ChatRoomUpdateIntent(val path: String, title: String?, target: String?, val avatar: ChatAvatar, val updateType: UpdateType) : Intent() {
	var title: String? = null
	var target: String? = null
	val message: String? = null
	var isPublic: Boolean = false
		private set
	var player: Player? = null
		private set
	var isIgnoreInvitation: Boolean = false
		private set

	init {
		when (updateType) {
			UpdateType.DESTROY -> {}
			UpdateType.CREATE  -> this.title = title
			else               -> this.target = target
		}
	}

	constructor(player: Player?, avatar: ChatAvatar, path: String, title: String?, isPublic: Boolean) : this(path, title, null, avatar, UpdateType.CREATE) {
		this.player = player
		this.isPublic = isPublic
	}

	constructor(avatar: ChatAvatar, path: String, updateType: UpdateType) : this(path, null, null, avatar, updateType)

	constructor(player: Player, path: String, updateType: UpdateType) : this(ChatAvatar(player.characterChatName), path, updateType) {
		this.player = player
	}

	enum class UpdateType {
		CREATE,
		DESTROY,
		JOIN,
		LEAVE,
		MODERATORS_ADD_TARGET,
		MODERATORS_REMOVE_TARGET,
		BANNED_ADD_TARGET,
		BANNED_REMOVE_TARGET,
		INVITED_ADD_TARGET,
		INVITED_REMOVE_TARGET
	}
}
