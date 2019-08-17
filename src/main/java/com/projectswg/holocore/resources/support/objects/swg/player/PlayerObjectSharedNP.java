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
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;

/**
 * PLAY 6
 */
@SuppressWarnings("ClassWithTooManyFields") // forced to by SWG protocol
class PlayerObjectSharedNP implements Persistable, MongoPersistable {
	
	private final PlayerObject obj;
	
	/** PLAY6-02 */ private byte			adminTag				= 0;
	/** PLAY6-03 */ private int				currentGcwRank			= 0;
	/** PLAY6-04 */ private float			currentGcwRankProgress	= 0;
	/** PLAY6-05 */ private int				maxGcwImperialRank		= 0;
	/** PLAY6-06 */ private int				maxGcwRebelRank			= 0;
	/** PLAY6-07 */ private int 			gcwNextUpdate			= 0;
	/** PLAY6-08 */ private String			citizenshipCity			= "";
	/** PLAY6-09 */ private byte			citizenshipType			= 0;
	/** PLAY6-10 */ private DefenderRegion	cityGcwDefenderRegion	= new DefenderRegion();
	/** PLAY6-11 */ private DefenderRegion	guildGcwDefenderRegion	= new DefenderRegion();
	/** PLAY6-12 */ private long			squelchedById			= 0;
	/** PLAY6-13 */ private String			squelchedByName			= "";
	/** PLAY6-14 */ private int				squelchExpireTime		= 0;
	/** PLAY6-15 */ private int				environmentFlags		= 0; // 1=forceDay 2=forceNight 3=spookyZone
	/** PLAY6-15 */ private String			defaultAttackOverride	= "";
	
	public PlayerObjectSharedNP(PlayerObject obj) {
		this.obj = obj;
	}
	
	public byte getAdminTag() {
		return adminTag;
	}
	
	public void setAdminTag(byte adminTag) {
		this.adminTag = adminTag;
		sendDelta(2, adminTag);
	}
	
	public int getCurrentGcwRank() {
		return currentGcwRank;
	}
	
	public void setCurrentGcwRank(int currentGcwRank) {
		this.currentGcwRank = currentGcwRank;
		sendDelta(3, currentGcwRank);
	}
	
	public float getCurrentGcwRankProgress() {
		return currentGcwRankProgress;
	}
	
	public void setCurrentGcwRankProgress(float currentGcwRankProgress) {
		this.currentGcwRankProgress = currentGcwRankProgress;
		sendDelta(4, currentGcwRankProgress);
	}
	
	public int getMaxGcwImperialRank() {
		return maxGcwImperialRank;
	}
	
	public void setMaxGcwImperialRank(int maxGcwImperialRank) {
		this.maxGcwImperialRank = maxGcwImperialRank;
		sendDelta(5, maxGcwImperialRank);
	}
	
	public int getMaxGcwRebelRank() {
		return maxGcwRebelRank;
	}
	
	public void setMaxGcwRebelRank(int maxGcwRebelRank) {
		this.maxGcwRebelRank = maxGcwRebelRank;
		sendDelta(6, maxGcwRebelRank);
	}
	
	public int getGcwNextUpdate() {
		return gcwNextUpdate;
	}
	
	public void setGcwNextUpdate(int gcwNextUpdate) {
		this.gcwNextUpdate = gcwNextUpdate;
		sendDelta(7, gcwNextUpdate);
	}
	
	public String getCitizenshipCity() {
		return citizenshipCity;
	}
	
	public void setCitizenshipCity(String citizenshipCity) {
		this.citizenshipCity = citizenshipCity;
		sendDelta(8, citizenshipCity, StringType.ASCII);
	}
	
	public byte getCitizenshipType() {
		return citizenshipType;
	}
	
	public void setCitizenshipType(byte citizenshipType) {
		this.citizenshipType = citizenshipType;
		sendDelta(9, citizenshipType);
	}
	
	public DefenderRegion getCityGcwDefenderRegion() {
		return new DefenderRegion(cityGcwDefenderRegion);
	}
	
	public void setCityGcwDefenderRegion(DefenderRegion cityGcwDefenderRegion) {
		this.cityGcwDefenderRegion = cityGcwDefenderRegion;
		sendDelta(10, cityGcwDefenderRegion);
	}
	
	public DefenderRegion getGuildGcwDefenderRegion() {
		return new DefenderRegion(guildGcwDefenderRegion);
	}
	
	public void setGuildGcwDefenderRegion(DefenderRegion guildGcwDefenderRegion) {
		this.guildGcwDefenderRegion = guildGcwDefenderRegion;
		sendDelta(11, guildGcwDefenderRegion);
	}
	
	public long getSquelchedById() {
		return squelchedById;
	}
	
	public void setSquelchedById(long squelchedById) {
		this.squelchedById = squelchedById;
		sendDelta(12, squelchedById);
	}
	
	public String getSquelchedByName() {
		return squelchedByName;
	}
	
	public void setSquelchedByName(String squelchedByName) {
		this.squelchedByName = squelchedByName;
		sendDelta(13, squelchedByName, StringType.ASCII);
	}
	
