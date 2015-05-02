package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public class ChatOnSendInstantMessage extends SWGPacket {
	
	public static final int CRC = 0x88DBB381;
	
	private int result;
	private int sequence;
	
	public ChatOnSendInstantMessage() {
		this(0, 0);
	}
	
	public ChatOnSendInstantMessage(int result, int sequence) {
		this.result = result;
		this.sequence = sequence;
	}
	
	public ChatOnSendInstantMessage(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		result = getInt(data);
		sequence = getInt(data);
	}
	
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(14);
		addShort(data, 2);
		addInt  (data, CRC);
		addInt  (data, result);
		addInt  (data, sequence);
		return data;
	}
	
	public void setResult(int result) {
		this.result = result;
	}
	
	public void setSequence(int sequence) {
		this.sequence = sequence;
	}
	
	public int getResult() {
		return result;
	}
	
	public int getSequence() {
		return sequence;
	}
	
}
