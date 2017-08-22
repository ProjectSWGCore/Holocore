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

import com.projectswg.common.debug.Log;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.SWGPacket;
import network.packets.swg.zone.object_controller.combat.CombatAction;
import network.packets.swg.zone.object_controller.combat.CombatSpam;

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
	
	protected final void decodeHeader(NetBuffer data) {
		if (!super.checkDecode(data, CRC))
			return;
		update = data.getInt();
		if (data.getInt() != controllerCrc)
			Log.e("Attempting to process invalid controller");
		objectId = data.getLong();
		data.getInt();
		return;
	}
	
	protected final void encodeHeader(NetBuffer data) {
		data.addShort(5);
		data.addInt(CRC);
		data.addInt(update);
		data.addInt(controllerCrc);
		data.addLong(objectId);
		data.addInt(0);
	}
	
	@Override
	public abstract void decode(NetBuffer data);
	@Override
	public abstract NetBuffer encode();
	
	public long getObjectId() { return objectId; }
	public int getUpdate() { return update; }
	public int getControllerCrc() { return controllerCrc; }
	
	public void setUpdate(int update) { this.update = update; }
	
	public static final ObjectController decodeController(NetBuffer data) {
		if (data.array().length < 14)
			return null;
		int pos = data.position();
		data.position(pos+10);
		int crc = data.getInt();
		data.position(pos);
		switch (crc) {
			case 0x0071: return new DataTransform(data);
			case 0x00CC: return new CombatAction(data);
			case 0x00F1: return new DataTransformWithParent(data);
			case 0x0115: return new SecureTrade(data);
			case 0x0116: return new CommandQueueEnqueue(data);
			case 0x0117: return new CommandQueueDequeue(data);
			case 0x0126: return new LookAtTarget(data);
			case 0x012E: return new PlayerEmote(data);
			case 0x0131: return new PostureUpdate(data);
			case 0x0134: return new CombatSpam(data);
			case 0x0146: return new ObjectMenuRequest(data);
			case 0x01BD: return new ShowFlyText(data);
			case 0x01BF: return new DraftSlotsQueryResponse(data);
			case 0x01DB: return new BiographyUpdate(data);
			case 0x0448: return new CommandTimer(data);
			case 0x044D: return new ChangeRoleIconChoice(data);
			case 0x04BC: return new ShowLootBox(data);
			case 0x04C5: return new IntendedTarget(data);
			case 0x00F5: return new MissionListRequest(data);
			case 0x041C: return new JTLTerminalSharedMessage(data);
			
		}
		Log.w("Unknown object controller: %08X", crc);
		return new GenericObjectController(crc, data);
	}
	
	private static class GenericObjectController extends ObjectController {
		
		public GenericObjectController(int crc, NetBuffer data) {
			super(0, crc);
			decode(data);
		}
		
		@Override
		public NetBuffer encode() {
			NetBuffer data = NetBuffer.allocate(HEADER_LENGTH);
			encodeHeader(data);
			return data;
		}
		
		@Override
		public void decode(NetBuffer data) {
			decodeHeader(data);
		}
	}
	
}
