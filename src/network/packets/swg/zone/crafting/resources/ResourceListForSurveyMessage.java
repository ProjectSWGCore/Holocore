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
package network.packets.swg.zone.crafting.resources;

import java.util.ArrayList;
import java.util.List;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class ResourceListForSurveyMessage extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("ResourceListForSurveyMessage");
	
	private final List<ResourceItem> resources;
	
	private String resourceType;
	private long creatureId;
	
	public ResourceListForSurveyMessage(long creatureId, String resourceType) {
		this.resources = new ArrayList<>();
		this.creatureId = creatureId;
		this.resourceType = resourceType;
	}
	
	public ResourceListForSurveyMessage(NetBuffer data) {
		this.resources = new ArrayList<>();
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		int resourceSize = data.getInt();
		for (int i = 0; i < resourceSize; i++) {
			resources.add(ResourceItem.decode(data));
		}
		resourceType = data.getAscii();
		creatureId = data.getLong();
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addShort(4);
		data.addInt(CRC);
		data.addInt(resources.size());
		for (ResourceItem item : resources) {
			ResourceItem.encode(data, item);
		}
		data.addAscii(resourceType);
		data.addLong(creatureId);
		return data;
	}
	
	public List<ResourceItem> getResources() {
		return resources;
	}
	
	public String getResourceType() {
		return resourceType;
	}
	
	public long getCreatureId() {
		return creatureId;
	}
	
	public void addResource(ResourceItem item) {
		resources.add(item);
	}
	
	public void removeResource(ResourceItem item) {
		resources.remove(item);
	}
	
	public void setResourceType(String resourceType) {
		this.resourceType = resourceType;
	}
	
	public void setCreatureId(long creatureId) {
		this.creatureId = creatureId;
	}

	private int getLength() {
		int length = 20 + resourceType.length();
		for (ResourceItem item : resources)
			length += item.getLength();
		return length;
	}
	
	public static class ResourceItem {
		
		private final String resourceName;
		private final String resourceClass;
		private final long resourceId;
		
		public ResourceItem(String resourceName, String resourceClass, long resourceId) {
			this.resourceName = resourceName;
			this.resourceClass = resourceClass;
			this.resourceId = resourceId;
		}
		
		public String getResourceName() {
			return resourceName;
		}
		
		public String getResourceClass() {
			return resourceClass;
		}
		
		public long getResourceId() {
			return resourceId;
		}
		
		public int getLength() {
			return 	12 + resourceName.length() + resourceClass.length();
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof ResourceItem))
				return false;
			ResourceItem item = (ResourceItem) o;
			return item.resourceName.equals(resourceName) && item.resourceClass.equals(resourceClass) && item.resourceId == resourceId;
		}
		
		@Override
		public int hashCode() {
			return Long.hashCode(resourceId) * 7 + resourceName.hashCode() * 3 + resourceClass.hashCode();
		}
		
		public static void encode(NetBuffer buffer, ResourceItem item) {
			buffer.addAscii(item.getResourceName());
			buffer.addLong(item.getResourceId());
			buffer.addAscii(item.getResourceClass());
		}
		
		public static ResourceItem decode(NetBuffer buffer) {
			String resourceName = buffer.getAscii();
			long resourceId = buffer.getLong();
			String resourceClass = buffer.getAscii();
			return new ResourceItem(resourceName, resourceClass, resourceId);
		}
		
	}
	
}
