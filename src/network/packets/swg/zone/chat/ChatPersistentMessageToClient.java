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
package network.packets.swg.zone.chat;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import resources.Terrain;
import resources.network.BaselineBuilder.Encodable;
import network.packets.swg.SWGPacket;

public class ChatPersistentMessageToClient extends SWGPacket {
	
	public static final int CRC = 0x08485E17;
	
	private String senderName;
	private String galaxy;
	private int mailId;
	private boolean header;
	private String subject;
	private String message;
	private List <WaypointAttachment> waypointAttachments;
	private List <ProseAttachment> proseAttachments;
	private byte status;
	private int timestamp;
	
	public ChatPersistentMessageToClient(boolean header, String sender, String galaxy, int mailId, String subject, String message, int timestamp, byte status) {
		waypointAttachments = new ArrayList<WaypointAttachment>();
		proseAttachments = new ArrayList<ProseAttachment>();
		this.senderName = sender;
		this.galaxy = galaxy;
		this.mailId = mailId;
		this.header = header;
		this.subject = subject;
		this.message = message;
		this.status = status;
		this.timestamp = timestamp;
	}
	
	public ChatPersistentMessageToClient(Encodable mailEncodable) {
		
	}
	public ChatPersistentMessageToClient(ByteBuffer data) {
		decode(data);
	}
	
	public void decode(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		senderName = getAscii(data);
		getAscii(data); // SWG
		galaxy = getAscii(data);
		mailId = getInt(data);
		header = getBoolean(data);
		
		if (header) {
			getInt(data); // 0?
			subject = getUnicode(data);
			getInt(data);
		} else {
			message = getUnicode(data);
			subject = getUnicode(data);
			int size = getInt(data) * 2;
			int start = data.position();
			do {
				getShort(data); // 0?
				byte type = getByte(data);
				getInt(data); // Integer version of the type, it seems
				if (type == 4)
					waypointAttachments.add(WaypointAttachment.deserialize(data));
				else if (type == 1)
					proseAttachments.add(ProseAttachment.deserialize(data));
				else {
					System.err.println("Invalid type supplied in ChatPersistentMessageToClient.decode");
					break;
				}
			} while (data.position() - start < size);
		}
		status = getByte(data);
		timestamp = getInt(data);
	}
	
