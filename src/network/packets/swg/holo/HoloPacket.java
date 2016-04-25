package network.packets.swg.holo;

import java.nio.ByteBuffer;

import network.packets.swg.SWGPacket;

public abstract class HoloPacket extends SWGPacket {
	
	public abstract void decode(ByteBuffer data);
	public abstract ByteBuffer encode();
	
}
