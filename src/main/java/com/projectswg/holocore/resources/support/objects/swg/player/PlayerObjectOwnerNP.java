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
package com.projectswg.holocore.resources.support.objects.swg.player;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.common.utilities.ByteUtilities;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.resources.support.data.collections.SWGBitSet;
import com.projectswg.holocore.resources.support.data.collections.SWGList;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * PLAY 9
 */
@SuppressWarnings("ClassWithTooManyFields") // no choice due to SWG protocol
class PlayerObjectOwnerNP implements Persistable, MongoPersistable {
	
	private final PlayerObject obj;
	
	/** PLAY9-00 */ private			int					craftingLevel				= 0;
	/** PLAY9-01 */ private			int 				craftingStage				= 0;
	/** PLAY9-02 */ private			long 				nearbyCraftStation			= 0;
	/** PLAY9-03 */ private final	SWGMap<Long, Integer> draftSchematics			= new SWGMap<>(9, 3);
	/** PLAY9-04 */ private			long				craftingComponentBioLink	= 0;
	/** PLAY9-05 */ private			int 				experimentPoints			= 0;
	/** PLAY9-06 */ private			int					expModified					= 0;
	/** PLAY9-07 */ private final	SWGList<String>		friendsList					= new SWGList<>(9, 7, StringType.ASCII);
	/** PLAY9-08 */ private final	SWGList<String>		ignoreList					= new SWGList<>(9, 8, StringType.ASCII);
	/** PLAY9-09 */ private			int 				languageId					= 0;
	/** PLAY9-10 */ private			int					food						= 0;
	/** PLAY9-11 */ private			int					maxFood						= 100;
	/** PLAY9-12 */ private			int					drink						= 0;
	/** PLAY9-13 */ private			int					maxDrink					= 100;
	/** PLAY9-14 */ private			int					meds						= 0;
	/** PLAY9-15 */ private			int					maxMeds						= 100;
	/** PLAY9-16 */ private final	SWGMap<Long, WaypointObject> groupWaypoints		= new SWGMap<>(9, 16);
	/** PLAY9-17 */ private final	SWGSet<Long>		playerHateList				= new SWGSet<>(9, 17);
	/** PLAY9-18 */ private			int 				killMeter					= 0;
	/** PLAY9-19 */ private			int					accountLotsOverLimit		= 0;
	/** PLAY9-20 */ private			long 				petId						= 0;
	/** PLAY9-21 */ private final	SWGList<String> 	petAbilities				= new SWGList<>(9, 21, StringType.ASCII);
	/** PLAY9-22 */ private final	SWGList<String> 	activePetAbilities			= new SWGList<>(9, 22, StringType.ASCII);
	/** PLAY9-23 */ private			byte				galacticReserveDeposit		= 0;
	/** PLAY9-24 */ private final	SWGBitSet			guildRank					= new SWGBitSet(9, 24);
	/** PLAY9-25 */ private final	SWGBitSet			citizenRank					= new SWGBitSet(9, 25);
	/** PLAY9-27 */ private			long				pgcRatingCount				= 0;
	/** PLAY9-28 */ private			long				pgcRatingTotal				= 0;
	/** PLAY9-29 */ private			int					pgcLastRatingTime			= 0;
	
	public PlayerObjectOwnerNP(PlayerObject obj) {
		this.obj = obj;
	}
	
	public int getCraftingLevel() {
		return craftingLevel;
	}
	
	public void setCraftingLevel(int craftingLevel) {
		this.craftingLevel = craftingLevel;
		sendDelta(0, craftingLevel);
	}
	
	public int getCraftingStage() {
		return craftingStage;
	}
	
	public void setCraftingStage(int craftingStage) {
		this.craftingStage = craftingStage;
		sendDelta(1, craftingStage);
	}
	
	public long getNearbyCraftStation() {
		return nearbyCraftStation;
	}
	
	public void setNearbyCraftStation(long nearbyCraftStation) {
		this.nearbyCraftStation = nearbyCraftStation;
		sendDelta(2, nearbyCraftStation);
	}
	
	public Map<Long, Integer> getDraftSchematics() {
		return Collections.unmodifiableMap(draftSchematics);
	}
	
