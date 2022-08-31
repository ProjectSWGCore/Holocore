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
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;

/**
 * PLAY 6
 */
@SuppressWarnings("ClassWithTooManyFields") // forced to by SWG protocol
class PlayerObjectSharedNP implements MongoPersistable {
	
	private final PlayerObject obj;
	
	/** PLAY6-02 */ private byte			adminTag				= 0;
	/** PLAY6-03 */ private String			home					= "";
	/** PLAY6-04 */ private boolean			citizen					= false;
	/** PLAY6-08 */ private String			defaultAttackOverride	= "";

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

	public String getHome() {
		return home;
	}
	
	public void setHome(String home) {
		this.home = home;
	}
	
	public boolean isCitizen() {
		return citizen;
	}
	
	public void setCitizen(boolean citizen) {
		this.citizen = citizen;
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
		bb.addAscii(home); // 3
		bb.addBoolean(citizen); // 4
		bb.addAscii(""); // 5
		bb.addInt(0); // Citizen Rank Title? 6 bytes -- 6
		bb.addInt(0); // Environment Flags Override -- 7
		bb.addAscii(defaultAttackOverride); // Vehicle Attack Command -- 8

		bb.incrementOperandCount(6);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("adminTag", adminTag);
		data.putString("defaultAttackOverride", defaultAttackOverride);
	}

	@Override
	public void readMongo(MongoData data) {
		adminTag = (byte) data.getInteger("adminTag", adminTag);
		defaultAttackOverride = data.getString("defaultAttackOverride", defaultAttackOverride);
	}

	private void sendDelta(int update, Object o) {
		obj.sendDelta(6, update, o);
	}

	private void sendDelta(int update, String str, StringType type) {
		obj.sendDelta(6, update, str, type);
	}
}
