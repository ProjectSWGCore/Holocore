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
package resources.objects.player;

import java.util.List;

import resources.collections.SWGList;
import resources.collections.SWGSet;
import resources.network.BaselineBuilder;
import resources.network.NetBufferStream;
import resources.objects.SWGObject;
import resources.persistable.Persistable;
import resources.player.Player;
import utilities.Encoder.StringType;

class PlayerObjectPrivateNP implements Persistable {

	private int 				experimentFlag		= 0;
	private int 				craftingStage		= 0;
	private long 				nearbyCraftStation	= 0;
	private SWGList<String> 	draftSchemList		= new SWGList<>(9, 3, StringType.ASCII);
	private int 				experimentPoints	= 0;
	private SWGList<String> 	friendsList			= new SWGList<>(9, 7, StringType.ASCII);
	private SWGList<String> 	ignoreList			= new SWGList<>(9, 8, StringType.ASCII);
	private int 				languageId			= 0;
	private SWGSet<Long> 		defenders			= new SWGSet<>(9, 17);
	private int 				killMeter			= 0;
	private long 				petId				= 0;
	private SWGList<String> 	petAbilities		= new SWGList<>(9, 21);
	private SWGList<String> 	activePetAbilities	= new SWGList<>(9, 22);
	
	public PlayerObjectPrivateNP() {
		
	}
	
	public void removeFriend(String friend, SWGObject target) {
		synchronized (friendsList) {
			friendsList.remove(friend);
		}
		friendsList.sendDeltaMessage(target);
	}
	
	public void addFriend(String friend, SWGObject target) {
		synchronized (friendsList) {
			friendsList.add(friend);
		}
		friendsList.sendDeltaMessage(target);
	}
	
	public List<String> getFriendsList() {
		return friendsList;
	}
	
	public void addIgnored(String ignored, SWGObject target) {
		synchronized (ignoreList) {
			ignoreList.add(ignored);
		}
		ignoreList.sendDeltaMessage(target);
	}
	
	public void removeIgnored(String ignored, SWGObject target) {
		synchronized (ignoreList) {
			ignoreList.remove(ignored);
		}
		ignoreList.sendDeltaMessage(target);
	}
	
	public boolean isIgnored(String target) {
		return ignoreList.contains(target);
	}
	
	public List<String> getIgnoreList() {
		return ignoreList;
	}
	
	public void addDraftSchematic(String schematic, SWGObject target) {
		draftSchemList.add(schematic);
		draftSchemList.sendDeltaMessage(target);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		bb.addInt(experimentFlag); // 0
		bb.addInt(craftingStage); // 1
		bb.addLong(nearbyCraftStation); // 2
		bb.addObject(draftSchemList); // 3
		bb.addInt(0); // Might or might not be a list, two ints that are part of the same delta -- 4
		bb.addInt(0);
		bb.addInt(experimentPoints); // 5
		bb.addInt(0); // Accomplishment Counter - Pre-NGE? -- 6
		bb.addObject(friendsList); // 7
		bb.addObject(ignoreList); // 8
		bb.addInt(languageId); // 9
		bb.addInt(0); // Current Stomach -- 10
		bb.addInt(100); // Max Stomach -- 11
		bb.addInt(0); // Current Drink -- 12
		bb.addInt(100); // Max Drink -- 13
		bb.addInt(0); // Current Consumable -- 14
		bb.addInt(100); // Max Consumable -- 15
		bb.addInt(0); // Group Waypoints -- 16
		bb.addInt(0);
		bb.addObject(defenders); // 17
		bb.addInt(killMeter); // 18
		bb.addInt(0); // Unk -- 19
		bb.addLong(petId); // 20
		bb.addObject(petAbilities); // 21
		bb.addObject(activePetAbilities); // 22
		bb.addByte(0); // Unk sometimes 0x01 or 0x02 -- 23
		bb.addInt(0); // Unk sometimes 4 -- 24
		bb.addLong(0); // Unk Bitmask starts with 0x20 ends with 0x40 -- 25
		bb.addLong(0); // Unk Changes from 6 bytes to 9 -- 26
		bb.addByte(0); // Unk Changes from 6 bytes to 9 -- 27
		bb.addLong(0); // Unk sometimes 856 -- 28
		bb.addLong(0); // Unk sometimes 8559 -- 29
		bb.addInt(0); // Residence Time? Seen as Saturday 28th May 2011 -- 30
		
		bb.incrementOperandCount(31);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addInt(languageId);
		stream.addInt(killMeter);
		stream.addLong(petId);
		stream.addList(draftSchemList, (s) -> stream.addAscii(s));
		stream.addList(friendsList, (s) -> stream.addAscii(s));
		stream.addList(ignoreList, (s) -> stream.addAscii(s));
		stream.addList(petAbilities, (s) -> stream.addAscii(s));
		stream.addList(activePetAbilities, (s) -> stream.addAscii(s));
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		languageId = stream.getInt();
		killMeter = stream.getInt();
		petId = stream.getLong();
		stream.getList((i) -> draftSchemList.add(stream.getAscii()));
		stream.getList((i) -> friendsList.add(stream.getAscii()));
		stream.getList((i) -> ignoreList.add(stream.getAscii()));
		stream.getList((i) -> petAbilities.add(stream.getAscii()));
		stream.getList((i) -> activePetAbilities.add(stream.getAscii()));
	}
	
}