	public void setDraftSchematic(int serverCrc, int clientCrc, int counter) {
		long combinedCrc = (((long) serverCrc << 32) & 0xFFFFFFFF00000000L) | (clientCrc & 0x00000000FFFFFFFFL);
		draftSchematics.put(combinedCrc, counter);
		draftSchematics.sendDeltaMessage(obj);
	}
	
	public long getCraftingComponentBioLink() {
		return craftingComponentBioLink;
	}
	
	public void setCraftingComponentBioLink(long craftingComponentBioLink) {
		this.craftingComponentBioLink = craftingComponentBioLink;
		sendDelta(4, craftingComponentBioLink);
	}
	
	public int getExperimentPoints() {
		return experimentPoints;
	}
	
	public void setExperimentPoints(int experimentPoints) {
		this.experimentPoints = experimentPoints;
		sendDelta(5, experimentPoints);
	}
	
	public int getExpModified() {
		return expModified;
	}
	
	public void incrementExpModified() {
		this.expModified++;
		sendDelta(6, expModified);
	}
	
	public List<String> getFriendsList() {
		return Collections.unmodifiableList(friendsList);
	}
	
	public boolean addFriend(String friendName) {
		synchronized (friendsList) {
			String friendLower = friendName.toLowerCase(Locale.US);
			if (friendsList.contains(friendLower))
				return false;
			friendsList.add(friendLower);
			friendsList.sendDeltaMessage(obj);
			return true;
		}
	}
	
	public boolean removeFriend(String friendName) {
		if (friendsList.remove(friendName.toLowerCase(Locale.US))) {
			friendsList.sendDeltaMessage(obj);
			return true;
		}
		return false;
	}
	
	public boolean isFriend(String friendName) {
		return friendsList.contains(friendName.toLowerCase(Locale.US));
	}
	
	public void sendFriendList() {
		friendsList.sendRefreshedListData(obj);
	}
	
	public List<String> getIgnoreList() {
		return Collections.unmodifiableList(ignoreList);
	}
	
	public boolean addIgnored(String ignoreName) {
		synchronized (ignoreList) {
			String ignoreLower = ignoreName.toLowerCase(Locale.US);
			if (ignoreList.contains(ignoreLower))
				return false;
			ignoreList.add(ignoreLower);
			ignoreList.sendDeltaMessage(obj);
			return true;
		}
	}
	
	public boolean removeIgnored(String ignoreName) {
		if (ignoreList.remove(ignoreName.toLowerCase(Locale.US))) {
			ignoreList.sendDeltaMessage(obj);
			return true;
		}
		return false;
	}
	
	public boolean isIgnored(String ignoreName) {
		return ignoreList.contains(ignoreName.toLowerCase(Locale.US));
	}
	
	public void sendIgnoreList() {
		ignoreList.sendRefreshedListData(obj);
	}
	
	public int getLanguageId() {
		return languageId;
	}
	
	public void setLanguageId(int languageId) {
		this.languageId = languageId;
		sendDelta(9, languageId);
	}
	
	public int getFood() {
		return food;
	}
	
	public void setFood(int food) {
		this.food = food;
		sendDelta(10, food);
	}
	
	public int getMaxFood() {
		return maxFood;
	}
	
	public void setMaxFood(int maxFood) {
		this.maxFood = maxFood;
		sendDelta(11, maxFood);
	}
	
	public int getDrink() {
		return drink;
	}
	
	public void setDrink(int drink) {
		this.drink = drink;
		sendDelta(12, drink);
	}
	
	public int getMaxDrink() {
		return maxDrink;
	}
	
	public void setMaxDrink(int maxDrink) {
		this.maxDrink = maxDrink;
		sendDelta(13, maxDrink);
	}
	
	public int getMeds() {
		return meds;
	}
	
	public void setMeds(int meds) {
		this.meds = meds;
		sendDelta(14, meds);
	}
	
	public int getMaxMeds() {
		return maxMeds;
	}
	
	public void setMaxMeds(int maxMeds) {
		this.maxMeds = maxMeds;
		sendDelta(15, maxMeds);
	}
	
	public Set<WaypointObject> getGroupWaypoints() {
		return new HashSet<>(groupWaypoints.values());
	}
	
