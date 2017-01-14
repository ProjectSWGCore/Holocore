package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueCraftCustomization extends ObjectController {

	public static final int CRC = 0x015A;
	
	private String itemName;
	private byte appearenceTemplate;
	private int itemAmount;
	private byte count;
	int[] property;
	int[] value;
	
	
	
	public MessageQueueCraftCustomization(String itemName, byte appearenceTemplate, int itemAmount, byte count, int[] property, int[] value) {
		super(CRC);
		this.itemName = itemName;
		this.appearenceTemplate = appearenceTemplate;
		this.itemAmount = itemAmount;
		this.count = count;
		this.property = property;
		this.value = value;
	}
	
	public MessageQueueCraftCustomization(ByteBuffer data) {
		super(CRC);
		decode(data);
	} 

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		itemName = getUnicode(data);	
		appearenceTemplate = getByte(data);
		itemAmount = getInt(data);
		count = getByte(data);
		
		for(int i = 1; i <= count; i++){
			property[i] = getInt(data);
			value[i] = getInt(data);
		}		
	}

	@Override
	public ByteBuffer encode() {
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 18 + itemName.length() + property.length + value.length);
		encodeHeader(data);
		addUnicode(data, itemName);
		addByte(data, appearenceTemplate);
		addInt(data, itemAmount);
		addByte(data, count);
		
		for(int i = 1; i <= count; i++){
			addInt(data, property[i] );
			addInt(data, value[i]);
		}
		return data;
	}

	public String getItemName() {
		return itemName;
	}

	public void setItemName(String itemName) {
		this.itemName = itemName;
	}

	public byte getAppearenceTemplate() {
		return appearenceTemplate;
	}

	public void setAppearenceTemplate(byte appearenceTemplate) {
		this.appearenceTemplate = appearenceTemplate;
	}

	public int getItemAmount() {
		return itemAmount;
	}

	public void setItemAmount(int itemAmount) {
		this.itemAmount = itemAmount;
	}

	public byte getCount() {
		return count;
	}

	public void setCount(byte count) {
		this.count = count;
	}

	public int[] getProperty() {
		return property;
	}

	public void setProperty(int[] property) {
		this.property = property;
	}

	public int[] getValue() {
		return value;
	}

	public void setValue(int[] value) {
		this.value = value;
	}	
}