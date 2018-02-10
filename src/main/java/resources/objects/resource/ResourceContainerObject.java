/***********************************************************************************
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
package resources.objects.resource;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;

import resources.network.BaselineBuilder;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class ResourceContainerObject extends TangibleObject {
	
	private long	resourceType	= 0;
	private String	resourceName	= "";
	private int		quantity		= 0;
	private int		maxQuantity		= 0;
	private String	parentName		= "";
	private String	displayName		= "";
	
	public ResourceContainerObject(long objectId) {
		super(objectId, BaselineType.RCNO);
	}
	
	public long getResourceType() {
		return resourceType;
	}
	
	public String getResourceName() {
		return resourceName;
	}
	
	public int getQuantity() {
		return quantity;
	}
	
	public int getMaxQuantity() {
		return maxQuantity;
	}
	
	public String getParentName() {
		return parentName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public void setResourceType(long resourceType) {
		this.resourceType = resourceType;
	}
	
	public void setResourceName(String resourceName) {
		this.resourceName = resourceName;
	}
	
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}
	
	public void setMaxQuantity(int maxQuantity) {
		this.maxQuantity = maxQuantity;
	}
	
	public void setParentName(String parentName) {
		this.parentName = parentName;
	}
	
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(quantity);
		bb.addLong(resourceType);
		bb.incrementOperandCount(2);
	}
	
	@Override
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addInt(maxQuantity);
		bb.addAscii(parentName);
		bb.addUnicode(resourceName);
		bb.addLong(0); // Resource Id
		bb.incrementOperandCount(4);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ResourceContainerObject))
			return super.equals(o);
		ResourceContainerObject rcno = (ResourceContainerObject) o;
		if (!super.equals(o))
			return false;
		return maxQuantity == rcno.maxQuantity && parentName.equals(rcno.parentName) && resourceName.equals(rcno.resourceName);
	}
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash ^= maxQuantity;
		hash ^= parentName.hashCode();
		hash ^= resourceName.hashCode();
		return hash;
	}
	
	/*
	private long	resourceType	= 0;
	private String	resourceName	= "";
	private int		quantity		= 0;
	private int		maxQuantity		= 0;
	private String	parentName		= "";
	private String	displayName		= "";
	 */
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		stream.addInt(quantity);
		stream.addInt(maxQuantity);
		stream.addLong(resourceType);
		stream.addAscii(parentName);
		stream.addUnicode(resourceName);
		stream.addUnicode(displayName);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		quantity = stream.getInt();
		maxQuantity = stream.getInt();
		resourceType = stream.getLong();
		parentName = stream.getAscii();
		resourceName = stream.getUnicode();
		displayName = stream.getUnicode();
	}
	
}
