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
import com.projectswg.holocore.resources.support.data.collections.SWGFlag;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.utilities.MathUtils;

import java.util.BitSet;

/**
 * PLAY 3
 */
@SuppressWarnings("ClassWithTooManyFields") // force to by SWG protocol
class PlayerObjectShared implements MongoPersistable {

	private final PlayerObject obj;

	private final SWGFlag		flagsList			= new SWGFlag(3, 5);
	private final SWGFlag		profileFlags		= new SWGFlag(3, 6);
	private String 				title				= "";
	private int 				bornDate			= 0;
	private int 				playTime			= 0;
	private int					professionIcon		= 0;

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

	public boolean isFlagSet(int flag) {
		return flagsList.get(flag);
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

	public void setBornDate(int year, int month, int day) {
		this.bornDate = MathUtils.numberDaysSince(year, month, day, 2000, 12, 31);
	}

	public void createBaseline3(BaselineBuilder bb) {
		bb.addObject(flagsList); // 5
		bb.addObject(profileFlags); // 6
		bb.addAscii(title); // 7
		bb.addInt(bornDate); // 8
		bb.addInt(playTime); // 9
		bb.addInt(professionIcon); // 10
		
		bb.incrementOperandCount(6);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putString("title", title);
		data.putInteger("bornDate", bornDate);
		data.putInteger("playTime", playTime);
		data.putInteger("professionIcon", professionIcon);
	}

	@Override
	public void readMongo(MongoData data) {
		title = data.getString("title", title);
		bornDate = data.getInteger("bornDate", bornDate);
		playTime = data.getInteger("playTime", playTime);
		professionIcon = data.getInteger("professionIcon", professionIcon);
	}

	private void sendDelta(int update, Object o) {
		obj.sendDelta(3, update, o);
	}

	private void sendDelta(int update, String str, StringType type) {
		obj.sendDelta(3, update, str, type);
	}

}
