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
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;

public class ResourceContainerObjectSharedNP implements MongoPersistable {
	
	private final ResourceContainerObject obj;
	
	/** RCNO6-08 */ private int			maxQuantity		= 100_000;
	/** RCNO6-09 */ private String		parentName		= "";
	/** RCNO6-10 */ private String		resourceName	= "";
	/** RCNO6-11 */ private StringId resourceNameId = new StringId();
	
	public ResourceContainerObjectSharedNP(ResourceContainerObject obj) {
		this.obj = obj;
	}
	
	public int getMaxQuantity() {
		return maxQuantity;
	}
	
	public void setMaxQuantity(int maxQuantity) {
		this.maxQuantity = maxQuantity;
		sendDelta(8, maxQuantity);
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public void setParentName(String parentName) {
		this.parentName = parentName;
		sendDelta(9, parentName, StringType.ASCII);
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
		sendDelta(10, resourceName, StringType.UNICODE);
	}
	
	public StringId getResourceNameId() {
		return new StringId(resourceNameId.getFile(), resourceNameId.getKey());
	}
	
	public void setResourceNameId(StringId resourceNameId) {
		this.resourceNameId = resourceNameId;
		sendDelta(11, this.resourceNameId);
	}
	
	public void createBaseline6(BaselineBuilder bb) {
		bb.addInt(maxQuantity);
		bb.addAscii(parentName);
		bb.addUnicode(resourceName);
		bb.addObject(resourceNameId);
		bb.incrementOperandCount(4);
	}
	
	public void parseBaseline6(NetBuffer data) {
		maxQuantity = data.getInt();
		parentName = data.getAscii();
		resourceName = data.getUnicode();
		resourceNameId = data.getEncodable(StringId.class);
	}
	
	@Override
	public void readMongo(MongoData data) {
		maxQuantity = data.getInteger("maxQuantity", maxQuantity);
		parentName = data.getString("parentName", parentName);
		resourceName = data.getString("resourceName", resourceName);
		data.getDocument("resourceNameId", resourceNameId);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("maxQuantity", maxQuantity);
		data.putString("parentName", parentName);
		data.putString("resourceName", resourceName);
		data.putDocument("resourceNameId", resourceNameId);
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(6, update, o);
	}
	
	private void sendDelta(int update, String o, StringType stringType) {
		obj.sendDelta(6, update, o, stringType);
	}
	
}
