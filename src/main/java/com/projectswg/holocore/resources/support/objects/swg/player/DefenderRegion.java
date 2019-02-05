/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;

public class DefenderRegion implements Encodable, MongoPersistable {
	
	private String region;
	private boolean qualifyForRegionBonus;
	private boolean qualifyForRegionDefenderTitle;
	
	public DefenderRegion() {
		this("", false, false);
	}
	
	public DefenderRegion(DefenderRegion region) {
		this(region.getRegion(), region.isQualifyForRegionBonus(), region.isQualifyForRegionDefenderTitle());
	}
	
	public DefenderRegion(String region, boolean qualifyForRegionBonus, boolean qualifyForRegionDefenderTitle) {
		this.region = region;
		this.qualifyForRegionBonus = qualifyForRegionBonus;
		this.qualifyForRegionDefenderTitle = qualifyForRegionDefenderTitle;
	}
	
	public String getRegion() {
		return region;
	}
	
	public void setRegion(String region) {
		this.region = region;
	}
	
	public boolean isQualifyForRegionBonus() {
		return qualifyForRegionBonus;
	}
	
	public void setQualifyForRegionBonus(boolean qualifyForRegionBonus) {
		this.qualifyForRegionBonus = qualifyForRegionBonus;
	}
	
	public boolean isQualifyForRegionDefenderTitle() {
		return qualifyForRegionDefenderTitle;
	}
	
	public void setQualifyForRegionDefenderTitle(boolean qualifyForRegionDefenderTitle) {
		this.qualifyForRegionDefenderTitle = qualifyForRegionDefenderTitle;
	}
	
	@Override
	public void readMongo(MongoData data) {
		this.region = data.getString("region", region);
		this.qualifyForRegionBonus = data.getBoolean("qualifyForRegionBonus", qualifyForRegionBonus);
		this.qualifyForRegionDefenderTitle = data.getBoolean("qualifyForRegionDefenderTitle", qualifyForRegionDefenderTitle);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putString("region", region);
		data.putBoolean("qualifyForRegionBonus", qualifyForRegionBonus);
		data.putBoolean("qualifyForRegionDefenderTitle", qualifyForRegionDefenderTitle);
	}
	
	@Override
	public void decode(NetBuffer data) {
		this.region = data.getAscii();
		this.qualifyForRegionBonus = data.getBoolean();
		this.qualifyForRegionDefenderTitle = data.getBoolean();
	}
	
	@Override
	public byte[] encode() {
		NetBuffer data = NetBuffer.allocate(getLength());
		data.addAscii(region);
		data.addBoolean(qualifyForRegionBonus);
		data.addBoolean(qualifyForRegionDefenderTitle);
		return data.getBuffer().array();
	}
	
	@Override
	public int getLength() {
		return 4 + region.length();
	}
}
