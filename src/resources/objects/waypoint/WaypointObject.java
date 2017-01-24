/***********************************************************************************
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
package resources.objects.waypoint;

import network.packets.Packet;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.Location;
import resources.Terrain;
import resources.encodables.OutOfBandData;
import resources.encodables.OutOfBandPackage;
import resources.network.NetBufferStream;
import resources.objects.intangible.IntangibleObject;
import resources.player.Player;

import java.nio.ByteBuffer;

public class WaypointObject extends IntangibleObject implements OutOfBandData {
	
	private long cellId;
	private String name = "New Waypoint";
	private WaypointColor color = WaypointColor.BLUE;
	private boolean active = true;
	
	public WaypointObject(long objectId) {
		super(objectId, BaselineType.WAYP);
	}

	public String getObjectName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public WaypointColor getColor() {
		return color;
	}

	public void setColor(WaypointColor color) {
		this.color = color;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public long getCellId() {
		return cellId;
	}

	public void setCellId(long cellId) {
		this.cellId = cellId;
	}

	public void createObject(Player target) {
		// NOTE: Client is never sent a WAYP baseline in NGE, WaypointObject's just go inside the Waypoint List in PLAY.
	}

	@Override
	public byte[] encode() {
		Location loc = getLocation();
		ByteBuffer bb = ByteBuffer.allocate(42 + name.length() * 2);
		Packet.addInt(bb, 0);
		Packet.addFloat(bb, (float) loc.getX());
		Packet.addFloat(bb, (float) loc.getY());
		Packet.addFloat(bb, (float) loc.getZ());
		Packet.addLong(bb, cellId);
		Packet.addCrc(bb, loc.getTerrain().getName());
		Packet.addUnicode(bb, name);
		Packet.addLong(bb, getObjectId());
		Packet.addByte(bb, color.getValue());
		Packet.addBoolean(bb, active);
		return bb.array();
	}

	@Override
	public void decode(ByteBuffer data) {
		data.getInt();
		Location loc = new Location(data.getFloat(), data.getFloat(), data.getFloat(), null);
		cellId 		= Packet.getLong(data);
		loc.setTerrain(Terrain.getTerrainFromCrc(data.getInt()));
		name 		= Packet.getUnicode(data);
		Packet.getLong(data); // objectId
		color		= WaypointColor.valueOf(data.get());
		active 		= Packet.getBoolean(data);
		setLocation(loc);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(0);
		stream.addBoolean(active);
		stream.addLong(cellId);
		stream.addAscii(name);
		stream.addAscii(color.name());
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		stream.getByte();
		active = stream.getBoolean();
		cellId = stream.getLong();
		name = stream.getAscii();
		color = WaypointColor.valueOf(stream.getAscii());
	}

	@Override
	public OutOfBandPackage.Type getOobType() {
		return OutOfBandPackage.Type.WAYPOINT;
	}

	@Override
	public int getOobPosition() {
		return -3;
	}

	@Override
	public boolean equals(Object o) {
		if (!super.equals(o))
			return false;
		if (o instanceof WaypointObject) {
			WaypointObject wp = (WaypointObject) o;
			return wp.name.equals(name) && wp.cellId == cellId && wp.color == color && wp.active == active;
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return ((super.hashCode() * 7 + name.hashCode()) * 13 + color.getValue()) * 17 + (int) cellId;
	}

	@Override
	public String toString() {
		return "WaypointObject[" +
				"cellId=" + cellId + ", name='" + name + '\'' + ", color=" + color + ", active=" + active +
				", location=" + getLocation() + "]";
	}

	public enum WaypointColor{
		BLUE(1), GREEN(2), ORANGE(3), YELLOW(4), PURPLE(5), WHITE(6), MULTICOLOR(7);
		
		private int i;
		
		WaypointColor(int i) {
			this.i = i;
		}
		
		public int getValue() { return i; }

		public static WaypointColor valueOf(int colorId) {
			for (WaypointColor color : WaypointColor.values()) {
				if (color.getValue() == colorId)
					return color;
			}
			return WaypointColor.BLUE;
		}

		public static WaypointColor fromString(String string) {
			switch(string) {
				case "blue": return WaypointColor.BLUE;
				case "green": return WaypointColor.GREEN;
				case "orange": return WaypointColor.ORANGE;
				case "yellow": return WaypointColor.YELLOW;
				case "purple": return WaypointColor.PURPLE;
				case "white": return WaypointColor.WHITE;
				case "multicolor": return WaypointColor.MULTICOLOR;
				default: return WaypointColor.BLUE;
			}
		}
	}
}
