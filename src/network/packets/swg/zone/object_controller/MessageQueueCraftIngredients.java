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

import com.projectswg.common.network.NetBuffer;

public class MessageQueueCraftIngredients extends ObjectController {
	
	public static final int CRC = 0x0105;
	
	private int count;
	private String[] resourceName;
	private byte[] type;
	private int[] quantity;
	
	public MessageQueueCraftIngredients(int count, String[] resourceName, byte[] type, int[] quantity) {
		super();
		this.count = count;
		this.resourceName = resourceName;
		this.type = type;
		this.quantity = quantity;
	}
	
	public MessageQueueCraftIngredients(NetBuffer data) {
		super(CRC);
		decode(data);
	}

	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		count = data.getInt();
		for(int i = 0; i < count; i++){
			resourceName[i] = data.getUnicode();
			type[i] = data.getByte();
			quantity[i] = data.getInt();
		}		
	}

	@Override
	public NetBuffer encode() {
		 int len = 4;
		for (int i = 0; i < count; i++)
		    len += 9 + resourceName[i].length();
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 4 + len);
		encodeHeader(data);
		data.addInt(count);
		for(int i = 0; i < count; i++){
			data.addUnicode(resourceName[i] );
			data.addByte(type[i]);
			data.addInt(quantity[i]);
		}
		return data;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public String[] getResourceName() {
		return resourceName;
	}

	public void setResourceName(String[] resourceName) {
		this.resourceName = resourceName;
	}

	public byte[] getType() {
		return type;
	}

	public void setType(byte[] type) {
		this.type = type;
	}

	public int[] getQuantity() {
		return quantity;
	}

	public void setQuantity(int[] quantity) {
		this.quantity = quantity;
	}	
}