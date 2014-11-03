package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.insertion.ChatRoomList.ChatRoom.User;

public class ChatRoomMessage extends SWGPacket {
	
	public static final int CRC = 0xCD4CE444;
	private User user = new User();
	private int roomId = 0;
	private String message = "";
	
	public ChatRoomMessage() {
		
	}
	
	public ChatRoomMessage(String game, String server, String name, int roomId, String message) {
		this.user.game = game;
		this.user.server = server;
		this.user.name = name;
		this.roomId = roomId;
		this.message = message;
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		user.game = getAscii(data);
		user.server = getAscii(data);
		user.name = getAscii(data);
		roomId = getInt(data);
		message = getUnicode(data);
		getUnicode(data);
	}
	
	public ByteBuffer encode() {
		int length = 24 + user.game.length() + user.server.length() + user.name.length() + message.length() * 2;
		ByteBuffer data = ByteBuffer.allocate(length);
		addShort(  data, 5);
		addInt(    data, CRC);
		addAscii(  data, user.game);
		addAscii(  data, user.server);
		addAscii(  data, user.name);
		addInt(    data, roomId);
		addUnicode(data, message);
		addUnicode(data, "");
		return data;
	}
}
