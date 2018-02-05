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

import java.util.BitSet;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import resources.collections.SWGBitSet;
import resources.collections.SWGFlag;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;
import resources.player.PlayerFlags;
import utilities.MathUtils;

class PlayerObjectShared implements Persistable {

	private final SWGFlag		flagsList			= new SWGFlag(3, 5);
	private final SWGFlag		profileFlags		= new SWGFlag(3, 6);
	private final SWGBitSet		collectionBadges	= new SWGBitSet(3, 16);
	private final SWGBitSet		collectionBadges2	= new SWGBitSet(3, 17);
	
	private String 				title				= "";
	private int 				bornDate			= 0;
	private int 				playTime			= 0;
	private int					professionIcon		= 0;
	private String				profession			= "";
	private int 				gcwPoints			= 0;
	private int 				pvpKills			= 0;
	private long 				lifetimeGcwPoints	= 0;
	private int 				lifetimePvpKills	= 0;
	private boolean				showHelmet			= true;
	private boolean				showBackpack		= true;
	
	public PlayerObjectShared() {
		
	}
	
	public SWGFlag getFlagsList() {
		return flagsList;
	}
	
	public SWGFlag getProfileFlags() {
		return profileFlags;
	}
	
	public String getTitle() {
		return title;
	}
	
	public int getBornDate() {
		return bornDate;
	}
	
	public int getPlayTime() {
		return playTime;
	}
	
	public int getProfessionIcon() {
		return professionIcon;
	}
	
	public String getProfession() {
		return profession;
	}
	
	public int getGcwPoints() {
		return gcwPoints;
	}
	
	public int getPvpKills() {
		return pvpKills;
	}
	
	public long getLifetimeGcwPoints() {
		return lifetimeGcwPoints;
	}
	
	public int getLifetimePvpKills() {
		return lifetimePvpKills;
	}
	
	public byte[] getCollectionBadges() {
		synchronized (collectionBadges) {
			return collectionBadges.toByteArray();
		}
	}
	
	public boolean isShowHelmet() {
		return showHelmet;
	}
	
	public boolean isShowBackpack() {
		return showBackpack;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public void setBornDate(int bornDate) {
		this.bornDate = bornDate;
	}
	
	public void setPlayTime(int playTime) {
		this.playTime = playTime;
	}
	
	public void setProfessionIcon(int professionIcon) {
		this.professionIcon = professionIcon;
	}
	
	public void setProfession(String profession) {
		this.profession = profession;
	}
	
	public void setBornDate(int year, int month, int day) {
		this.bornDate = MathUtils.numberDaysSince(year, month, day, 2000, 12, 31);
	}
	
	public void setGcwPoints(int gcwPoints) {
		this.gcwPoints = gcwPoints;
	}
	
	public void setPvpKills(int pvpKills) {
		this.pvpKills = pvpKills;
	}
	
	public void setLifetimeGcwPoints(long lifetimeGcwPoints) {
		this.lifetimeGcwPoints = lifetimeGcwPoints;
	}
	
	public void setLifetimePvpKills(int lifetimePvpKills) {
		this.lifetimePvpKills = lifetimePvpKills;
	}
	
	public void setCollectionBadges(byte [] collection, SWGObject target) {
		synchronized (collectionBadges) {
			this.collectionBadges.clear();
			this.collectionBadges.or(BitSet.valueOf(collection));
			collectionBadges.sendDeltaMessage(target);
		}
	}
	
	public void setShowHelmet(boolean showHelmet) {
		this.showHelmet = showHelmet;
	}
	
	public void setShowBackpack(boolean showBackpack) {
		this.showBackpack = showBackpack;
	}
	
	public void setFlagBitmask(SWGObject target, PlayerFlags ... flags) {
		boolean changed = false;
		for (PlayerFlags flag : flags) {
			changed |= !flagsList.get(flag.getFlag());
			flagsList.set(flag.getFlag());
		}
		if (changed)
			flagsList.sendDeltaMessage(target);
	}
	
	public void clearFlagBitmask(SWGObject target, PlayerFlags ... flags) {
		boolean changed = false;
		for (PlayerFlags flag : flags) {
			changed |= flagsList.get(flag.getFlag());
			flagsList.clear(flag.getFlag());
		}
		if (changed)
			flagsList.sendDeltaMessage(target);
	}
	
	public void toggleFlag(SWGObject target, PlayerFlags ... flags) {
		for (PlayerFlags flag : flags)
			flagsList.flip(flag.getFlag());
		flagsList.sendDeltaMessage(target);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		bb.addObject(flagsList); // 4 flags -- 5
		bb.addObject(profileFlags); // 4 flags -- 6
		bb.addAscii(title); // 7
		bb.addInt(bornDate); // Born Date -- 4001 = 12/15/2011 || Number of days after 12/31/2000 -- 8
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
	
}
