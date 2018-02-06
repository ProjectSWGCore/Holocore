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
package resources.network;

import com.projectswg.common.encoding.Encoder;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;

import resources.objects.SWGObject;
import resources.player.Player;
import resources.player.PlayerState;

public class DeltaBuilder {
	
	private final SWGObject object;
	private final BaselineType type;
	private final int num;
	private final int updateType;
	private final byte[] data;
	
	public DeltaBuilder(SWGObject object, BaselineType type, int num, int updateType, Object change) {
		this.object = object;
		this.type = type;
		this.num = num;
		this.data = (change instanceof byte[] ? (byte[]) change : Encoder.encode(change));
		this.updateType = updateType;
	}
	
	public DeltaBuilder(SWGObject object, BaselineType type, int num, int updateType, Object change, StringType strType) {
		this.object = object;
		this.type = type;
		this.num = num;
		this.data = (change instanceof byte[] ? (byte[]) change : Encoder.encode(change, strType));
		this.updateType = updateType;
	}
	
	public boolean send() {
		DeltasMessage message = getBuiltMessage();
		Player owner = object.getOwner();
		boolean sent = false;
		if (owner != null) {
			switch (owner.getPlayerState()) {
				case ZONED_IN:
					owner.sendPacket(message);
					sent = true;
					break;
				case ZONING_IN:
					owner.addBufferedDelta(message);
					sent = true;
					break;
			}
		}
		if (num == 3 || num == 6) { // Shared Objects
			sent = object.sendObservers(message) > 0 || sent;
		}
		return sent;
	}
	
	public DeltasMessage getBuiltMessage() {
		return new DeltasMessage(object.getObjectId(), type, num, updateType, data);
	}
	
	public byte [] getEncodedData() {
		return data;
	}
	
}
