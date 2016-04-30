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
package network.packets.swg.zone.object_controller;

import java.nio.ByteBuffer;

import resources.server_info.Log;
import network.packets.swg.SWGPacket;

public abstract class ObjectController extends SWGPacket {
	
	public static final int CRC = 0x80CE5E46;
	protected static final int HEADER_LENGTH = 26;
	
	private final int controllerCrc;
	private int update = 0;
	private long objectId = 0;
	
	public ObjectController() {
		this(0, 0);
	}
	
	public ObjectController(int controllerCrc) {
		this(0, controllerCrc);
	}
	
	public ObjectController(long objectId, int controllerCrc) {
		this.objectId = objectId;
		this.controllerCrc = controllerCrc;
		this.update = 0x1B;
	}
	
	protected final void decodeHeader(ByteBuffer data) {
		if (!super.decode(data, CRC))
			return;
		update = getInt(data);
		if (getInt(data) != controllerCrc)
			System.err.println("ObjectController[" + getClass().getSimpleName() + "] Attempting to process invalid controller");
		objectId = getLong(data);
		getInt(data);
		return;
	}
	
	protected final void encodeHeader(ByteBuffer data) {
		addShort(data, 5);
		addInt(  data, CRC);
		addInt  (data, update);
		addInt(  data, controllerCrc);
		addLong( data, objectId);
		addInt(  data, 0);
	}
	
	public abstract void decode(ByteBuffer data);
	public abstract ByteBuffer encode();
	
	public long getObjectId() { return objectId; }
	public int getUpdate() { return update; }
	public int getControllerCrc() { return controllerCrc; }
	
	public void setUpdate(int update) { this.update = update; }
	
	public static final ObjectController decodeController(ByteBuffer data) {
		if (data.array().length < 14)
			return null;
		int crc = data.getInt(10);
		switch (crc) {
			case 0x0071: return new DataTransform(data);
			case 0x00F1: return new DataTransformWithParent(data);
			case 0x0116: return new CommandQueueEnqueue(data);
			case 0x0117: return new CommandQueueDequeue(data);
			case 0x0126: return new LookAtTarget(data);
			case 0x012E: return new PlayerEmote(data);
			case 0x0131: return new PostureUpdate(data);
			case 0x0146: return new ObjectMenuRequest(data);
			case 0x04C5: return new IntendedTarget(data);
			case ChangeRoleIconChoice.CRC: return new ChangeRoleIconChoice(data);
		}
		Log.w("ObjectController", "Unknown object controller: %08X", crc);
		return new GenericObjectController(crc, data);
	}
	
	private static class GenericObjectController extends ObjectController {
		
		public GenericObjectController(int crc, ByteBuffer data) {
			super(0, crc);
			decode(data);
		}
		
		@Override
		public ByteBuffer encode() {
			ByteBuffer data = ByteBuffer.allocate(HEADER_LENGTH);
			encodeHeader(data);
			return data;
		}
		
		@Override
		public void decode(ByteBuffer data) {
			decodeHeader(data);
		}
	}
	
}
