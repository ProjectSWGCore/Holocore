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
package com.projectswg.holocore.resources.objects.player;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

import com.projectswg.holocore.resources.network.BaselineBuilder;
import com.projectswg.holocore.resources.player.AccessLevel;
import com.projectswg.holocore.resources.player.Player;

class PlayerObjectSharedNP implements Persistable {
	
	private int			adminTag			= 0;
	private int 		currentRank			= 0;
	private float 		rankProgress		= 0;
	private int 		highestRebelRank	= 0;
	private int 		highestImperialRank	= 0;
	private int 		gcwNextUpdate		= 0;
	private String 		home				= "";
	private boolean 	citizen				= false;
	
	public PlayerObjectSharedNP() {
		
	}
	
	public int getAdminTag() {
		return adminTag;
	}
	
	public void setAdminTag(AccessLevel access) {
		switch (access) {
			case PLAYER:	adminTag = 0; break;
			case CSR:		adminTag = 1; break;
			case DEV:		adminTag = 2; break;
			case WARDEN:	adminTag = 3; break;
			case QA:		adminTag = 4; break;
		}
	}
	
	public void setAdminTag(int tag) {
		this.adminTag = tag;
	}
	
	public int getCurrentRank() {
		return currentRank;
	}
	
	public void setCurrentRank(int currentRank) {
		this.currentRank = currentRank;
	}
	
	public float getRankProgress() {
		return rankProgress;
	}
	
	public void setRankProgress(float rankProgress) {
		this.rankProgress = rankProgress;
	}
	
	public int getHighestRebelRank() {
		return highestRebelRank;
	}
	
	public void setHighestRebelRank(int highestRebelRank) {
		this.highestRebelRank = highestRebelRank;
	}
	
	public int getHighestImperialRank() {
		return highestImperialRank;
	}
	
	public void setHighestImperialRank(int highestImperialRank) {
		this.highestImperialRank = highestImperialRank;
	}
	
	public int getGcwNextUpdate() {
		return gcwNextUpdate;
	}
	
	public void setGcwNextUpdate(int gcwNextUpdate) {
		this.gcwNextUpdate = gcwNextUpdate;
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
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		bb.addByte(adminTag); // Admin Tag (0 = none, 1 = CSR, 2 = Developer, 3 = Warden, 4 = QA) -- 2
		bb.addInt(currentRank); // 3
		bb.addFloat(rankProgress); // 4
		bb.addInt(highestImperialRank); // 5
		bb.addInt(highestRebelRank); // 6
		bb.addInt(gcwNextUpdate); // 7
		bb.addAscii(home); // 8
		bb.addBoolean(citizen); // 9
		bb.addAscii(""); // City Region Defender 'region' -- 10
			bb.addBoolean(false); // City Region Defender byte #1
			bb.addBoolean(false); // City Region Defender byte #2
		bb.addAscii(""); // Guild Region Defender 'region' -- 11
			bb.addBoolean(false); // Guild Region Defender byte #1
			bb.addBoolean(false); // Guild Region Defender byte #2
		bb.addLong(0); // General? -- 12
		bb.addAscii(""); // 13
		bb.addInt(0); // Citizen Rank Title? 6 bytes -- 14
		bb.addInt(0); // Environment Flags Override -- 15
		bb.addAscii(""); // Vehicle Attack Command -- 16
		
		bb.incrementOperandCount(15);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addInt(adminTag);
		stream.addInt(currentRank);
		stream.addFloat(rankProgress);
		stream.addInt(highestRebelRank);
		stream.addInt(highestImperialRank);
		stream.addInt(gcwNextUpdate);
		stream.addAscii(home);
		stream.addBoolean(citizen);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		adminTag = stream.getInt();
		currentRank = stream.getInt();
		rankProgress = stream.getFloat();
		highestRebelRank = stream.getInt();
		highestImperialRank = stream.getInt();
		gcwNextUpdate = stream.getInt();
		home = stream.getAscii();
		citizen = stream.getBoolean();
	}
	
}
