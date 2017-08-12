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
package network.packets.swg.zone.auction;

import com.projectswg.common.data.EnumLookup;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class IsVendorOwnerResponseMessage extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("IsVendorOwnerResponseMessage");

	private int ownerResult;
	private int auctionResult;
	private long containerId;
	private String marketName;
	private short maxPageSize;
	
	public IsVendorOwnerResponseMessage(long containerId, String marketName, int auctionResult, int ownerResult, short maxPageSize) {
		super();
		this.containerId = containerId;	
		this.marketName = marketName;
		this.auctionResult = auctionResult;
		this.ownerResult = ownerResult;
		this.maxPageSize = maxPageSize;
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;	
		containerId = data.getLong();
		auctionResult = data.getInt();
		marketName = data.getAscii();
		ownerResult = data.getInt();
		maxPageSize = data.getShort();
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(26 + marketName.length());
		data.addShort(5);
		data.addInt(CRC);
		data.addLong(containerId);
		data.addInt(auctionResult);
		data.addAscii(marketName);
		data.addInt(ownerResult);
		data.addShort(maxPageSize);
		return data;
	}	

	public int getOwnerResult() {
		return ownerResult;
	}

	public int getAuctionResult() {
		return auctionResult;
	}

	public long getContainerId() {
		return containerId;
	}

	public String getMarketName() {
		return marketName;
	}

	public short getMaxPageSize() {
		return maxPageSize;
	}
	
	public enum VendorOwnerResult {
		UNDEFINED					(Integer.MIN_VALUE),
		IS_OWNER					(0),
		IS_NOT_OWNER				(1),
		IS_BAZAAR					(2);

		
		private static final EnumLookup<Integer, VendorOwnerResult> LOOKUP = new EnumLookup<>(VendorOwnerResult.class, t -> t.getId());
		
		private int id;
		
		VendorOwnerResult(int id) {
			this.id = id;
		}	
		
		public int getId() {
			return id;
		}
		
		public static VendorOwnerResult getTypeForInt(int id) {
			return LOOKUP.getEnum(id, UNDEFINED);
		}
	}	
}