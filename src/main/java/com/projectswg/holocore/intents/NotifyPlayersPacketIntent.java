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
package com.projectswg.holocore.intents;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.holocore.resources.player.Player;
import me.joshlarson.jlcommon.control.Intent;

import java.util.List;

public class NotifyPlayersPacketIntent extends Intent {
	
	private SWGPacket packet;
	private Terrain terrain;
	private List<Long> networkIds;
	private ConditionalNotify condition;
	
	public NotifyPlayersPacketIntent(SWGPacket packet, Terrain terrain, ConditionalNotify condition, List<Long> networkIds) {
		this.packet = packet;
		this.terrain = terrain;
		this.condition = condition;
		this.networkIds = networkIds;
	}
	
	public NotifyPlayersPacketIntent(SWGPacket packet, Terrain terrain, ConditionalNotify condition) {
		this(packet, terrain, condition, null);
	}
	
	public NotifyPlayersPacketIntent(SWGPacket packet, ConditionalNotify condition, List<Long> networkIds) {
		this(packet, null, condition, networkIds);
	}
	
	public NotifyPlayersPacketIntent(SWGPacket packet, List<Long> networkIds) {
		this(packet, null, null, networkIds);
	}
	
	public NotifyPlayersPacketIntent(SWGPacket packet, Terrain terrain) {
		this(packet, terrain, null);
	}
	
	public NotifyPlayersPacketIntent(SWGPacket p) {
		this(p, null, null, null);
	}
	
	public SWGPacket getPacket() {
		return packet;
	}
	
	public Terrain getTerrain() {
		return terrain;
	}
	
	public ConditionalNotify getCondition() {
		return condition;
	}
	
	public List<Long> getNetworkIds() {
		return networkIds;
	}
	
	public interface ConditionalNotify {
		boolean meetsCondition(Player player);
	}
}
