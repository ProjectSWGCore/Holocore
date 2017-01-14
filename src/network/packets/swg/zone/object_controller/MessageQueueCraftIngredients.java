package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftIngredients extends ObjectController {
	
	public static final int CRC = 0x0105;
	
	private int count;
	private String[] resourceName;
	private byte[] type;
	private int[] quantity;
	
	public MessageQueueCraftIngredients(int count, String[] resourceName, byte[] type, int[] quantity) {
		super();
		this.count = count;
		this.resourceName = resourceName;
		this.type = type;
		this.quantity = quantity;
	}
	
	public MessageQueueCraftIngredients(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		count = getInt(data);
		for(int i = 0; i <= count; i++){
			resourceName[i] = getUnicode(data);
			type[i] = getByte(data);
			quantity[i] = getInt(data);
		}		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 13 + resourceName.length + type.length + quantity.length);
		encodeHeader(data);
		addInt(data, count);
		for(int i = 0; i <= count; i++){
			addUnicode(data, resourceName[i] );
			addByte(data, type[i]);
			addInt(data, quantity[i]);
		}
		return data;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String[] getResourceName() {
		return resourceName;
	}

	public void setResourceName(String[] resourceName) {
		this.resourceName = resourceName;
	}

	public byte[] getType() {
		return type;
	}

	public void setType(byte[] type) {
		this.type = type;
	}

	public int[] getQuantity() {
		return quantity;
	}

	public void setQuantity(int[] quantity) {
		this.quantity = quantity;
	}	
}