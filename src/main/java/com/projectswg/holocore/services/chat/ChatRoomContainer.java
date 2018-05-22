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
package com.projectswg.holocore.services.chat;

import com.projectswg.common.data.encodables.chat.ChatRoom;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRoomContainer {
	
	private final Object mapMutex;
	private final Map<Integer, ChatRoom> idMap;
	private final Map<String, ChatRoom> pathMap;
	
	public ChatRoomContainer() {
		this.mapMutex = new Object();
		this.idMap = new HashMap<>();
		this.pathMap = new HashMap<>();
	}
	
	public boolean addRoom(@NotNull ChatRoom room) {
		assert room.getId() > 0 : "ChatRoom ID must be > 0";
		assert room.getPath() != null : "path must not be null";
		assert !room.getPath().isEmpty() : "path must be non-empty";
		assert room.getPath().startsWith("SWG.") : "Path must start with \"SWG.\"!";
		synchronized (mapMutex) {
			ChatRoom overwrittenId = idMap.put(room.getId(), room);
			ChatRoom overwrittenPath = pathMap.put(room.getPath(), room);
			if (overwrittenId != null || overwrittenPath != null) {
				if (overwrittenId != overwrittenPath)
					Log.w("Internal maps in ChatRoomContainer are out of sync! ID and Path don't match for room: %s", room);
				if (overwrittenId != null)
					idMap.put(overwrittenId.getId(), overwrittenId);
				else
					idMap.put(overwrittenPath.getId(), overwrittenPath);
				if (overwrittenPath != null)
					pathMap.put(overwrittenPath.getPath(), overwrittenPath);
				else
					pathMap.put(overwrittenId.getPath(), overwrittenId);
				return false;
			}
		}
		return true;
	}
	
	public boolean destroyRoom(@NotNull ChatRoom room) {
		synchronized (mapMutex) {
			boolean success = idMap.remove(room.getId()) != null;
			return pathMap.remove(room.getPath()) != null && success;
		}
	}
	
	public List<ChatRoom> getAllRooms() {
		synchronized (mapMutex) {
			return new ArrayList<>(idMap.values());
		}
	}
	
	public ChatRoom getRoomById(int roomId) {
		assert roomId > 0 : "ChatRoom ID must be > 0";
		synchronized (mapMutex) {
			return idMap.get(roomId);
		}
	}
	
	public ChatRoom getRoomByPath(@NotNull String path) {
		assert !path.isEmpty() : "path must be non-empty";
		assert path.startsWith("SWG.") : "Path must start with \"SWG.\"!";
		synchronized (mapMutex) {
			return pathMap.get(path);
		}
	}
	
	public boolean hasRoomWithId(int roomId) {
		assert roomId > 0 : "ChatRoom ID must be > 0";
		synchronized (mapMutex) {
			return idMap.containsKey(roomId);
		}
	}
	
	public boolean hasRoomWithPath(@NotNull String path) {
		assert !path.isEmpty() : "path must be non-empty";
		assert path.startsWith("SWG.") : "Path must start with \"SWG.\"!";
		synchronized (mapMutex) {
			return pathMap.containsKey(path);
		}
	}
	
}