	public void addGroupWaypoint(WaypointObject waypoint) {
		if (groupWaypoints.containsKey(waypoint.getObjectId()))
			groupWaypoints.update(waypoint.getObjectId());
		else
			groupWaypoints.put(waypoint.getObjectId(), waypoint);
		groupWaypoints.sendDeltaMessage(obj);
	}
	
	public Set<Long> getPlayerHateList() {
		return Collections.unmodifiableSet(playerHateList);
	}
	
	public void addHatedPlayer(long hatedPlayerId) {
		if (playerHateList.add(hatedPlayerId))
			playerHateList.sendDeltaMessage(obj);
	}
	
	public int getKillMeter() {
		return killMeter;
	}
	
	public void setKillMeter(int killMeter) {
		this.killMeter = killMeter;
		sendDelta(18, killMeter);
	}
	
	public int getAccountLotsOverLimit() {
		return accountLotsOverLimit;
	}
	
	public void setAccountLotsOverLimit(int accountLotsOverLimit) {
		this.accountLotsOverLimit = accountLotsOverLimit;
		sendDelta(19, accountLotsOverLimit);
	}
	
	public long getPetId() {
		return petId;
	}
	
	public void setPetId(long petId) {
		this.petId = petId;
		sendDelta(20, petId);
	}
	
	public List<String> getPetAbilities() {
		return Collections.unmodifiableList(petAbilities);
	}
	
	public void addPetAbility(String ability) {
		synchronized (petAbilities) {
			if (petAbilities.contains(ability))
				return;
			petAbilities.add(ability);
			petAbilities.sendDeltaMessage(obj);
		}
	}
	
	public void removePetAbility(String ability) {
		if (petAbilities.remove(ability))
			petAbilities.sendDeltaMessage(obj);
	}
	
	public List<String> getActivePetAbilities() {
		return Collections.unmodifiableList(activePetAbilities);
	}
	
	public void addActivePetAbility(String ability) {
		synchronized (activePetAbilities) {
			if (activePetAbilities.contains(ability))
				return;
			activePetAbilities.add(ability);
			activePetAbilities.sendDeltaMessage(obj);
		}
	}
	
	public BitSet getGuildRank() {
		return (BitSet) guildRank.clone();
	}
	
	public void setGuildRank(int guildRank) {
		this.guildRank.clear();
		this.guildRank.set(guildRank);
		this.guildRank.sendDeltaMessage(obj);
	}
	
	public BitSet getCitizenRank() {
		return (BitSet) citizenRank.clone();
	}
	
	public void setCitizenRank(int citizenRank) {
		this.citizenRank.clear();
		this.citizenRank.set(citizenRank);
		this.citizenRank.sendDeltaMessage(obj);
	}
	
	public byte getGalacticReserveDeposit() {
		return galacticReserveDeposit;
	}
	
	public void setGalacticReserveDeposit(byte galacticReserveDeposit) {
		this.galacticReserveDeposit = galacticReserveDeposit;
		sendDelta(23, galacticReserveDeposit);
	}
	
	public long getPgcRatingCount() {
		return pgcRatingCount;
	}
	
	public void setPgcRatingCount(long pgcRatingCount) {
		this.pgcRatingCount = pgcRatingCount;
		sendDelta(27, pgcRatingCount);
	}
	
	public long getPgcRatingTotal() {
		return pgcRatingTotal;
	}
	
	public void setPgcRatingTotal(long pgcRatingTotal) {
		this.pgcRatingTotal = pgcRatingTotal;
		sendDelta(28, pgcRatingTotal);
	}
	
	public int getPgcLastRatingTime() {
		return pgcLastRatingTime;
	}
	
	public void setPgcLastRatingTime(int pgcLastRatingTime) {
		this.pgcLastRatingTime = pgcLastRatingTime;
		sendDelta(29, pgcLastRatingTime);
	}
	
