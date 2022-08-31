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
package com.projectswg.holocore.resources.support.objects.swg.mission;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.oob.waypoint.WaypointPackage;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.encoding.Encodable;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;

public class MissionObject extends IntangibleObject {
	
	private int difficulty					= 0;
	private MissionLocation location		= new MissionLocation();
	private String missionCreator			= "";
	private int reward						= 0;
	private MissionLocation startLocation	= new MissionLocation();
	private CRC targetAppearance			= new CRC();
	private StringId description			= new StringId();
	private StringId title					= new StringId();
	private int status						= 0;
	private CRC missionType					= new CRC();
	private String targetName				= "";
	private WaypointObject waypoint			= new WaypointObject(0);
	
	public MissionObject(long objectId) {
		super(objectId, BaselineType.MISO);
	}
	
	@Override
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(difficulty);
		bb.addObject(location);
		bb.addUnicode(missionCreator);
		bb.addInt(reward);
		bb.addObject(startLocation);
		bb.addObject(targetAppearance);
		bb.addObject(description);
		bb.addObject(title);
		bb.addInt(status);
		bb.addObject(missionType);
		bb.addAscii(targetName);
		bb.addObject(waypoint.getOOB());
	}
	
	@Override
	public void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		difficulty = buffer.getInt();
		location = buffer.getEncodable(MissionLocation.class);
		missionCreator = buffer.getUnicode();
		reward = buffer.getInt();
		startLocation = buffer.getEncodable(MissionLocation.class);
		targetAppearance = buffer.getEncodable(CRC.class);
		description = buffer.getEncodable(StringId.class);
		title = buffer.getEncodable(StringId.class);
		status = buffer.getInt();
		missionType = buffer.getEncodable(CRC.class);
		targetName = buffer.getAscii();
		int pos = buffer.position();
		buffer.seek(24);
		buffer.getUnicode();
		waypoint = new WaypointObject(buffer.getLong());
		buffer.position(pos);
		waypoint.setOOB(new WaypointPackage(buffer));
	}
	
	public static class MissionLocation implements Encodable {
		
		private Point3D location;
		private long objectId;
		private Terrain terrain;
		
		public MissionLocation() {
			location = new Point3D();
			objectId = 0;
			terrain = Terrain.TATOOINE;
		}
		
		@Override
		public byte[] encode() {
			NetBuffer data = NetBuffer.allocate(getLength());
			data.addEncodable(location);
			data.addLong(objectId);
			data.addInt(terrain.getCrc());
			return data.array();
		}
		
		@Override
		public void decode(NetBuffer data) {
			location = data.getEncodable(Point3D.class);
			objectId = data.getLong();
			terrain = Terrain.getTerrainFromCrc(data.getInt());
		}
		
		@Override
		public int getLength() {
			return location.getLength() + 12;
		}
		
	}
	
}
