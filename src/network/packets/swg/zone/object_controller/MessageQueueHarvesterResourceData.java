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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

public class MessageQueueHarvesterResourceData extends ObjectController {

	public static final int CRC = 0x00EA;
	
	private long harvesterId;
	private int resourceListSize;
	private long[] resourceId;
	private String[] resourceName;
	private String[] resourceType;
	private byte[] resourceDensity;
		
	public MessageQueueHarvesterResourceData(long harvesterId, int resourceListSize, long[] resourceId, String[] resourceName, String[] resourceType, byte[] resourceDensity) {
		super(CRC);
		this.harvesterId = harvesterId;
		this.resourceListSize = resourceListSize;
		this.resourceId = resourceId;
		this.resourceName = resourceName;
		this.resourceType = resourceType;
		this.resourceDensity = resourceDensity;
	}
	
	public MessageQueueHarvesterResourceData(ByteBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(ByteBuffer data) {
		decodeHeader(data);
		harvesterId = getLong(data);
		resourceListSize = getInt(data);
		for(int i = 0; i < resourceListSize; i++){
			resourceId[i] = getLong(data);
			resourceName[i] = getUnicode(data);
			resourceType[i] = getUnicode(data);
			resourceDensity[i] = getByte(data);
		}
		
	}

	@Override
	public ByteBuffer encode() {
		int len = HEADER_LENGTH + 12;
		for (int i = 0; i < resourceListSize; i++)
		    len += 17 + resourceName[i].length() * 2 + resourceType[i].length() * 2;
		ByteBuffer data = ByteBuffer.allocate(len);
		encodeHeader(data);
		addLong(data, harvesterId);
		addInt(data, resourceListSize);
		for(int i = 0; i < resourceListSize; i++){
			addLong(data, resourceId[i]);
			addUnicode(data, resourceName[i]);
			addUnicode(data, resourceType[i]);
			addByte(data, resourceDensity[i]);
		}
		
		return data;
	}

	public long getHarvesterId() {
		return harvesterId;
	}

	public void setHarvesterId(long harvesterId) {
		this.harvesterId = harvesterId;
	}

	public int getResourceListSize() {
		return resourceListSize;
	}

	public void setResourceListSize(int resourceListSize) {
		this.resourceListSize = resourceListSize;
	}

	public long[] getResourceId() {
		return resourceId;
	}

	public void setResourceId(long[] resourceId) {
		this.resourceId = resourceId;
	}

	public String[] getResourceName() {
		return resourceName;
	}

	public void setResourceName(String[] resourceName) {
		this.resourceName = resourceName;
	}

	public String[] getResourceType() {
		return resourceType;
	}

	public void setResourceTypo(String[] resourceType) {
		this.resourceType = resourceType;
	}

	public byte[] getResourceDensity() {
		return resourceDensity;
	}

	public void setResourceDensity(byte[] resourceDensity) {
		this.resourceDensity = resourceDensity;
	}
}