	public void createBaseline9(BaselineBuilder bb) {
		bb.addInt(craftingLevel); // 0
		bb.addInt(craftingStage); // 1
		bb.addLong(nearbyCraftStation); // 2
		bb.addObject(draftSchematics); // 3
		bb.addLong(craftingComponentBioLink); // 4
		bb.addInt(experimentPoints); // 5
		bb.addInt(expModified); // 6
		bb.addObject(friendsList); // 7
		bb.addObject(ignoreList); // 8
		bb.addInt(languageId); // 9
		bb.addInt(food); // 10
		bb.addInt(maxFood); // 11
		bb.addInt(drink); // 12
		bb.addInt(maxDrink); // 13
		bb.addInt(meds); // 14
		bb.addInt(maxMeds); // 15
		bb.addObject(groupWaypoints); // 16
		bb.addObject(playerHateList); // 17
		bb.addInt(killMeter); // 18
		bb.addInt(accountLotsOverLimit); // 19
		bb.addLong(petId); // 20
		bb.addObject(petAbilities); // 21
		bb.addObject(activePetAbilities); // 22
		bb.addByte(galacticReserveDeposit); // 23
		bb.addObject(guildRank); // 24
		bb.addObject(citizenRank); // 25
		bb.addByte(0); // 26
		bb.addLong(pgcRatingCount); // 27
		bb.addLong(pgcRatingTotal); // 28
		bb.addInt(pgcLastRatingTime); // 29
		bb.addInt(0); // 30
		
		bb.incrementOperandCount(31);
	}
	
	public void parseBaseline9(NetBuffer buffer) {
		draftSchematics.clear();
		friendsList.clear();
		ignoreList.clear();
		groupWaypoints.clear();
		playerHateList.clear();
		petAbilities.clear();
		activePetAbilities.clear();
		guildRank.clear();
		citizenRank.clear();
		
		craftingLevel = buffer.getInt();
		craftingStage = buffer.getInt();
		nearbyCraftStation = buffer.getLong();
		draftSchematics.putAll(SWGMap.getSwgMap(buffer, 9, 3, Long.class, Integer.class));
		craftingComponentBioLink = buffer.getLong();
		experimentPoints = buffer.getInt();
		expModified = buffer.getInt();
		friendsList.addAll(SWGList.getSwgList(buffer, 9, 7, StringType.ASCII));
		ignoreList.addAll(SWGList.getSwgList(buffer, 9, 8, StringType.ASCII));
		languageId = buffer.getInt();
		food = buffer.getInt();
		maxFood = buffer.getInt();
		drink = buffer.getInt();
		maxDrink = buffer.getInt();
		meds = buffer.getInt();
		maxMeds = buffer.getInt();
		SWGMap.getSwgMap(buffer, 9, 16, Long.class, WaypointPackage.class).values().forEach(p -> groupWaypoints.put(p.getObjectId(), new WaypointObject(p)));
		playerHateList.addAll(SWGSet.getSwgSet(buffer, 9, 17, Long.class));
		killMeter = buffer.getInt();
		accountLotsOverLimit = buffer.getInt();
		petId = buffer.getLong();
		petAbilities.addAll(SWGList.getSwgList(buffer, 9, 21, StringType.ASCII));
		activePetAbilities.addAll(SWGList.getSwgList(buffer, 9, 22, StringType.ASCII));
		galacticReserveDeposit = buffer.getByte();
		guildRank.or(buffer.getEncodable(SWGBitSet.class));
		citizenRank.or(buffer.getEncodable(SWGBitSet.class));
		buffer.getByte(); // unknown
		pgcRatingCount = buffer.getLong();
		pgcRatingTotal = buffer.getLong();
		pgcLastRatingTime = buffer.getInt();
		buffer.getInt(); // unknown
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("craftingLevel", craftingLevel);
		data.putInteger("craftingStage", craftingStage);
		data.putLong("nearbyCraftStation", nearbyCraftStation);
		data.putMap("draftSchematics", draftSchematics, String::valueOf);
		data.putLong("craftingComponentBioLink", craftingComponentBioLink);
		data.putInteger("experimentPoints", experimentPoints);
		data.putInteger("expModified", expModified);
		data.putArray("friendsList", friendsList);
		data.putArray("ignoreList", ignoreList);
		data.putInteger("languageId", languageId);
		data.putInteger("food", food);
		data.putInteger("maxFood", maxFood);
		data.putInteger("drink", drink);
		data.putInteger("maxDrink", maxDrink);
		data.putInteger("meds", meds);
		data.putInteger("maxMeds", maxMeds);
		data.putArray("groupWaypoints", groupWaypoints.values().stream().map(WaypointObject::getOOB).collect(Collectors.toList()));
		data.putArray("playerHateList", playerHateList);
		data.putInteger("killMeter", killMeter);
		data.putInteger("accountLotsOverLimit", accountLotsOverLimit);
		data.putLong("petId", petId);
		data.putArray("petAbilities", petAbilities);
		data.putArray("activePetAbilities", activePetAbilities);
		data.putByteArray("guildRank", guildRank.toByteArray());
		data.putByteArray("citizenRank", citizenRank.toByteArray());
		data.putInteger("galacticReserveDeposit", galacticReserveDeposit);
		data.putLong("pgcRatingCount", pgcRatingCount);
		data.putLong("pgcRatingTotal", pgcRatingTotal);
		data.putInteger("pgcLastRatingTime", pgcLastRatingTime);
	}
	
