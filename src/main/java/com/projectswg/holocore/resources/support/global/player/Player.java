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
package com.projectswg.holocore.resources.support.global.player;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.network.OutboundPacketIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentChain;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class Player implements Comparable<Player> {
	
	private final long networkId;
	private final IntentChain intentChain;
	
	private String			username			= "";
	private AccessLevel		accessLevel			= AccessLevel.PLAYER;
	private PlayerServer	server				= PlayerServer.NONE;
	private PlayerState		state				= PlayerState.DISCONNECTED;
	private CreatureObject	creatureObject		= null;
	private long			lastInboundMessage	= 0;
	
	public Player() {
		this(0);
	}
	
	public Player(long networkId) {
		this.networkId = networkId;
		this.intentChain = new IntentChain();
	}
	
	public void setPlayerState(PlayerState state) {
		this.state = state;
	}
	
	public void setPlayerServer(PlayerServer server) {
		this.server = server;
	}
	
	public void setUsername(String username) {
		this.username = username;
	}
	
	public void setAccessLevel(AccessLevel accessLevel) {
		this.accessLevel = accessLevel;
	}
	
	public void setCreatureObject(CreatureObject obj) {
		this.creatureObject = obj;
		if (obj != null && obj.getOwner() != this)
			obj.setOwner(this);
	}
	
	public void updateLastPacketTimestamp() {
		lastInboundMessage = System.nanoTime();
	}
	
	public long getNetworkId() {
		return networkId;
	}
	
	public PlayerState getPlayerState() {
		return state;
	}
	
	public PlayerServer getPlayerServer() {
		return server;
	}
	
	public String getUsername() {
		return username;
	}
	
	@NotNull
	public String getCharacterName() {
		if (creatureObject != null)
			return creatureObject.getObjectName();
		return "";
	}
	
	public String getCharacterFirstName() {
		String name = getCharacterName();
		int spaceIndex = name.indexOf(' ');
		if (spaceIndex != -1)
			return name.substring(0, spaceIndex);
		return name;
	}
	
	public String getCharacterChatName() {
		return getCharacterFirstName().toLowerCase(Locale.US);
	}
	
	public AccessLevel getAccessLevel() {
		return accessLevel;
	}
	
	public String getGalaxyName() {
		return ProjectSWG.getGalaxy().getName();
	}
	
	public CreatureObject getCreatureObject() {
		return creatureObject;
	}
	
	public PlayerObject getPlayerObject() {
		if(creatureObject != null){
			SWGObject player = creatureObject.getSlottedObject("ghost");
			if (player instanceof PlayerObject)
				return (PlayerObject) player;			
		}
		return null;
	}
	
	public double getTimeSinceLastPacket() {
		return (System.nanoTime()-lastInboundMessage)/1E6;
	}
	
	public boolean isBaselinesSent(SWGObject obj) {
		CreatureObject creature = this.creatureObject;
		return creature == null || creature.isBaselinesSent(obj);
	}
	
	public void sendPacket(SWGPacket ... packets) {
		for (SWGPacket p : packets) {
			broadcast(new OutboundPacketIntent(this, p));
		}
	}
	
	public void broadcast(Intent intent) {
		CreatureObject creatureObject = this.creatureObject;
		if (creatureObject != null)
			creatureObject.broadcast(intent);
		else
			intentChain.broadcastAfter(intent);
	}
	
	@Override
	public String toString() {
		String str = "Player[";
		str += (creatureObject==null?"null":creatureObject.getObjectId());
		str += " NAME=" + username + " / " + (creatureObject==null?"null":creatureObject.getObjectName());
		str += " STATE=" + state;
		return str + ']';
	}
	
	@Override
	public int compareTo(@NotNull Player p) {
		return Long.compare(networkId, p.getNetworkId());
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Player && ((Player) o).getNetworkId() == getNetworkId();
	}
	
	@Override
	public int hashCode() {
		return Long.hashCode(networkId);
	}
	
	public enum PlayerServer {
		NONE,
		LOGIN,
		ZONE
	}
	
}
