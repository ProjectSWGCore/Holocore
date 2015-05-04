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

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.deltas.DeltasMessage;
import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Encoder;
import utilities.Encoder.StringType;

public class DeltaBuilder {
	private SWGObject object;
	private BaselineType type;
	private int num;
	private int updateType;
	private byte[] data;

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
	
	public void sendTo(Player target) {
		target.sendPacket(getBuiltMessage());
	}
	
	public void send() {
		DeltasMessage message = getBuiltMessage();
		switch(num) {
		case 3: object.sendObservers(message); break;
		case 6: object.sendObservers(message); break;
		default: object.sendSelf(message); break;
		}
	}

	public DeltasMessage getBuiltMessage() {
		DeltasMessage delta = new DeltasMessage();
		delta.setId(object.getObjectId());
		delta.setType(type);
		delta.setNum(num);
		delta.setUpdate(updateType);
		delta.setData(data);
		return delta;
	}
}