	@Override
	public void readMongo(MongoData data) {
		byte [] emptyByteArray = new byte[0];
		draftSchematics.clear();
		friendsList.clear();
		ignoreList.clear();
		groupWaypoints.clear();
		playerHateList.clear();
		petAbilities.clear();
		activePetAbilities.clear();
		guildRank.clear();
		citizenRank.clear();
		
		craftingLevel = data.getInteger("craftingLevel", craftingLevel);
		craftingStage = data.getInteger("craftingStage", craftingStage);
		nearbyCraftStation = data.getLong("nearbyCraftStation", nearbyCraftStation);
		draftSchematics.putAll(data.getMap("draftSchematics", Integer.class, Long::valueOf, Function.identity()));
		craftingComponentBioLink = data.getLong("craftingComponentBioLink", craftingComponentBioLink);
		experimentPoints = data.getInteger("experimentPoints", experimentPoints);
		expModified = data.getInteger("expModified", expModified);
		friendsList.addAll(data.getArray("friendsList", String.class));
		ignoreList.addAll(data.getArray("ignoreList", String.class));
		languageId = data.getInteger("languageId", languageId);
		food = data.getInteger("food", food);
		maxFood = data.getInteger("maxFood", maxFood);
		drink = data.getInteger("drink", drink);
		maxDrink = data.getInteger("maxDrink", maxDrink);
		meds = data.getInteger("meds", meds);
		maxMeds = data.getInteger("maxMeds", maxMeds);
		data.getArray("groupWaypoints", doc -> new WaypointObject(MongoData.create(doc, WaypointPackage::new))).forEach(obj -> groupWaypoints.put(obj.getObjectId(), obj));
		playerHateList.addAll(data.getArray("playerHateList", Long.class));
		killMeter = data.getInteger("killMeter", killMeter);
		accountLotsOverLimit = data.getInteger("accountLotsOverLimit", accountLotsOverLimit);
		petId = data.getLong("petId", petId);
		petAbilities.addAll(data.getArray("petAbilities", String.class));
		activePetAbilities.addAll(data.getArray("activePetAbilities", String.class));
		guildRank.xor(BitSet.valueOf((data.getByteArray("guildRank", emptyByteArray))));
		citizenRank.xor(BitSet.valueOf(data.getByteArray("citizenRank", emptyByteArray)));
		galacticReserveDeposit = (byte) data.getInteger("galacticReserveDeposit", galacticReserveDeposit);
		pgcRatingCount = data.getLong("pgcRatingCount", pgcRatingCount);
		pgcRatingTotal = data.getLong("pgcRatingTotal", pgcRatingTotal);
		pgcLastRatingTime = data.getInteger("pgcLastRatingTime", pgcLastRatingTime);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addInt(languageId);
		stream.addInt(killMeter);
		stream.addLong(petId);
		stream.addList(friendsList, stream::addAscii);
		stream.addList(ignoreList, stream::addAscii);
		stream.addList(petAbilities, stream::addAscii);
		stream.addList(activePetAbilities, stream::addAscii);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		languageId = stream.getInt();
		killMeter = stream.getInt();
		petId = stream.getLong();
		stream.getList((i) -> friendsList.add(stream.getAscii()));
		stream.getList((i) -> ignoreList.add(stream.getAscii()));
		stream.getList((i) -> petAbilities.add(stream.getAscii()));
		stream.getList((i) -> activePetAbilities.add(stream.getAscii()));
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(9, update, o);
	}
	
}
