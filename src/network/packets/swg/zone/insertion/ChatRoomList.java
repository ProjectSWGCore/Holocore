/***********************************************************************************
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
package network.packets.swg.zone.insertion;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.insertion.ChatRoomList.ChatRoom.User;

public class ChatRoomList extends SWGPacket {
	
	public static final int CRC = 0x70DEB197;
	private List <ChatRoom> chatrooms = new ArrayList<ChatRoom>();
	
	public ChatRoomList() {
		
	}
	
	public ChatRoomList(ChatRoom room) {
		chatrooms.add(room);
	}
	
	public ChatRoomList(ChatRoom [] rooms) {
		for (ChatRoom r : rooms) {
			chatrooms.add(r);
		}
	}
	
	public void addChatRoom(ChatRoom r) {
		chatrooms.add(r);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		int chatroomCount = getInt(data);
		for (int i = 0; i < chatroomCount; i++) {
			ChatRoom room = new ChatRoom();
			room.setRoomId(getInt(data));
			room.setPrivateFlag(getInt(data));
			room.setModeratedFlag(getByte(data));
			room.setRoomPathName(getAscii(data));
			room.setGame(getAscii(data));
			room.setServer(getAscii(data));
			room.setOwner(getAscii(data));
			getAscii(data); // SWG
			getAscii(data); // galaxy
			room.setCreator(getAscii(data));
			room.setTitle(getUnicode(data));
			int moderatorCount = getInt(data);
			for (int m = 0; m < moderatorCount; m++) {
				User user = new User();
				user.setGame(getAscii(data));
				user.setServer(getAscii(data));
				user.setName(getAscii(data));
				room.addModerator(user);
			}
			int userCount = getInt(data);
			for (int u = 0; u < userCount; u++) {
				User user = new User();
				user.setGame(getAscii(data));
				user.setServer(getAscii(data));
				user.setName(getAscii(data));
				room.addUser(user);
			}
		}
	}
	
	public ByteBuffer encode() {
		int length = 10;
		for (ChatRoom r : chatrooms) {
			length += r.getSize();
		}
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(data, 2);
		addInt(  data, CRC);
		addInt(  data, chatrooms.size());
		for (ChatRoom r : chatrooms) {
			addInt(data, r.getRoomId());
			addInt(data, r.getPrivateFlag());
			addByte(data, r.getModeratedFlag());
			addAscii(data, r.getRoomPathName());
			addAscii(data, r.getGame());
			addAscii(data, r.getServer());
			addAscii(data, r.getOwner());
			addAscii(data, r.getCreator());
			addUnicode(data, r.getTitle());
			addInt(data, r.getModerators().size());
			for (User u : r.getModerators()) {
				addAscii(data, u.game);
				addAscii(data, u.server);
				addAscii(data, u.name);
			}
			addInt(data, r.getUsers().size());
			for (User u : r.getUsers()) {
				addAscii(data, u.game);
				addAscii(data, u.server);
				addAscii(data, u.name);
			}
		}
		return data;
	}
	
	public static class ChatRoom {
		private int roomId = 0;
		private int privateFlag = 0;
		private int moderatedFlag = 0;
		private String roomPathName = "";
		private String game = "";
		private String server = "";
		private String owner = "";
		private String creator = "";
		private String title = "";
		private Vector <User> moderators = new Vector<User>();
		private Vector <User> users = new Vector<User>();
		
		public ChatRoom() {
			
		}
		
		public ChatRoom(int roomId, int privateFlag, int moderatedFlag, String roomPathName, String game, String server, String owner, String creator, String title) {
			this.roomId = roomId;
			this.privateFlag = privateFlag;
			this.moderatedFlag = moderatedFlag;
			this.roomPathName = roomPathName;
			this.game = game;
			this.server = server;
			this.owner = owner;
			this.creator = creator;
			this.title = title;
		}
		
		public void addModerator(String game, String server, String name) {
			moderators.add(new User(game, server, name));
		}
		
		public void addUser(String game, String server, String name) {
			users.add(new User(game, server, name));
		}
		
		public int getSize() {
			int length = 31;
			length += roomPathName.length();
			length += game.length();
			length += server.length();
			length += owner.length();
			length += creator.length();
			length += title.length() * 2;
			for (User u : moderators) length += u.getSize();
			for (User u : users) length += u.getSize();
			return length;
		}
		
		public int getRoomId() { return roomId; }
		public int getPrivateFlag() { return privateFlag; }
		public int getModeratedFlag() { return moderatedFlag; }
		public String getRoomPathName() { return roomPathName; }
		public String getGame() { return game; }
		public String getServer() { return server; }
		public String getOwner() { return owner; }
		public String getCreator() { return creator; }
		public String getTitle() { return title; }
		public List <User> getUsers() { return users; }
		public List <User> getModerators() { return moderators; }
		
		public void setRoomId(int id) { this.roomId = id; }
		public void setPrivateFlag(int flag) { this.privateFlag = flag; }
		public void setModeratedFlag(int flag) { this.moderatedFlag = flag; }
		public void setRoomPathName(String path) { this.roomPathName = path; }
		public void setGame(String game) { this.game = game; }
		public void setServer(String server) { this.server = server; }
		public void setOwner(String owner) { this.owner = owner; }
		public void setCreator(String creator) { this.creator = creator; }
		public void setTitle(String title) { this.title = title; }
		public void setUsers(Vector<User> users) { this.users = users; }
		public void setModerators(Vector<User> moderators) { this.moderators = moderators; }
		public void addUser(User user) { this.users.add(user); }
		public void addModerator(User moderator) { this.moderators.add(moderator); }
		
		public static class User {
			public String game = "";
			public String server = "";
			public String name = "";
			
			public User() {
				
			}
			
			public User(String game, String server, String name) {
				this.game = game;
				this.server = server;
				this.name = name;
			}
			
			public int getSize() {
				return 6 + game.length() + server.length() + name.length();
			}
			
			public String getGame() { return game; }
			public String getServer() { return server; }
			public String getName() { return name; }
			
			public void setGame(String game) { this.game = game; }
			public void setServer(String server) { this.server = server; }
			public void setName(String name) { this.name = name; }
		}
	}
}
