package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import resources.RadialOption;

public class ObjectMenuRequest extends ObjectController {
	
	public static final int CRC = 0x0146;
	
	private long targetId;
	private long requesterId;
	private List <RadialOption> options;
	private byte counter;
	
	public ObjectMenuRequest(long objectId) {
		super(objectId, CRC);
		options = new ArrayList<RadialOption>();
	}
	
	public ObjectMenuRequest(ByteBuffer data) {
		super(CRC);
		options = new ArrayList<RadialOption>();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		
		targetId = getLong(data);
		requesterId = getLong(data);
		
		int optionsCount = getInt(data);
		for (int i = 0; i < optionsCount; i++) {
			RadialOption option = new RadialOption();
			getByte(data); // option number
			option.setParentId(getByte(data));
			option.setId(getShort(data));
			option.setOptionType(getByte(data));
			option.setText(getUnicode(data));
			options.add(option);
		}
		counter = getByte(data);
	}
	
	public ByteBuffer encode() {
		int optSize = 0;
		for (RadialOption option : options)
			optSize += 7 + option.getText().length()*2;
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + 21 + optSize);
		encodeHeader(data);
		addLong(data, targetId);
		addLong(data, requesterId);
		addInt(data, options.size());
		int optNum = 0;
		for (RadialOption option : options) {
			addByte(data, ++optNum); // option number
			addByte(data, option.getParentId());
			addShort(data, option.getId());
			addByte(data, option.getOptionType());
			addUnicode(data, option.getText());
		}
		addByte(data, counter);
		return data;
	}
	
	public long getTargetId() { return targetId; }
	public long getRequesterId() { return requesterId; }
	public int getCounter() { return counter; }
	public List <RadialOption> getOptions() { return options; }
	
	public void setTargetId(long targetId) { this.targetId = targetId; }
	public void setRequesterId(long requesterId) { this.requesterId = requesterId; }
	public void setCounter(byte counter) { this.counter = counter; }
	public void addOption(RadialOption opt) { options.add(opt); }
	
}
