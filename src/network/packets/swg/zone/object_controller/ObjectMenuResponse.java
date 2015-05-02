package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import resources.RadialOption;

public class ObjectMenuResponse extends ObjectController {
	
	public static final int CRC = 0x0147;
	
	private long targetId;
	private long requesterId;
	private List <RadialOption> options;
	private int counter;
	
	public ObjectMenuResponse(long objectId) {
		super(objectId, CRC);
	}
	
	public ObjectMenuResponse(long objectId, long targetId, long requesterId, List<RadialOption> options, int counter) {
		super(objectId, CRC);
		this.targetId = targetId;
		this.requesterId = requesterId;
		this.options = options;
		this.counter = counter;
	}
	
	public ObjectMenuResponse(ByteBuffer data) {
		super(CRC);
		options = new ArrayList<RadialOption>();
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		targetId = getLong(data);
		requesterId = getLong(data);
		int optionCount = getInt(data);
		for (int i = 0; i < optionCount; i++) {
			RadialOption option = new RadialOption();
			getByte(data); // option number
			option.setParentId(getByte(data));
			option.setId(getShort(data));
			option.setOptionType(getByte(data));
			option.setText(getUnicode(data));
		}
		counter = getByte(data);
	}
	
	public ByteBuffer encode() {
		int optionsDataSize = 0;
		for (RadialOption o : options) {
			optionsDataSize += 9;
			if (o.getText() != null || !o.getText().isEmpty())
				optionsDataSize += o.getText().length()*2;
		}
		ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH + optionsDataSize + 21);
		encodeHeader(data);
		
		addLong(data, targetId);
		addLong(data, requesterId);

		addInt(data, options.size());
		for (int i = 0; i < options.size(); i++) {
			RadialOption option = options.get(i);
			addByte(data, i + 1);
			addByte(data, option.getParentId());
			addShort(data, option.getId());
			addByte(data, option.getOptionType());
			if (option.getText() != null || !option.getText().isEmpty())
				addUnicode(data, option.getText());
			else data.putInt(0);
		}
		addByte(data, counter);
		return data;
	}
}
