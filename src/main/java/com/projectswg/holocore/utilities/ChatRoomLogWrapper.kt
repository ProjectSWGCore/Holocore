/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.utilities

import com.projectswg.holocore.intents.support.global.chat.SystemChatRoomMessageIntent
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.jlcommon.log.LogWrapper

class ChatRoomLogWrapper(private val roomPath: String) : LogWrapper {
	
	override fun onLog(level: Log.LogLevel, str: String) {
		val message = when (level) {
			Log.LogLevel.TRACE  -> return
			Log.LogLevel.DATA   -> " \\#5555FF\\D: "+str.substringAfter(": ")
			Log.LogLevel.INFO   -> " \\#00FF00\\I: "+str.substringAfter(": ")
			Log.LogLevel.WARN   -> " \\#FFFF00\\W: "+str.substringAfter(": ")
			Log.LogLevel.ERROR  -> " \\#FF0000\\E: "+str.substringAfter(": ")
			Log.LogLevel.ASSERT -> " \\#FF00FF\\A: "+str.substringAfter(": ")
		}
		SystemChatRoomMessageIntent.broadcast(roomPath, message)
	}
	
}
