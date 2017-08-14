package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

import resources.schematic.DraftSchematic;
import resources.schematic.IngridientSlot;

public class DraftSlotsQueryResponse extends ObjectController {

	private final static int CRC = 0x01BF;
	
	private DraftSchematic schematic;
	private IngridientSlot ingridientSlot;
	
	public DraftSlotsQueryResponse(DraftSchematic schematic) {
		this.schematic = schematic;
		for (IngridientSlot ingridientSlot : schematic.getIngridientSlot()) {
			this.ingridientSlot = ingridientSlot;
		}
	}

	public DraftSlotsQueryResponse(NetBuffer data){
		super(CRC);
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		schematic.setCombinedCrc(data.getLong());
		ingridientSlot = data.getEncodable(IngridientSlot.class);
		schematic.setComplexity(data.getInt());
		schematic.setVolume(data.getInt());
		schematic.setCanManufacture(data.getBoolean());
	}

	@Override
	public NetBuffer encode() {
		int length = 0;
		for (IngridientSlot ingridientslot : schematic.getIngridientSlot()) {
			length += ingridientslot.getLength();
		}
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 17 + length);
		encodeHeader(data);
		data.addLong(schematic.getCombinedCrc());
		data.addEncodable(ingridientSlot);
		data.addInt(schematic.getComplexity());
		data.addInt(schematic.getVolume());
		data.addBoolean(schematic.isCanManufacture());
		return data;
	}
	
	public DraftSchematic getSchematic() {
		return schematic;
	}
}