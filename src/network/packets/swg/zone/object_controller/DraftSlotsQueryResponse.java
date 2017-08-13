package network.packets.swg.zone.object_controller;

import com.projectswg.common.network.NetBuffer;

import resources.schematic.DraftSchematic;
import resources.schematic.IngridientSlot;

public class DraftSlotsQueryResponse extends ObjectController {

	private final static int CRC = 0x01BF;
	
	private DraftSchematic schematic;
	private long combinedCrc;
	private int complexity;
	private int volume;
	private boolean canManufacture;
	private IngridientSlot ingridientSlot;
	
	public DraftSlotsQueryResponse(DraftSchematic schematic) {
		this.schematic = schematic;
		this.combinedCrc = schematic.getCombinedCrc();
		this.complexity = schematic.getComplexity();
		this.volume = schematic.getComplexity();
		this.canManufacture = schematic.isCanManufacture();
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
		combinedCrc = data.getLong();
		ingridientSlot = data.getEncodable(IngridientSlot.class);
		complexity = data.getInt();
		volume = data.getInt();
		canManufacture = data.getBoolean();
	}

	@Override
	public NetBuffer encode() {
		int length = 0;
		for (IngridientSlot ingridientslot : schematic.getIngridientSlot()) {
			length += ingridientslot.getLength();
		}
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 17 + length);
		encodeHeader(data);
		data.addLong(combinedCrc);
		data.addEncodable(ingridientSlot);
		data.addInt(complexity);
		data.addInt(volume);
		data.addBoolean(isCanManufacture());
		return data;
	}
	
	public DraftSchematic getSchematic() {
		return schematic;
	}

	public long getCombinedCrc() {
		return combinedCrc;
	}

	public int getComplexity() {
		return complexity;
	}

	public int getVolume() {
		return volume;
	}

	public boolean isCanManufacture() {
		return canManufacture;
	}
}