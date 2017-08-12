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

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;

public class AuctionQueryHeadersResponseMessage extends SWGPacket {
	
	public static final int CRC = com.projectswg.common.data.CRC.getCrc("AuctionQueryHeadersResponseMessage");
	
	private int counter;
	private int screen;
	private List <AuctionItem> items;
	
	public AuctionQueryHeadersResponseMessage() {
		items = new ArrayList<AuctionItem>();
	}
	
	public AuctionQueryHeadersResponseMessage(NetBuffer data) {
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		counter = data.getInt();
		screen = data.getInt();
		String [] locations = new String[data.getInt()];
		for (int i = 0; i < locations.length; i++)
			locations[i] = data.getAscii();
		int itemCount = data.getInt();
		AuctionItem [] items = new AuctionItem[itemCount];
		for (int itemI = 0; itemI < itemCount; itemI++) {
			AuctionItem item = new AuctionItem();
			item.setItemName(data.getUnicode());
			items[itemI] = item;
		}
		itemCount = data.getInt();
		if (itemCount != items.length)
			throw new IllegalStateException("I WAS LIED TO!");
		for (int itemI = 0; itemI < itemCount; itemI++) {
			AuctionItem item = items[itemI];
			item.setObjectId(data.getLong());
			data.getByte();
			item.setPrice(data.getInt());
			item.setExpireTime(data.getInt()*1000L+System.currentTimeMillis());
			if (data.getInt() != item.getPrice())
				throw new IllegalStateException("I WAS LIED TO AT INDEX " + itemI);
			item.setVuid(locations[data.getShort()]);
			item.setOwnerId(data.getLong());
			item.setOwnerName(locations[data.getShort()]);
			data.getLong();
			data.getInt();
			data.getInt();
			data.getShort();
			item.setItemType(data.getInt());
			data.getInt();
			item.setAuctionOptions(data.getInt());
			data.getInt();
		}
		data.getShort();
		data.getByte();
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(6);
		data.addShort(8);
		data.addInt(CRC);
		data.addInt(counter);
		data.addInt(screen);
		
		Set <String> locations = new LinkedHashSet<String>();
		for (AuctionItem item : items) {
			locations.add(item.getVuid());
			locations.add(item.getOwnerName());
		}
		data.addInt(items.size());
		for (String item : locations) {
			data.addAscii(item);
		}
		
		data.addInt(items.size());
		for (AuctionItem item : items)
			data.addUnicode(item.getItemName());
		
		data.addInt(items.size());
		
		int i = 0;
		for(AuctionItem item : items) {
			data.addLong(item.getObjectId());
			data.addByte(i);
			data.addInt(item.getPrice());
			data.addInt((int) ((item.getExpireTime() - System.currentTimeMillis()) / 1000));
			data.addInt(item.getPrice()); // if != price then auction instead of instant sale
			//data.addInt(0);
			data.addShort(getString(locations, item.getVuid()));
			data.addLong(item.getOwnerId());
			data.addShort(getString(locations, item.getOwnerName()));
			data.addLong(0);
			data.addInt(0); // unk seen as 2 mostly, doesnt seem to have any effect
			data.addInt(0);
			data.addShort((short) 0);
			data.addInt(item.getItemType()); // gameObjectType/category bitmask

			data.addInt(0); 
			int options = 0;
			
			if (item.getStatus() == AuctionState.OFFERED || item.getStatus() == AuctionState.FORSALE) 
				options |= 0x800;
			
			data.addInt(item.getAuctionOptions() | options);
			data.addInt(0);
			i++;
		}
		
		data.addShort(0);
		
		data.addByte((byte) 0);
		return data;
	}
	
	private int getString(Set <String> strings, String str) {
		int index = 0;
		for (String s : strings) {
			if (s.equals(str))
				return index;
			index++;
		}
		return index;
	}
	
	public static enum AuctionState {
		PREMIUM		(0x400),
		WITHDRAW	(0x800),
		FORSALE		(1),
		SOLD		(2),
		EXPIRED		(4),
		OFFERED		(5),
		RETRIEVED	(6);
		
		private int id;
		
		AuctionState(int id) {
			this.id = id;
		}
		
		public int getId() { return id; }
	}
	
	public static class AuctionItem {
		private long objectId;
		private long ownerId;
		private long vendorId;
		private long buyerId;
		private long offerToId;
		private int itemType;
		private int itemTypeCRC;
		private String ownerName;
		private String bidderName;
		private String itemName;
		private String itemDescription;
		private String planet;
		private String location;
		private int price;
		private int proxyBid;
		private boolean auction;
		private String vuid;
		private boolean onBazaar = false;
		private long expireTime;
		private int auctionOptions;
		private AuctionState state;
		
		public long getObjectId() { return objectId; }
		public long getOwnerId() { return ownerId; }
		public long getVendorId() { return vendorId; }
		public long getBuyerId() { return buyerId; }
		public long getOfferToId() { return offerToId; }
		public int getItemType() { return itemType; }
		public String getOwnerName() { return ownerName; }
		public String getBidderName() { return bidderName; }
		public String getItemName() { return itemName; }
		public String getLocation() { return location; }
		public int getPrice() { return price; }
		public int getProxyBid() { return proxyBid; }
		public boolean isAuction() { return auction; }
		public String getVuid() { return vuid; }
		public AuctionState getStatus() { return state; }
		public boolean isOnBazaar() { return onBazaar; }
		public int getAuctionOptions() { return auctionOptions; }
		public String getPlanet() { return planet; }
		public int getItemTypeCRC() { return itemTypeCRC; }
		
		public String getItemDescription() { return itemDescription; }
		public void setObjectId(long objectId) { this.objectId = objectId; }
		public void setOwnerId(long ownerId) { this.ownerId = ownerId; }
		public void setVendorId(long vendorId) { this.vendorId = vendorId; }
		public void setBuyerId(long buyerId) { this.buyerId = buyerId; }
		public void setOfferToId(long offerToId) { this.offerToId = offerToId; }
		public void setItemType(int itemType) { this.itemType = itemType; }
		public void setOwnerName(String ownerName) { this.ownerName = ownerName; }
		public void setBidderName(String bidderName) { this.bidderName = bidderName; }
		public void setItemName(String itemName) { this.itemName = itemName; }
		public void setLocation(String location) { this.location = location; }
		public void setItemDescription(String itemDescription) { this.itemDescription = itemDescription; }
		public void setPrice(int price) { this.price = price; }
		public void setProxyBid(int proxyBid) { this.proxyBid = proxyBid; }
		public void setAuction(boolean auction) { this.auction = auction; }
		public void setVuid(String vuid) { this.vuid = vuid; }
		public void setStatus(AuctionState state) { this.state = state; }
		public void setOnBazaar(boolean onBazaar) { this.onBazaar = onBazaar; }
		public long getExpireTime() { return expireTime; }
		public void setExpireTime(long expireTime) { this.expireTime = expireTime; }
		public void setAuctionOptions(int auctionOptions) { this.auctionOptions = auctionOptions; }
		public void setPlanet(String planet) { this.planet = planet; }
		public void setItemTypeCRC(int itemTypeCRC) { this.itemTypeCRC = itemTypeCRC; }
	}

}
