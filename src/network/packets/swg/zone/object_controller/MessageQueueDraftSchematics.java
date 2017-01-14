package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueDraftSchematics extends ObjectController {

	private final static int CRC = 0x0102;
	
	private long toolId;
	private long craftingStationId;
	private int schematicsCounter;
	private int schematicsId;
	private int schematicsCrc;
	private byte subcategory1;
	private byte subcategory2;
	private byte subcategory3;
	private byte subcategory4;
		
	public MessageQueueDraftSchematics(long toolId, long craftingStationId, int schematicsCounter, int schematicsId, int schematicsCrc, byte subcategory1, byte subcategory2, byte subcategory3, byte subcategory4) {
		super(CRC);
		this.toolId = toolId;
		this.craftingStationId = craftingStationId;
		this.schematicsCounter = schematicsCounter;
		this.schematicsId = schematicsId;
		this.schematicsCrc = schematicsCrc;
		this.subcategory1 = subcategory1;
		this.subcategory2 = subcategory2;
		this.subcategory3 = subcategory3;
		this.subcategory4 = subcategory4;
	}

	public MessageQueueDraftSchematics(ByteBuffer data) {
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		toolId = getLong(data);
		craftingStationId = getLong(data);
		schematicsCounter = getInt(data);
		for(int i = 0; i < schematicsCounter; i++){
			schematicsId = getInt(data);
			schematicsCrc = getInt(data);
			subcategory1 = getByte(data);
			subcategory2 = getByte(data);
			subcategory3 = getByte(data);
			subcategory4 = getByte(data);
		}
		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 32);
		encodeHeader(data);
		addLong(data, toolId);
		addLong(data, craftingStationId);
		addInt(data, schematicsCounter);
		for(int i = 0; i< schematicsCounter; i++){
			addInt(data, schematicsId);
			addInt(data, schematicsCrc);
			addByte(data, subcategory1);
			addByte(data, subcategory2);
			addByte(data, subcategory3);
			addByte(data, subcategory4);
		}
		return data;
	}

	public long getToolId() {
		return toolId;
	}

	public void setToolId(long toolId) {
		this.toolId = toolId;
	}

	public long getCraftingStationId() {
		return craftingStationId;
	}

	public void setCraftingStationId(long craftingStationId) {
		this.craftingStationId = craftingStationId;
	}

	public int getSchematicsCounter() {
		return schematicsCounter;
	}

	public void setSchematicsCounter(int schematicsCounter) {
		this.schematicsCounter = schematicsCounter;
	}

	public int getSchematicsId() {
		return schematicsId;
	}

	public void setSchematicsId(int schematicsId) {
		this.schematicsId = schematicsId;
	}

	public int getSchematicsCrc() {
		return schematicsCrc;
	}

	public void setSchematicsCrc(int schematicsCrc) {
		this.schematicsCrc = schematicsCrc;
	}

	public byte getSubcategory1() {
		return subcategory1;
	}

	public void setSubcategory1(byte subcategory1) {
		this.subcategory1 = subcategory1;
	}

	public byte getSubcategory2() {
		return subcategory2;
	}

	public void setSubcategory2(byte subcategory2) {
		this.subcategory2 = subcategory2;
	}

	public byte getSubcategory3() {
		return subcategory3;
	}

	public void setSubcategory3(byte subcategory3) {
		this.subcategory3 = subcategory3;
	}

	public byte getSubcategory4() {
		return subcategory4;
	}

	public void setSubcategory4(byte subcategory4) {
		this.subcategory4 = subcategory4;
	}
}