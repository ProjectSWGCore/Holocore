/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.objects.manufacture;

import java.util.Map.Entry;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.collections.SWGMap;
import resources.common.CRC;
import resources.encodables.StringId;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;
import resources.server_info.CrcDatabase;

public class ManufactureSchematicObject extends IntangibleObject {
	
	private SWGMap<StringId, Float> attributes	= new SWGMap<>(3, 5);
	private int itemsPerContainer				= 0;
	private float manufactureTime				= 0;
	private byte [] appearanceData				= new byte[0];
	private byte [] customAppearance			= new byte[0];
	private String draftSchematicTemplate		= "";
	private boolean crafting					= false;
	private byte schematicChangedSignal			= 0;
	
	public ManufactureSchematicObject(long objectId) {
		super(objectId, BaselineType.MSCO);
	}
	
	public float getManufactureAttribute(String name) {
		Float attr;
		synchronized (attributes) {
			attr = attributes.get(name);
		}
		if (attr == null)
			return Float.NaN;
		return attr;
	}
	
	public void setItemsPerContainer(int items) {
		this.itemsPerContainer = items;
	}
	
	public int getItemsPerContainer() {
		return itemsPerContainer;
	}
	
	public void setManufactureTime(float time) {
		this.manufactureTime = time;
	}
	
	public float getManufactureTime() {
		return manufactureTime;
	}
	
	public byte [] getAppearanceData() {
		return appearanceData;
	}
	
	public void setAppearanceData(byte[] appearanceData) {
		this.appearanceData = appearanceData;
	}
	
	public byte [] getCustomAppearance() {
		return customAppearance;
	}
	
	public void setCustomAppearance(byte[] customAppearance) {
		this.customAppearance = customAppearance;
	}
	
	public String getDraftSchematicTemplate() {
		return draftSchematicTemplate;
	}
	
	public void setDraftSchematicTemplate(String draftSchematicTemplate) {
		this.draftSchematicTemplate = draftSchematicTemplate;
	}
	
	public boolean isCrafting() {
		return crafting;
	}
	
	public void setCrafting(boolean crafting) {
		this.crafting = crafting;
	}
	
	public byte getSchematicChangedSignal() {
		return schematicChangedSignal;
	}
	
	public void setSchematicChangedSignal(byte schematicChangedSignal) {
		this.schematicChangedSignal = schematicChangedSignal;
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addObject(attributes);
		bb.addInt(itemsPerContainer);
		bb.addFloat(manufactureTime);
	}
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addArray(appearanceData);
		bb.addArray(customAppearance);
		bb.addInt(CRC.getCrc(draftSchematicTemplate));
		bb.addBoolean(crafting);
		bb.addByte(schematicChangedSignal);
	}
	
	@Override
	public void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		attributes = buffer.getSwgMap(3, 5, StringId.class, Float.class);
		itemsPerContainer = buffer.getInt();
		manufactureTime = buffer.getFloat();
	}
	
	@Override
	public void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		appearanceData = buffer.getArray();
		customAppearance = buffer.getArray();
		try (CrcDatabase db = new CrcDatabase()) {
			draftSchematicTemplate = db.getString(buffer.getInt());
		}
		crafting = buffer.getBoolean();
		schematicChangedSignal = buffer.getByte();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		stream.addInt(itemsPerContainer);
		stream.addFloat(manufactureTime);
		stream.addArray(appearanceData);
		stream.addArray(customAppearance);
		stream.addAscii(draftSchematicTemplate);
		stream.addBoolean(crafting);
		stream.addByte(schematicChangedSignal);
		synchronized (attributes) {
			stream.addInt(attributes.size());
			for (Entry<StringId, Float> e : attributes.entrySet()) {
				e.getKey().save(stream);
				stream.addFloat(e.getValue());
			}
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		itemsPerContainer = stream.getInt();
		manufactureTime = stream.getFloat();
		appearanceData = stream.getArray();
		customAppearance = stream.getArray();
		draftSchematicTemplate = stream.getAscii();
		crafting = stream.getBoolean();
		schematicChangedSignal = stream.getByte();
		for (int i = 0; i < stream.getInt(); i++) {
			StringId id = new StringId();
			id.read(stream);
			attributes.put(id, stream.getFloat());
		}
	}
	
}
