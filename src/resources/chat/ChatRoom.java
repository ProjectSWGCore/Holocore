/*******************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com
 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.
 * Our goal is to create an emulator which will provide a server for players to
 * continue playing a game similar to the one they used to play. We are basing
 * it on the final publish of the game prior to end-game events.
 *
 * This file is part of Holocore.
 *
 * --------------------------------------------------------------------------------
 *
 * Holocore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Holocore is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package resources.chat;

import network.packets.Packet;
import network.packets.swg.SWGPacket;
import network.packets.swg.zone.chat.ChatRoomMessage;
import resources.encodables.Encodable;
import resources.encodables.OutOfBandPackage;
import resources.network.NetBufferStream;
import resources.persistable.Persistable;
import services.player.PlayerManager;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class ChatRoom implements Encodable, Persistable {
	
	private int id;
	private int type;
	private String path;
	private ChatAvatar owner;
	private ChatAvatar creator;
	private String title;
	private List<ChatAvatar> moderators;
	private List<ChatAvatar> invited;
	private boolean moderated; // No one but moderators can talk
	private List<ChatAvatar> banned;
	// Members are only actually apart of a room when they're "in the room", so we don't need to save this info
	// as each player will automatically re-join the room based on their joined channels list
	private transient List<ChatAvatar> members;
	
	// Large amount of data that's sent frequently, best if we can pre-encode everything and save it for later
	private transient boolean modified = true;
	private transient byte[] data;

	public ChatRoom() {
		owner = new ChatAvatar();
		creator = new ChatAvatar();
		moderators = new ArrayList<>();
		invited = new ArrayList<>();
		members = new ArrayList<>();
		banned = new ArrayList<>();
	}
	
	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		members = new ArrayList<>();
		ois.defaultReadObject();
	}
	
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public ChatAvatar getOwner() {
		return owner;
	}

	public void setOwner(ChatAvatar owner) {
		this.owner = owner;
	}

	public ChatAvatar getCreator() {
		return creator;
	}

	public void setCreator(ChatAvatar creator) {
		this.creator = creator;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<ChatAvatar> getModerators() {
		return moderators;
	}

	public List<ChatAvatar> getInvited() {
		return invited;
	}

	public boolean isModerated() {
		return moderated;
	}

	public void setModerated(boolean moderated) {
		this.moderated = moderated;
	}

	public List<ChatAvatar> getMembers() {
		return members;
	}

	public boolean isPublic() {
		return type == 0;
	}

	public void setIsPublic(boolean isPublic) {
		this.type = (isPublic ? 0 : 1);
	}

	public List<ChatAvatar> getBanned() {
		return banned;
	}

	public ChatResult canJoinRoom(ChatAvatar avatar, boolean ignoreInvitation) {
		if (banned.contains(avatar))
			return ChatResult.ROOM_AVATAR_BANNED;

		if (members.contains(avatar))
			return ChatResult.ROOM_ALREADY_JOINED;

		if (isPublic() || ignoreInvitation || invited.contains(avatar) || moderators.contains(avatar))
			return ChatResult.SUCCESS;

		return ChatResult.ROOM_AVATAR_NO_PERMISSION;
	}

	public ChatResult canSendMessage(ChatAvatar avatar) {
		if (banned.contains(avatar))
			return ChatResult.ROOM_AVATAR_BANNED;

		if (moderated && !moderators.contains(avatar))
			return ChatResult.CUSTOM_FAILURE;

		return ChatResult.SUCCESS;
	}

	public boolean isModerator(ChatAvatar avatar) {
		return avatar.equals(owner) || moderators.contains(avatar);
	}

	public boolean isMember(ChatAvatar avatar) {
		return members.contains(avatar);
	}

	public boolean isBanned(ChatAvatar avatar) {
		return banned.contains(avatar);
	}

	public boolean isInvited(ChatAvatar avatar) {
		return invited.contains(avatar);
	}

	public boolean addMember(ChatAvatar avatar) {
		return members.add(avatar);
	}

	public boolean removeMember(ChatAvatar avatar) {
		return members.remove(avatar);
	}

	public boolean addModerator(ChatAvatar avatar) {
		return moderators.add(avatar);
	}

	public boolean removeModerator(ChatAvatar avatar) {
		return moderators.remove(avatar);
	}

	public boolean addInvited(ChatAvatar avatar) {
		return invited.add(avatar);
	}

	public boolean removeInvited(ChatAvatar avatar) {
		return invited.remove(avatar);
	}

	public boolean addBanned(ChatAvatar avatar) {
		return banned.add(avatar);
	}

	public boolean removeBanned(ChatAvatar avatar) {
		return banned.remove(avatar);
	}

	public void sendMessage(ChatAvatar sender, String message, OutOfBandPackage oob, PlayerManager playerManager) {
		ChatRoomMessage chatRoomMessage = new ChatRoomMessage(sender, getId(), message, oob);
		for (ChatAvatar member : members) {
			if (member.getPlayer().getPlayerObject().isIgnored(sender.getName()))
				continue;
			
			member.getPlayer().sendPacket(chatRoomMessage);
		}
	}

	public void sendPacketToMembers(PlayerManager manager, SWGPacket... packets) {
		for (ChatAvatar member : members) {
			member.getPlayer().sendPacket(packets);
		}
	}

	@Override
	public void decode(ByteBuffer data) {
		id			= Packet.getInt(data);
		type		= Packet.getInt(data);
		moderated 	= Packet.getBoolean(data);
		path		= Packet.getAscii(data);
		owner		= Packet.getEncodable(data, ChatAvatar.class);
		creator		= Packet.getEncodable(data, ChatAvatar.class);
		title		= Packet.getUnicode(data);
		moderators	= Packet.getList(data, ChatAvatar.class);
		invited		= Packet.getList(data, ChatAvatar.class);
	}

	@Override
	public byte[] encode() {
		if (!modified && data != null)
			return data;

		int avatarIdSize = 0; // The encode method for ChatAvatar saves the encode result if the class was modified/null data
		for (ChatAvatar moderator : moderators) {
			avatarIdSize += moderator.encode().length;
		}

		for (ChatAvatar invitee : invited) {
			avatarIdSize += invitee.encode().length;
		}
		avatarIdSize += owner.encode().length + creator.encode().length;

		ByteBuffer bb = ByteBuffer.allocate(23 + path.length() + (title.length() * 2) + avatarIdSize);
		Packet.addInt(bb, id);
		Packet.addInt(bb, type);
		Packet.addBoolean(bb, moderated);
		Packet.addAscii(bb, path);
		Packet.addEncodable(bb, owner);
		Packet.addEncodable(bb, creator);
		Packet.addUnicode(bb, title);
		Packet.addList(bb, moderators);
		Packet.addList(bb, invited);

		data = bb.array();
		modified = false;

		return data;
	}
	/*
	private int id;
	private int type;
	private String path;
	private ChatAvatar owner;
	private ChatAvatar creator;
	private String title;
	private List<ChatAvatar> moderators;
	private List<ChatAvatar> invited;
	private boolean moderated; // No one but moderators can talk
	private List<ChatAvatar> banned;
	 */
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		owner.save(stream);
		creator.save(stream);
		stream.addInt(id);
		stream.addInt(type);
		stream.addAscii(path);
		stream.addUnicode(title);
		stream.addBoolean(moderated);
		stream.addList(moderators, (a) -> a.save(stream));
		stream.addList(invited, (a) -> a.save(stream));
		stream.addList(banned, (a) -> a.save(stream));
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		owner.read(stream);
		creator.read(stream);
		id = stream.getInt();
		type = stream.getInt();
		path = stream.getAscii();
		title = stream.getUnicode();
		moderated = stream.getBoolean();
		stream.getList((i) -> moderators.add(inflateAvatar(stream)));
		stream.getList((i) -> invited.add(inflateAvatar(stream)));
		stream.getList((i) -> banned.add(inflateAvatar(stream)));
	}
	
	private ChatAvatar inflateAvatar(NetBufferStream stream) {
		ChatAvatar avatar = new ChatAvatar();
		avatar.read(stream);
		return avatar;
	}

	@Override
	public String toString() {
		return "ChatRoom[id=" + id + ", type=" + type + ", path='" + path + "', title='" + title + '\'' +
				", creator=" + creator + ", moderated=" + moderated + ", isPublic=" + isPublic() + "]";
	}
	
	public static ChatRoom create(NetBufferStream stream) {
		ChatRoom room = new ChatRoom();
		room.read(stream);
		return room;
	}
}
