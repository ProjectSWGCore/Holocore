package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

import resources.schematic.DraftSchematic;
import resources.schematic.IngridientSlot;

public class DraftSlotsQueryResponse extends ObjectController {

	private final static int CRC = 0x01BF;
	
	private DraftSchematic schematic;
	
	public DraftSlotsQueryResponse(DraftSchematic schematic) {
		this.schematic = schematic;
	}

	public DraftSlotsQueryResponse(NetBuffer data){
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		schematic.setCombinedCrc(data.getLong());
		schematic.setComplexity(data.getInt());
		schematic.setVolume(data.getInt());
		schematic.setCanManufacture(data.getBoolean());
		schematic.getIngridientSlot().clear();
		schematic.getIngridientSlot().addAll(data.getList(IngridientSlot.class));
	}

	@Override
	public NetBuffer encode() {
		int length = 0;
		for (IngridientSlot ingridientSlot : schematic.getIngridientSlot()) {
			length += ingridientSlot.getLength();
		}
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 21 + length);
		encodeHeader(data);
		data.addLong(schematic.getCombinedCrc());
		data.addInt(schematic.getComplexity());
		data.addInt(schematic.getVolume());
		data.addBoolean(schematic.isCanManufacture());
		data.addList(schematic.getIngridientSlot());		
		return data;
	}
	
	public DraftSchematic getSchematic() {
		return schematic;
	}
}