	public ByteBuffer encode() {
		int extraSize = 0;
		if (header) {
			extraSize += 12 + subject.length()*2;
		} else {
			extraSize += 12 + message.length()*2 + subject.length()*2;
			for (WaypointAttachment waypoint : waypointAttachments)
				extraSize += 7 + waypoint.getSerializableSize();
			for (ProseAttachment prose : proseAttachments)
				extraSize += 7 + prose.getSerializableSize();
		}
		ByteBuffer data = ByteBuffer.allocate(25 + extraSize + galaxy.length() + senderName.length());
		addShort(data, 2);
		addInt(  data, CRC);
		addAscii(data, senderName);
		addAscii(data, "SWG");
		addAscii(data, galaxy);
		addInt(data, mailId);
		addBoolean(data, header);
		
		if (header) {
			addInt(data, 0); // message (empty as 1 is sending only the header to the client)
			addUnicode(data, subject);
			addInt(data, 0);
		} else {
			addUnicode(data, message);
			addUnicode(data, subject);
			addInt(data, 0); // TODO: Mail Attachments
			/*for (WaypointAttachment waypoint : waypointAttachments) {
				addShort(data, 0);
				addByte(data, 4);
				addInt(data, 0xFFFFFFFD);
				waypoint.serialize(data);
			}
			for (ProseAttachment prose : proseAttachments) {
				addShort(data, 0);
				addByte(data, 1);
				addInt(data, 0xFFFFFFFF);
				prose.serialize(data);
			}*/
		}
		addByte(data, status);
		addInt(data, timestamp);
		return data;
	}
	
	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}
	
	public void setGalaxy(String galaxy) {
		this.galaxy = galaxy;
	}
	
	public void setMailId(int mailId) {
		this.mailId = mailId;
	}
	
	public void setHeader(boolean header) {
		this.header = header;
	}
	
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public void addWaypoint(WaypointAttachment waypoint) {
		waypointAttachments.add(waypoint);
	}
	
	public void addAllWaypoints(Collection <WaypointAttachment> waypoints) {
		waypointAttachments.addAll(waypoints);
	}
	
	public void addProse(ProseAttachment prose) {
		proseAttachments.add(prose);
	}
	
	public void addAllProses(Collection <ProseAttachment> proses) {
		proseAttachments.addAll(proses);
	}
	
	public void setStatus(byte status) {
		this.status = status;
	}
	
	public void setTimestamp(int timestamp) {
		this.timestamp = timestamp;
	}
	
	public static class WaypointAttachment {
		private float x;
		private float y;
		private float z;
		private Terrain planet;
		private String attachmentName;
		private long cellId;
		private byte color;
		private boolean active;
		
		public float getX() { return x; }
		public float getY() { return y; }
		public float getZ() { return z; }
		public Terrain getPlanet() { return planet; }
		public String getAttachmentName() { return attachmentName; }
		public long getCellId() { return cellId; }
		public byte getColor() { return color; }
		public boolean isActive() { return active; }
		
		public void setX(float x) { this.x = x; }
		public void setY(float y) { this.y = y; }
		public void setZ(float z) { this.z = z; }
		public void setPlanet(Terrain p) { this.planet = p; }
		public void setAttachmentName(String name) { this.attachmentName = name; }
		public void setCellId(long id) { this.cellId = id; }
		public void setColor(byte c) { this.color = c; }
		public void setActive(boolean active) { this.active = active; }
		
		private final int getSerializableSize() {
			return 43 + attachmentName.length()*2;
		}
		
		private static final WaypointAttachment deserialize(ByteBuffer data) {
			WaypointAttachment waypoint = new WaypointAttachment();
			getInt(data); // 0?
			waypoint.setX(getFloat(data));
			waypoint.setY(getFloat(data));
			waypoint.setZ(getFloat(data));
			getLong(data); // 0?
			waypoint.setPlanet(Terrain.getTerrainFromCrc(getInt(data)));
			waypoint.setAttachmentName(getUnicode(data));
			waypoint.setCellId(getLong(data));
			waypoint.setColor(getByte(data));
			waypoint.setActive(getBoolean(data));
			getByte(data); // 0?
			return waypoint;
		}
		
		public final void serialize(ByteBuffer data) {
			addInt    (data, 0);
			addFloat  (data, getX());
			addFloat  (data, getY());
			addFloat  (data, getZ());
			addLong   (data, 0);
			addInt    (data, planet.getCrc());
			addUnicode(data, attachmentName);
			addLong   (data, cellId);
			addByte   (data, color);
			addBoolean(data, active);
			addByte   (data, 0);
		}
	}
	
	public static class ProseAttachment {
		private String stf;
		private ProseSegment tu;
		private ProseSegment tt;
		private ProseSegment to;
		private int diInt;
		private float dfFloat;
		
		public String getSTF() { return stf; }
		public ProseSegment getTu() { return tu; }
		public ProseSegment getTt() { return tt; }
		public ProseSegment getTo() { return to; }
		public int getDiInteger() { return diInt; }
		public float getDfFloat() { return dfFloat; }
		
		public void setSTF(String stf) { this.stf = stf; }
		public void setTu(ProseSegment tu) { this.tu = tu; }
		public void setTt(ProseSegment tt) { this.tt = tt; }
		public void setTo(ProseSegment to) { this.to = to; }
		public void setDiInteger(int di) { this.diInt = di; }
		public void setDfFloat(float df) { this.dfFloat = df; }
		
		private final int getSerializableSize() {
			return 10 + stf.length() + tu.getSerializableSize() + tt.getSerializableSize() + to.getSerializableSize();
		}
		
		private static final ProseAttachment deserialize(ByteBuffer data) {
			ProseAttachment prose = new ProseAttachment();
			prose.setSTF(getAscii(data));
			prose.setTu(ProseSegment.deserialize(data));
			prose.setTt(ProseSegment.deserialize(data));
			prose.setTo(ProseSegment.deserialize(data));
			prose.setDiInteger(getInt(data));
			prose.setDfFloat(getFloat(data));
			return prose;
		}
		
		public final void serialize(ByteBuffer data) {
			addAscii(data, stf);
			tu.serialize(data);
			tt.serialize(data);
			to.serialize(data);
			addInt(data, diInt);
			addFloat(data, dfFloat);
		}
		
		public static class ProseSegment {
			private long objectId;
			private String stf;
			private String customString;
			
			public long getObjectId() { return objectId; }
			public String getSTF() { return stf; }
			public String getCustomString() { return customString; }
			
			public void setObjectId(long id) { this.objectId = id; }
			public void setSTF(String stf) { this.stf = stf; }
			public void setCustomString(String custom) { this.customString = custom; }
			
			private final int getSerializableSize() {
				return 14 + stf.length() + customString.length()*2;
			}
			
			private static final ProseSegment deserialize(ByteBuffer data) {
				ProseSegment segment = new ProseSegment();
				segment.setObjectId(getLong(data));
				segment.setSTF(getAscii(data));
				segment.setCustomString(getUnicode(data));
				return segment;
			}
			
			private final void serialize(ByteBuffer data) {
				addLong(data, objectId);
				addAscii(data, stf);
				addUnicode(data, customString);
			}
		}
	}
	/*
	 * ASCII	Stf
	 * LONG		TuObjectId
	 * ASCII	TuStf
	 * UNICODE	TuCustomString
	 * LONG		TtObjectId
	 * ASCII	TtStf
	 * UNICODE	TtCustomString
	 * LONG		ToObjectId
	 * ASCII	ToStf
	 * UNICODE	ToCustomString
	 * INT		DiInteger
	 * FLOAT	DfFloat
	 * BYTE		0
	 */
	
}