	public int getSquelchExpireTime() {
		return squelchExpireTime;
	}
	
	public void setSquelchExpireTime(int squelchExpireTime) {
		this.squelchExpireTime = squelchExpireTime;
		sendDelta(14, squelchExpireTime);
	}
	
	public int getEnvironmentFlags() {
		return environmentFlags;
	}
	
	public void setEnvironmentFlags(int environmentFlags) {
		this.environmentFlags = environmentFlags;
		sendDelta(15, environmentFlags);
	}
	
	public String getDefaultAttackOverride() {
		return defaultAttackOverride;
	}
	
	public void setDefaultAttackOverride(String defaultAttackOverride) {
		this.defaultAttackOverride = defaultAttackOverride;
		sendDelta(16, defaultAttackOverride, StringType.ASCII);
	}
	
	public void createBaseline6(BaselineBuilder bb) {
		bb.addByte(adminTag); // 2
		bb.addInt(currentGcwRank); // 3
		bb.addFloat(currentGcwRankProgress); // 4
		bb.addInt(maxGcwImperialRank); // 5
		bb.addInt(maxGcwRebelRank); // 6
		bb.addInt(gcwNextUpdate); // 7
		bb.addAscii(citizenshipCity); // 8
		bb.addByte(citizenshipType); // 9
		bb.addObject(cityGcwDefenderRegion); // 10
		bb.addObject(guildGcwDefenderRegion); // 11
		bb.addLong(squelchedById); // 12
		bb.addAscii(squelchedByName); // 13
		bb.addInt(squelchExpireTime); // 14
		bb.addInt(environmentFlags); // 15
		bb.addAscii(defaultAttackOverride); // 16
		
		bb.incrementOperandCount(15);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("adminTag", adminTag);
		data.putInteger("currentGcwRank", currentGcwRank);
		data.putFloat("currentGcwRankProgress", currentGcwRankProgress);
		data.putInteger("maxGcwImperialRank", maxGcwImperialRank);
		data.putInteger("maxGcwRebelRank", maxGcwRebelRank);
		data.putInteger("gcwNextUpdate", gcwNextUpdate);
		data.putString("citizenshipCity", citizenshipCity);
		data.putInteger("citizenshipType", citizenshipType);
		data.putDocument("cityGcwDefenderRegion", cityGcwDefenderRegion);
		data.putDocument("guildGcwDefenderRegion", guildGcwDefenderRegion);
		data.putLong("squelchedById", squelchedById);
		data.putString("squelchedByName", squelchedByName);
		data.putInteger("squelchExpireTime", squelchExpireTime);
		data.putInteger("environmentFlags", environmentFlags);
		data.putString("defaultAttackOverride", defaultAttackOverride);
	}
	
	@Override
	public void readMongo(MongoData data) {
		adminTag = (byte) data.getInteger("adminTag", adminTag);
		currentGcwRank = data.getInteger("currentGcwRank", currentGcwRank);
		currentGcwRankProgress = data.getFloat("currentGcwRankProgress", currentGcwRankProgress);
		maxGcwImperialRank = data.getInteger("maxGcwImperialRank", maxGcwImperialRank);
		maxGcwRebelRank = data.getInteger("maxGcwRebelRank", maxGcwRebelRank);
		gcwNextUpdate = data.getInteger("gcwNextUpdate", gcwNextUpdate);
		citizenshipCity = data.getString("citizenshipCity", citizenshipCity);
		citizenshipType = (byte) data.getInteger("citizenshipType", citizenshipType);
		cityGcwDefenderRegion = data.getDocument("cityGcwDefenderRegion", cityGcwDefenderRegion);
		guildGcwDefenderRegion = data.getDocument("guildGcwDefenderRegion", guildGcwDefenderRegion);
		squelchedById = data.getLong("squelchedById", squelchedById);
		squelchedByName = data.getString("squelchedByName", squelchedByName);
		squelchExpireTime = data.getInteger("squelchExpireTime", squelchExpireTime);
		environmentFlags = data.getInteger("environmentFlags", environmentFlags);
		defaultAttackOverride = data.getString("defaultAttackOverride", defaultAttackOverride);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addInt(adminTag);
		stream.addInt(currentGcwRank);
		stream.addFloat(currentGcwRankProgress);
		stream.addInt(maxGcwRebelRank);
		stream.addInt(maxGcwImperialRank);
		stream.addInt(gcwNextUpdate);
		stream.addAscii(citizenshipCity);
		stream.addByte(citizenshipType);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		adminTag = (byte) stream.getInt();
		currentGcwRank = stream.getInt();
		currentGcwRankProgress = stream.getFloat();
		maxGcwRebelRank = stream.getInt();
		maxGcwImperialRank = stream.getInt();
		gcwNextUpdate = stream.getInt();
		citizenshipCity = stream.getAscii();
		citizenshipType = stream.getByte();
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(6, update, o);
	}
	
	private void sendDelta(int update, String str, StringType type) {
		obj.sendDelta(6, update, str, type);
	}
	
}
