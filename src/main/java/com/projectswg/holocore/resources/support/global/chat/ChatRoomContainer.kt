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
package com.projectswg.holocore.resources.support.global.chat

import com.projectswg.common.data.encodables.chat.ChatRoom
import me.joshlarson.jlcommon.log.Log

class ChatRoomContainer {
	private val mapMutex = Any()
	private val idMap: MutableMap<Int, ChatRoom?> = HashMap()
	private val pathMap: MutableMap<String, ChatRoom?> = HashMap()

	fun addRoom(room: ChatRoom): Boolean {
		assert(room.id > 0) { "ChatRoom ID must be > 0" }
		checkNotNull(room.path) { "path must not be null" }
		assert(!room.path.isEmpty()) { "path must be non-empty" }
		assert(room.path.startsWith("SWG.")) { "Path must start with \"SWG.\"!" }
		synchronized(mapMutex) {
			val overwrittenId = idMap.put(room.id, room)
			val overwrittenPath = pathMap.put(room.path, room)
			if (overwrittenId != null || overwrittenPath != null) {
				if (overwrittenId !== overwrittenPath) Log.w("Internal maps in ChatRoomContainer are out of sync! ID and Path don't match for room: %s", room)
				if (overwrittenId != null) idMap[overwrittenId.id] = overwrittenId
				else idMap[overwrittenPath!!.id] = overwrittenPath
				if (overwrittenPath != null) pathMap[overwrittenPath.path] = overwrittenPath
				else pathMap[overwrittenId!!.path] = overwrittenId
				return false
			}
		}
		return true
	}

	fun destroyRoom(room: ChatRoom): Boolean {
		synchronized(mapMutex) {
			val success = idMap.remove(room.id) != null
			return pathMap.remove(room.path) != null && success
		}
	}

	val allRooms: List<ChatRoom?>
		get() {
			synchronized(mapMutex) {
				return ArrayList(idMap.values)
			}
		}

	fun getRoomById(roomId: Int): ChatRoom? {
		assert(roomId > 0) { "ChatRoom ID must be > 0" }
		synchronized(mapMutex) {
			return idMap[roomId]
		}
	}

	fun getRoomByPath(path: String): ChatRoom? {
		assert(!path.isEmpty()) { "path must be non-empty" }
		assert(path.startsWith("SWG.")) { "Path must start with \"SWG.\"!" }
		synchronized(mapMutex) {
			return pathMap[path]
		}
	}

	fun hasRoomWithId(roomId: Int): Boolean {
		assert(roomId > 0) { "ChatRoom ID must be > 0" }
		synchronized(mapMutex) {
			return idMap.containsKey(roomId)
		}
	}

	fun hasRoomWithPath(path: String): Boolean {
		assert(!path.isEmpty()) { "path must be non-empty" }
		assert(path.startsWith("SWG.")) { "Path must start with \"SWG.\"!" }
		synchronized(mapMutex) {
			return pathMap.containsKey(path)
		}
	}
}
