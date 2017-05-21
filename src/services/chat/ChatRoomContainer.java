/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;

import resources.chat.ChatRoom;

public class ChatRoomContainer {
	
	private final Object mapMutex;
	private final Map<Integer, ChatRoom> idMap;
	private final Map<String, ChatRoom> pathMap;
	
	public ChatRoomContainer() {
		this.mapMutex = new Object();
		this.idMap = new HashMap<>();
		this.pathMap = new HashMap<>();
	}
	
	public boolean addRoom(ChatRoom room) {
		Assert.notNull(room, "ChatRoom cannot be null!");
		Assert.test(room.getId() > 0, "ChatRoom ID must be > 0!");
		Assert.notNull(room.getPath(), "Path must not be null!");
		Assert.test(!room.getPath().isEmpty(), "Path must be non-empty!");
		Assert.test(room.getPath().startsWith("SWG."), "Path must start with \"SWG.\"!");
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
	
	public boolean destroyRoom(ChatRoom room) {
		Assert.notNull(room, "ChatRoom cannot be null!");
		synchronized (mapMutex) {
			boolean success = true;
			success = idMap.remove(room.getId()) != null && success;
			success = pathMap.remove(room.getPath()) != null && success;
			return success;
		}
	}
	
	public List<ChatRoom> getAllRooms() {
		synchronized (mapMutex) {
			return new ArrayList<>(idMap.values());
		}
	}
	
	public ChatRoom getRoomById(int roomId) {
		Assert.test(roomId > 0, "Room ID must be > 0!");
		synchronized (mapMutex) {
			return idMap.get(roomId);
		}
	}
	
	public ChatRoom getRoomByPath(String path) {
		Assert.notNull(path, "Path must not be null!");
		Assert.test(!path.isEmpty(), "Path must be non-empty!");
		Assert.test(path.startsWith("SWG."), "Path must start with \"SWG.\"!");
		synchronized (mapMutex) {
			return pathMap.get(path);
		}
	}
	
	public boolean hasRoomWithId(int roomId) {
		Assert.test(roomId > 0, "Room ID must be > 0!");
		synchronized (mapMutex) {
			return idMap.containsKey(roomId);
		}
	}
	
	public boolean hasRoomWithPath(String path) {
		Assert.notNull(path, "Path must not be null!");
		Assert.test(!path.isEmpty(), "Path must be non-empty!");
		Assert.test(path.startsWith("SWG."), "Path must start with \"SWG.\"!");
		synchronized (mapMutex) {
			return pathMap.containsKey(path);
		}
	}
	
}
