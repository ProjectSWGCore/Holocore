/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.support.data.server_info.loader.conversation.events

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.holocore.resources.gameplay.conversation.events.EmitSystemMessageEvent
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.loader.conversation.EventParser

class SystemMessageEventParser : EventParser<EmitSystemMessageEvent> {
	override fun parse(args: Map<String, Any>): EmitSystemMessageEvent {
		if (args.containsKey("text")) {
			val text = args["text"] as String
			return EmitSystemMessageEvent(text)
		} else if (args.containsKey("file")) {
			val file = args["file"] as String
			val key = args["key"] as String? ?: throw IllegalArgumentException("Args must contain file and key")
			return EmitSystemMessageEvent(ServerData.strings[StringId(file, key)] ?: throw IllegalArgumentException("No file/key found"))
		}
		throw IllegalArgumentException("Args must contain either text or file/key")
	}
}
