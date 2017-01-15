package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftSelectSchematic extends ObjectController {

	public static final int CRC = 0x010E;
	
	private int schematicId;
	
	public MessageQueueCraftSelectSchematic(int schematicId) {
		super(CRC);
		this.schematicId = schematicId;
	}
	
	public MessageQueueCraftSelectSchematic(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		schematicId = getInt(data);		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 4 );
		encodeHeader(data);
		addInt(data, schematicId);
		return data;
	}
}