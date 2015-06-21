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
import network.packets.swg.zone.chat.ChatRoomMessage;
import resources.encodables.OutOfBandPackage;
import resources.network.BaselineBuilder;
import resources.player.Player;
import services.player.PlayerManager;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Waverunner
 */
public class ChatRoom implements Serializable, BaselineBuilder.Encodable, BaselineBuilder.Decodable {
	private static final long serialVersionUID = 1L;

	private int id;
	private int type;
	private String path;
	private ChatAvatar owner;
	private ChatAvatar creator;
	private String title;
	private List<ChatAvatar> moderators;
	private List<ChatAvatar> invited;
	private boolean muted; // No one but moderators can talk
	private boolean isPublic;
	private List<ChatAvatar> banned;
	// Members are only actually apart of a room when they're "in the room", so we don't need to save this info
	// as each player will automatically re-join the room based on their joined channels list
	private transient List<ChatAvatar> members;

	// Large amount of data that's sent frequently, best if we can pre-encode everything and save it for later
	private transient boolean modified = true;
	private transient byte[] data;

	public ChatRoom() {
		moderators = new ArrayList<>();
		invited = new ArrayList<>();
		members = new ArrayList<>();
		banned = new ArrayList<>();
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

	public boolean isMuted() {
		return muted;
	}

	public void setMuted(boolean muted) {
		this.muted = muted;
	}

	public List<ChatAvatar> getMembers() {
		return members;
	}

	public boolean isPublic() {
		return isPublic;
	}

	public void setIsPublic(boolean isPublic) {
		this.isPublic = isPublic;
	}

	public List<ChatAvatar> getBanned() {
		return banned;
	}

	public ChatResult canJoinRoom(ChatAvatar avatar) {
		if (banned.contains(avatar))
			return ChatResult.ROOM_AVATAR_BANNED;

		if (members.contains(avatar))
			return ChatResult.ROOM_ALREADY_JOINED;

		if (isPublic || invited.contains(avatar))
			return ChatResult.SUCCESS;

		return ChatResult.ROOM_AVATAR_NOT_INVITED;
	}

	public ChatResult canSendMessage(ChatAvatar avatar) {
		if (banned.contains(avatar))
			return ChatResult.ROOM_AVATAR_BANNED;

		if (muted && !moderators.contains(avatar))
			return ChatResult.ROOM_NOT_MODERATOR;

		return ChatResult.SUCCESS;
	}

	public void sendMessage(ChatAvatar sender, String message, OutOfBandPackage oob, PlayerManager playerManager) {
		ChatRoomMessage chatRoomMessage = new ChatRoomMessage(sender, getId(), message, oob);

		for (ChatAvatar avatar : members) {
			Player player = playerManager.getPlayerFromNetworkId(avatar.getNetworkId());
			if (player == null)
				return;

			player.sendPacket(chatRoomMessage);
		}
	}

	@Override
	public void decode(ByteBuffer data) {}

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

		ByteBuffer bb = ByteBuffer.allocate(23 + path.length() + (title.length() * 2) + avatarIdSize).order(ByteOrder.LITTLE_ENDIAN);
		bb.putInt(id);
		bb.putInt(type);
		bb.put(muted ? (byte) 1 : (byte) 0);
		Packet.addAscii(bb, path);
		bb.put(owner.encode());
		bb.put(creator.encode());
		Packet.addUnicode(bb, title);
		bb.putInt(moderators.size());
		for (ChatAvatar moderator : moderators) {
			bb.put(moderator.encode());
		}
		bb.putInt(invited.size());
		for (ChatAvatar invitee : invited) {
			bb.put(invitee.encode());
		}

		data = bb.array();
		modified = false;

		return data;
	}

	@Override
	public String toString() {
		return "ChatRoom{" +
				"id=" + id +
				", path='" + path + '\'' +
				", title='" + title + '\'' +
				'}';
	}
}
