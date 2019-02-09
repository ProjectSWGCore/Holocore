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
import com.projectswg.holocore.resources.support.data.collections.SWGBitSet;
import com.projectswg.holocore.resources.support.data.collections.SWGFlag;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * PLAY 3
 */
@SuppressWarnings("ClassWithTooManyFields") // force to by SWG protocol
class PlayerObjectShared implements Persistable, MongoPersistable {
	
	private final PlayerObject obj;
	
	/** PLAY3-05 */ private final	SWGFlag		flagsList			= new SWGFlag(3, 5); // special case for BitSet
	/** PLAY3-06 */ private final	SWGFlag		profileFlags		= new SWGFlag(3, 6); // special case for BitSet
	/** PLAY3-07 */ private 		String 		title				= "";
	/** PLAY3-08 */ private			int 		bornDate			= 0;
	/** PLAY3-09 */ private			int 		playTime			= 0;
	/** PLAY3-10 */ private			int			professionIcon		= 0;
	/** PLAY3-11 */ private			String		profession			= "";
	/** PLAY3-12 */ private			int 		gcwPoints			= 0;
	/** PLAY3-13 */ private			int 		pvpKills			= 0;
	/** PLAY3-14 */ private			long 		lifetimeGcwPoints	= 0;
	/** PLAY3-15 */ private			int 		lifetimePvpKills	= 0;
	/** PLAY3-16 */ private final	SWGBitSet	collectionBadges	= new SWGBitSet(3, 16);
	/** PLAY3-17 */ private final	SWGBitSet	collectionBadges2	= new SWGBitSet(3, 17);
	/** PLAY3-18 */ private			boolean		showBackpack		= true;
	/** PLAY3-19 */ private			boolean		showHelmet			= true;
	
	public PlayerObjectShared(PlayerObject obj) {
		this.obj = obj;
	}
	
	public BitSet getFlagsList() {
		return (BitSet) flagsList.clone();
	}
	
	public void setFlag(int flag) {
		flagsList.set(flag);
		flagsList.sendDeltaMessage(obj);
	}
	
	public void clearFlag(int flag) {
		flagsList.clear(flag);
		flagsList.sendDeltaMessage(obj);
	}
	
	public void toggleFlag(int flag) {
		flagsList.flip(flag);
		flagsList.sendDeltaMessage(obj);
	}
	
	public void setFlags(BitSet flags) {
		flagsList.or(flags);
		flagsList.sendDeltaMessage(obj);
	}
	
	public void clearFlags(BitSet flags) {
		flagsList.andNot(flags);
		flagsList.sendDeltaMessage(obj);
	}
	
	public void toggleFlags(BitSet flags) {
		flagsList.xor(flags);
		flagsList.sendDeltaMessage(obj);
	}
	
	public BitSet getProfileFlags() {
		return (BitSet) profileFlags.clone();
	}
	
