package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnDestroyRoom extends SWGPacket {
	
	public static final int CRC = 0xE8EC5877;
	
	private String galaxy;
	private String owner;
	private boolean success;
	private int roomId;
	private int requestId;
	
	public ChatOnDestroyRoom() {
		this("", "", true, 0, 0);
	}
	
	public ChatOnDestroyRoom(String galaxy, String owner, boolean success, int roomId, int requestId) {
		this.galaxy = galaxy;
		this.owner = owner;
		this.success = success;
		this.roomId = roomId;
		this.requestId = requestId;
	}
	
	public ChatOnDestroyRoom(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		getAscii(data); // SWG
		galaxy = getAscii(data);
		owner = getAscii(data);
		success = getInt(data) == 0;
		roomId = getInt(data);
		requestId = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(27 + galaxy.length() + owner.length());
		addShort(data, 5);
		addInt  (data, CRC);
		addAscii(data, "SWG");
		addAscii(data, galaxy);
		addAscii(data, owner);
		addInt  (data, success ? 0 : 1);
		addInt  (data, roomId);
		addInt  (data, requestId);
		return data;
	}

}

