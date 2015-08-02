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
package intents;

import network.packets.Packet;
import resources.Terrain;
import resources.control.Intent;
import resources.player.Player;

import java.util.List;

public class NotifyPlayersPacketIntent extends Intent {

	public static final String TYPE = "GalaxyWidePacketIntent";
	
	private Packet packet;
	private Terrain terrain;
	private List<Long> networkIds;
	private ConditionalNotify condition;

	public NotifyPlayersPacketIntent(Packet packet, Terrain terrain, ConditionalNotify condition, List<Long> networkIds) {
		super(TYPE);
		this.packet = packet;
		this.terrain = terrain;
		this.condition = condition;
		this.networkIds = networkIds;
	}

	public NotifyPlayersPacketIntent(Packet packet, Terrain terrain, ConditionalNotify condition) {
		this(packet, terrain, condition, null);
	}

	public NotifyPlayersPacketIntent(Packet packet, ConditionalNotify condition, List<Long> networkIds) {
		this(packet, null, condition, networkIds);
	}

	public NotifyPlayersPacketIntent(Packet packet, List<Long> networkIds) {
		this(packet, null, null, networkIds);
	}

	public NotifyPlayersPacketIntent(Packet packet, Terrain terrain) {
		this(packet, terrain, null);
	}

	public NotifyPlayersPacketIntent(Packet p) {
		this(p, null, null, null);
	}

	public Packet getPacket() { return packet; }
	public Terrain getTerrain() { return terrain; }
	public ConditionalNotify getCondition() { return condition; }
	public List<Long> getNetworkIds() { return networkIds; }

	public interface ConditionalNotify {
		boolean meetsCondition(Player player);
	}
}