	public void setProfileFlag(int flag) {
		profileFlags.set(flag);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public void clearProfileFlag(int flag) {
		profileFlags.clear(flag);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public void toggleProfileFlag(int flag) {
		profileFlags.flip(flag);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public void setProfileFlags(BitSet flags) {
		profileFlags.or(flags);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public void clearProfileFlags(BitSet flags) {
		profileFlags.andNot(flags);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public void toggleProfileFlags(BitSet flags) {
		profileFlags.xor(flags);
		profileFlags.sendDeltaMessage(obj);
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
		sendDelta(7, title, StringType.ASCII);
	}
	
	public int getBornDate() {
		return bornDate;
	}
	
	public void setBornDate(int bornDate) {
		this.bornDate = bornDate;
		sendDelta(8, bornDate);
	}
	
	public int getPlayTime() {
		return playTime;
	}
	
	public void setPlayTime(int playTime) {
		this.playTime = playTime;
		sendDelta(9, playTime);
	}
	
	public void incrementPlayTime(int playTime) {
		this.playTime += playTime;
		sendDelta(9, this.playTime);
	}
	
	public int getProfessionIcon() {
		return professionIcon;
	}
	
	public void setProfessionIcon(int professionIcon) {
		this.professionIcon = professionIcon;
		sendDelta(10, professionIcon);
	}
	
	public String getProfession() {
		return profession;
	}
	
	public void setProfession(String profession) {
		this.profession = profession;
		sendDelta(11, profession, StringType.ASCII);
	}
	
	public int getGcwPoints() {
		return gcwPoints;
	}
	
	public void setGcwPoints(int gcwPoints) {
		this.gcwPoints = gcwPoints;
		sendDelta(12, gcwPoints);
	}
	
	public int getPvpKills() {
		return pvpKills;
	}
	
	public void setPvpKills(int pvpKills) {
		this.pvpKills = pvpKills;
		sendDelta(13, pvpKills);
	}
	
	public long getLifetimeGcwPoints() {
		return lifetimeGcwPoints;
	}
	
	public void setLifetimeGcwPoints(long lifetimeGcwPoints) {
		this.lifetimeGcwPoints = lifetimeGcwPoints;
		sendDelta(14, lifetimeGcwPoints);
	}
	
	public int getLifetimePvpKills() {
		return lifetimePvpKills;
	}
	
	public void setLifetimePvpKills(int lifetimePvpKills) {
		this.lifetimePvpKills = lifetimePvpKills;
		sendDelta(15, lifetimePvpKills);
	}
	
	public List<Integer> getCollectionBadgeIds() {
		List<Integer> flags = new ArrayList<>();
		for (int flag = collectionBadges.nextSetBit(0); flag >= 0; flag = collectionBadges.nextSetBit(flag+1)) {
			if (flag == Integer.MAX_VALUE)
				break;
			flags.add(flag);
		}
		for (int flag = collectionBadges2.nextSetBit(0); flag >= 0; flag = collectionBadges2.nextSetBit(flag+1)) {
			if (flag == Integer.MAX_VALUE)
				break;
			flags.add(flag);
		}
		return flags;
	}
	
	public BitSet getCollectionBadges() {
		BitSet ret = (BitSet) collectionBadges.clone();
		ret.or(collectionBadges2);
		return ret;
	}
	
	public boolean getCollectionFlag(int flag) {
		return collectionBadges.get(flag);
	}
	
	public void setCollectionFlag(int flag) {
		SWGBitSet set = getCollections(flag);
		if (!set.get(flag)) {
			set.set(flag);
			set.sendDeltaMessage(obj);
		}
	}
	
	public void clearCollectionFlag(int flag) {
		SWGBitSet set = getCollections(flag);
		if (set.get(flag)) {
			set.clear(flag);
			set.sendDeltaMessage(obj);
		}
	}
	
	public void toggleCollectionFlag(int flag) {
		SWGBitSet set = getCollections(flag);
		set.flip(flag);
		set.sendDeltaMessage(obj);
	}
	
	public void setCollectionFlags(BitSet flags) {
		boolean triggeredCollection1 = false;
		boolean triggeredCollection2 = false;
		for (int flag = flags.nextSetBit(0); flag >= 0; flag = flags.nextSetBit(flag+1)) {
			if (flag == Integer.MAX_VALUE)
				break;
			SWGBitSet set = getCollections(flag);
			set.set(flag);
			triggeredCollection1 |= set == collectionBadges;
			triggeredCollection2 |= set == collectionBadges2;
		}
		if (triggeredCollection1)
			collectionBadges.sendDeltaMessage(obj);
		if (triggeredCollection2)
			collectionBadges2.sendDeltaMessage(obj);
	}
	
	public void clearCollectionFlags(BitSet flags) {
		boolean triggeredCollection1 = false;
		boolean triggeredCollection2 = false;
		for (int flag = flags.nextSetBit(0); flag >= 0; flag = flags.nextSetBit(flag+1)) {
			if (flag == Integer.MAX_VALUE)
				break;
			SWGBitSet set = getCollections(flag);
			set.clear(flag);
			triggeredCollection1 |= set == collectionBadges;
			triggeredCollection2 |= set == collectionBadges2;
		}
		if (triggeredCollection1)
			collectionBadges.sendDeltaMessage(obj);
		if (triggeredCollection2)
			collectionBadges2.sendDeltaMessage(obj);
	}
	
	public void toggleCollectionFlags(BitSet flags) {
		boolean triggeredCollection1 = false;
		boolean triggeredCollection2 = false;
		for (int flag = flags.nextSetBit(0); flag >= 0; flag = flags.nextSetBit(flag+1)) {
			if (flag == Integer.MAX_VALUE)
				break;
			SWGBitSet set = getCollections(flag);
			set.flip(flag);
			
			triggeredCollection1 |= set == collectionBadges;
			triggeredCollection2 |= set == collectionBadges2;
		}
		if (triggeredCollection1)
			collectionBadges.sendDeltaMessage(obj);
		if (triggeredCollection2)
			collectionBadges2.sendDeltaMessage(obj);
	}
	
	public boolean isShowBackpack() {
		return showBackpack;
	}
	
	public void setShowBackpack(boolean showBackpack) {
		this.showBackpack = showBackpack;
		sendDelta(18, showBackpack);
	}
	
	public boolean isShowHelmet() {
		return showHelmet;
	}
	
	public void setShowHelmet(boolean showHelmet) {
		this.showHelmet = showHelmet;
		sendDelta(19, showHelmet);
	}
	
	public void createBaseline3(BaselineBuilder bb) {
		bb.addObject(flagsList); // 5
		bb.addObject(profileFlags); // 6
		bb.addAscii(title); // 7
		bb.addInt(bornDate); // 8
		bb.addInt(playTime); // 9
		bb.addInt(professionIcon); // 10
		bb.addAscii(profession); // 11
		bb.addInt(gcwPoints); // 12
		bb.addInt(pvpKills); // 13
		bb.addLong(lifetimeGcwPoints); // 14
		bb.addInt(lifetimePvpKills); // 15
		bb.addObject(collectionBadges); // 16
		bb.addObject(collectionBadges2); // 17
		bb.addBoolean(showBackpack); // 18
		bb.addBoolean(showHelmet); // 19
		
		bb.incrementOperandCount(15);
	}
	
	@Override
	public void saveMongo(MongoData data) {
	}
	
	@Override
	public void readMongo(MongoData data) {
		title = data.getString("title", title);
		profession = data.getString("profession", profession);
		bornDate = data.getInteger("bornDate", bornDate);
		playTime = data.getInteger("playTime", playTime);
		professionIcon = data.getInteger("professionIcon", professionIcon);
		gcwPoints = data.getInteger("gcwPoints", gcwPoints);
		pvpKills = data.getInteger("pvpKills", pvpKills);
		lifetimePvpKills = data.getInteger("lifetimePvpKills", lifetimePvpKills);
		lifetimeGcwPoints = data.getLong("lifetimeGcwPoints", lifetimeGcwPoints);
		showBackpack = data.getBoolean("showBackpack", showBackpack);
		showHelmet = data.getBoolean("showHelmet", showHelmet);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		flagsList.save(stream);
		profileFlags.save(stream);
		collectionBadges.save(stream);
		stream.addAscii(title);
		stream.addAscii(profession);
		stream.addInt(bornDate);
		stream.addInt(playTime);
		stream.addInt(professionIcon);
		stream.addInt(gcwPoints);
		stream.addInt(pvpKills);
		stream.addInt(lifetimePvpKills);
		stream.addLong(lifetimeGcwPoints);
		stream.addBoolean(showBackpack);
		stream.addBoolean(showHelmet);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		flagsList.read(stream);
		profileFlags.read(stream);
		collectionBadges.read(stream);
		title = stream.getAscii();
		profession = stream.getAscii();
		bornDate = stream.getInt();
		playTime = stream.getInt();
		professionIcon = stream.getInt();
		gcwPoints = stream.getInt();
		pvpKills = stream.getInt();
		lifetimePvpKills = stream.getInt();
		lifetimeGcwPoints = stream.getLong();
		showBackpack = stream.getBoolean();
		showHelmet = stream.getBoolean();
	}
	
	private SWGBitSet getCollections(int beginSlotId) {
		int index = beginSlotId / 16000;
		assert index >= 0 && index <= 1;
		return index == 0 ? collectionBadges : collectionBadges2;
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(3, update, o);
	}
	
	private void sendDelta(int update, String str, StringType type) {
		obj.sendDelta(3, update, str, type);
	}
	
}
