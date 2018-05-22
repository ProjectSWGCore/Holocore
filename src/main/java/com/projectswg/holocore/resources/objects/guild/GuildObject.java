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
package com.projectswg.holocore.resources.objects.guild;

import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.collections.SWGMap;
import com.projectswg.holocore.resources.collections.SWGSet;
import com.projectswg.holocore.resources.network.BaselineBuilder;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;

public class GuildObject extends SWGObject {
	
	private SWGSet<String> abbreviations = new SWGSet<>(3, 4, StringType.ASCII);
	private SWGMap<String, CurrentServerGCWZonePercent> gcwImperialScorePercentileThisGalaxy = new SWGMap<>(6, 2);
	private SWGMap<String, CurrentServerGCWZonePercent> gcwGroupImperialScorePercentileThisGalaxy = new SWGMap<>(6, 3);
	private SWGMap<String, CurrentServerGCWZoneHistory> gcwImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 4);
	private SWGMap<String, CurrentServerGCWZoneHistory> gcwGroupImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 5);
	private SWGMap<String, OtherServerGCWZonePercent> gcwImperialScorePercentileOtherGalaxies = new SWGMap<>(6, 6);
	private SWGMap<String, OtherServerGCWZonePercent> gcwGroupImperialScorePercentileOtherGalaxies = new SWGMap<>(6, 7);
	
	public GuildObject(long objectId) {
		super(objectId, BaselineType.GILD);
	}
	
	@Override
	protected void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addObject(abbreviations);
	}
	
	@Override
	protected void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addObject(gcwImperialScorePercentileThisGalaxy);
		bb.addObject(gcwGroupImperialScorePercentileThisGalaxy);
		bb.addObject(gcwImperialScorePercentileHistoryThisGalaxy);
		bb.addObject(gcwGroupImperialScorePercentileHistoryThisGalaxy);
		bb.addObject(gcwImperialScorePercentileOtherGalaxies);
		bb.addObject(gcwGroupImperialScorePercentileOtherGalaxies);
	}
	
	@Override
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		abbreviations = SWGSet.getSwgSet(buffer, 3, 5, StringType.ASCII);
	}
	
	@Override
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		gcwImperialScorePercentileThisGalaxy = SWGMap.getSwgMap(buffer, 6, 2, StringType.ASCII, CurrentServerGCWZonePercent.class);
		gcwGroupImperialScorePercentileThisGalaxy = SWGMap.getSwgMap(buffer, 6, 3, StringType.ASCII, CurrentServerGCWZonePercent.class);
		gcwImperialScorePercentileHistoryThisGalaxy = SWGMap.getSwgMap(buffer, 6, 4, StringType.ASCII, CurrentServerGCWZoneHistory.class);
		gcwGroupImperialScorePercentileHistoryThisGalaxy = SWGMap.getSwgMap(buffer, 6, 5, StringType.ASCII, CurrentServerGCWZoneHistory.class);
		gcwImperialScorePercentileOtherGalaxies = SWGMap.getSwgMap(buffer, 6, 6, StringType.ASCII, OtherServerGCWZonePercent.class);
		gcwGroupImperialScorePercentileOtherGalaxies = SWGMap.getSwgMap(buffer, 6, 7, StringType.ASCII, OtherServerGCWZonePercent.class);
	}
	
	public static class CurrentServerGCWZonePercent implements Encodable {
		
		private int percentage = 0;
		
		@Override
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(4);
			data.addInt(percentage);
			return data.array();
		}
		
		@Override
		public void decode(NetBuffer data) {
			percentage = data.getInt();
		}
		
		@Override
		public int getLength() {
			return 4;
		}
		
		@Override
		public String toString() {
			return percentage + "%";
		}
	}
	
	public static class CurrentServerGCWZoneHistory implements Encodable {
		
		private int lastUpdateTime = 0;
		private int percentage = 0;
		
		@Override
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(8);
			data.addInt(lastUpdateTime);
			data.addInt(percentage);
			return data.array();
		}
		
		@Override
		public void decode(NetBuffer data) {
			lastUpdateTime = data.getInt();
			percentage = data.getInt();
		}
		
		@Override
		public int getLength() {
			return 8;
		}
		
		@Override
		public String toString() {
			return lastUpdateTime + ":" + percentage + "%";
		}
	}
	
	public static class OtherServerGCWZonePercent implements Encodable {
		
		private String zone = "";
		private int percentage = 0;
		
		@Override
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(6 + zone.length());
			data.addAscii(zone);
			data.addInt(percentage);
			return data.array();
		}
		
		@Override
		public void decode(NetBuffer data) {
			zone = data.getAscii();
			percentage = data.getInt();
		}
		
		@Override
		public int getLength() {
			return 6 + zone.length();
		}
		
		@Override
		public String toString() {
			return zone + ":" + percentage + "%";
		}
	}
	
}
