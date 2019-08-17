/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.resource;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;

public class ResourceContainerObjectShared implements MongoPersistable {
	
	private final ResourceContainerObject obj;
	
	/** RCNO3-13 */ private int	quantity		= 0;
	/** RCNO3-14 */ private long	resourceType	= 0;
	
	public ResourceContainerObjectShared(ResourceContainerObject obj) {
		this.obj = obj;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public void setQuantity(int quantity) {
		this.quantity = quantity;
		sendDelta(13, quantity);
	}
	
	public long getResourceType() {
		return resourceType;
	}
	
	public void setResourceType(long resourceType) {
		this.resourceType = resourceType;
		sendDelta(14, resourceType);
	}
	
	public void createBaseline3(BaselineBuilder bb) {
		bb.addInt(quantity);
		bb.addLong(resourceType);
		bb.incrementOperandCount(2);
	}
	
	public void parseBaseline3(NetBuffer data) {
		quantity = data.getInt();
		resourceType = data.getLong();
	}
	
	@Override
	public void readMongo(MongoData data) {
		quantity = data.getInteger("quantity", quantity);
		resourceType = data.getLong("type", resourceType);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("quantity", quantity);
		data.putLong("type", resourceType);
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(3, update, o);
	}
	
}
