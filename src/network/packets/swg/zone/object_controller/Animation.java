package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class Animation extends ObjectController {

	public static final int CRC = 0x00F2;

	private String animation;
	
	public Animation(long objectId, String animation) {
		super(objectId, CRC);
		this.animation = animation;
	}
	
	public Animation(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		animation = getAscii(data);
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 2 + animation.length());
		encodeHeader(data);
		addAscii(data, animation);
		return data;
	}
}

