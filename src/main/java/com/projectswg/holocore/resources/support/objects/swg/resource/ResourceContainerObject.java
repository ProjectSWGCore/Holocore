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
package com.projectswg.holocore.resources.support.objects.swg.resource;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;

public class ResourceContainerObject extends TangibleObject {
	
	private final ResourceContainerObjectShared		base3	= new ResourceContainerObjectShared(this);
	private final ResourceContainerObjectSharedNP	base6	= new ResourceContainerObjectSharedNP(this);
	
	public ResourceContainerObject(long objectId) {
		super(objectId, BaselineType.RCNO);
	}
	
	@Override
	public int getCounter() {
		return base3.getQuantity();
	}
	
	@Override
	public void setCounter(int counter) {
		super.setCounter(counter);
		base3.setQuantity(counter);
	}
	
	@Override
	public int getMaxCounter() {
		return base6.getMaxQuantity();
	}
	
	public int getQuantity() {
		return base3.getQuantity();
	}
	
	public void setQuantity(int quantity) {
		base3.setQuantity(quantity);
	}
	
	public long getResourceType() {
		return base3.getResourceType();
	}
	
	public void setResourceType(long resourceType) {
		base3.setResourceType(resourceType);
	}
	
	public int getMaxQuantity() {
		return base6.getMaxQuantity();
	}
	
	public void setMaxQuantity(int maxQuantity) {
		base6.setMaxQuantity(maxQuantity);
	}
	
	public String getParentName() {
		return base6.getParentName();
	}
	
	public void setParentName(String parentName) {
		base6.setParentName(parentName);
	}
	
	public String getResourceName() {
		return base6.getResourceName();
	}
	
	public void setResourceName(String resourceName) {
		base6.setResourceName(resourceName);
	}
	
	public StringId getResourceNameId() {
		return base6.getResourceNameId();
	}
	
	public void setResourceNameId(StringId resourceNameId) {
		base6.setResourceNameId(resourceNameId);
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		base3.createBaseline3(bb);
	}
	
	@Override
	public void parseBaseline3(NetBuffer data) {
		base3.parseBaseline3(data);
	}
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		base6.createBaseline6(bb);
	}
	
	@Override
	public void parseBaseline6(NetBuffer data) {
		base6.parseBaseline6(data);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		base3.saveMongo(data.getDocument("base3"));
		base6.saveMongo(data.getDocument("base6"));
	}
	
	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		base3.readMongo(data.getDocument("base3"));
		base6.readMongo(data.getDocument("base6"));
	}
	
}
