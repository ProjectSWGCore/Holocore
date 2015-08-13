package resources.radial;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import network.packets.Packet;
import resources.encodables.Encodable;
import resources.server_info.Log;

public class RadialOptionList implements Encodable {
	
	private final List<RadialOption> options;
	
	public RadialOptionList() {
		options = new ArrayList<>();
	}
	
	public RadialOptionList(List<RadialOption> options) {
		this();
		this.options.addAll(options);
	}
	
	public void addOption(RadialOption option) {
		options.add(option);
	}
	
	public void setOptions(List<RadialOption> options) {
		this.options.clear();
		this.options.addAll(options);
	}
	
	public List<RadialOption> getOptions() {
		return Collections.unmodifiableList(options);
	}
	
	public void decode(ByteBuffer data) {
		int optionsCount = Packet.getInt(data);
		Map<Integer, RadialOption> optionMap = new HashMap<>();
		for (int i = 0; i < optionsCount; i++) {
			RadialOption option = new RadialOption();
			int opt = Packet.getByte(data); // option number
			int parent = Packet.getByte(data); // parentId
			int radialType = Packet.getShort(data); // radialType
			Packet.getByte(data); // optionType
			Packet.getUnicode(data); // text
			RadialItem item = RadialItem.getFromId(radialType);
			if (item == null) {
				Log.e("ObjectMenuRequest", "No radial item found for: %04X");
				continue;
			}
			option.setItem(item);
			optionMap.put(opt, option);
			if (parent == 0) {
				options.add(option);
			} else {
				RadialOption parentOpt = optionMap.get(parent);
				if (parentOpt == null) {
					Log.e("ObjectMenuRequest", "Parent not found! Parent=%d  Option=%s", parent, option);
				} else {
					parentOpt.addChild(option);
				}
			}
		}
	}
	
	public byte [] encode() {
		ByteBuffer data = ByteBuffer.allocate(4 + getOptionSize());
		Packet.addInt(data, getOptionCount());
		addOptions(data);
		return data.array();
	}
	
	public int getSize() {
		return 4 + getOptionSize();
	}
	
	private int getOptionCount() {
		int count = 0;
		for (RadialOption option : options) {
			count += getOptionCount(option);
		}
		return count;
	}
	
	private int getOptionSize() {
		int size = 0;
		for (RadialOption option : options) {
			size += getOptionSize(option);
		}
		return size;
	}
	
	private void addOptions(ByteBuffer data) {
		int index = 1;
		for (RadialOption option : options) {
			index = addOption(data, option, 0, index);
		}
	}
	
	private int getOptionCount(RadialOption parent) {
		int count = 1;
		for (RadialOption child : parent.getChildren()) {
			count += getOptionCount(child);
		}
		return count;
	}
	
	private int getOptionSize(RadialOption parent) {
		int size = 9;
		if (parent.getText() != null && !parent.getText().isEmpty())
			size += parent.getText().length()*2;
		for (RadialOption child : parent.getChildren()) {
			size += getOptionSize(child);
		}
		return size;
	}
	
	private int addOption(ByteBuffer data, RadialOption parent, int parentIndex, int index) {
		int myIndex = index++;
		Packet.addByte(data, myIndex);
		Packet.addByte(data, parentIndex);
		Packet.addShort(data, parent.getId());
		Packet.addByte(data, parent.getOptionType());
		if (parent.getText() != null || !parent.getText().isEmpty())
			Packet.addUnicode(data, parent.getText());
		else
			data.putInt(0);
		for (RadialOption option : parent.getChildren()) {
			index = addOption(data, option, myIndex, index);
		}
		return index;
	}
	
}
