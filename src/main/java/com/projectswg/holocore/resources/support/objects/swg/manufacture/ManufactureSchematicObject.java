/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.objects.swg.manufacture;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;

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
		attributes = SWGMap.getSwgMap(buffer, 3, 5, StringId.class, Float.class);
		itemsPerContainer = buffer.getInt();
		manufactureTime = buffer.getFloat();
	}
	
	@Override
	public void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		appearanceData = buffer.getArray();
		customAppearance = buffer.getArray();
		draftSchematicTemplate = CRC.getString(buffer.getInt());
		crafting = buffer.getBoolean();
		schematicChangedSignal = buffer.getByte();
	}
	
}
