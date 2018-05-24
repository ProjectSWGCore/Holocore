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
package com.projectswg.holocore.resources.player;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.holocore.intents.network.OutboundPacketIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentChain;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

public class Player implements Comparable<Player> {
	
	private final List<DeltasMessage> bufferedDeltas;
	private final AtomicBoolean bufferedDeltasSent;
	private final IntentChain packetChain;
	private final Object sendingLock;
	private final long networkId;
	
	private String			username			= "";
	private String			galaxyName			= "";
	private AccessLevel		accessLevel			= AccessLevel.PLAYER;
	private PlayerServer	server				= PlayerServer.NONE;
	private PlayerState		state				= PlayerState.DISCONNECTED;
	private CreatureObject	creatureObject		= null;
	private long			lastInboundMessage	= 0;
	private int				userId				= 0;
	
	public Player() {
		this(0);
	}
	
	public Player(long networkId) {
		this.bufferedDeltas = new ArrayList<>();
		this.bufferedDeltasSent = new AtomicBoolean(false);
		this.packetChain = new IntentChain();
		this.sendingLock = new Object();
		this.networkId = networkId;
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
	
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	public void setAccessLevel(AccessLevel accessLevel) {
		this.accessLevel = accessLevel;
	}
	
	public void setGalaxyName(String galaxyName) {
		this.galaxyName = galaxyName;
	}
	
	public void setCreatureObject(CreatureObject obj) {
		this.creatureObject = obj;
		if (obj != null && obj.getOwner() != this)
			obj.setOwner(this);
		if (obj == null)
			packetChain.reset();
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
	
	public int getUserId() {
		return userId;
	}
	
	public AccessLevel getAccessLevel() {
		return accessLevel;
	}
	
	public String getGalaxyName() {
		return galaxyName;
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
	
	public Object getSendingLock() {
		return sendingLock;
	}
	
	public void addBufferedDelta(DeltasMessage packet) {
		synchronized (bufferedDeltas) {
			if (bufferedDeltasSent.get())
				sendPacket(packet);
			else
				bufferedDeltas.add(packet);
		}
	}
	
	public void sendBufferedDeltas() {
		DeltasMessage [] packets;
		synchronized (bufferedDeltas) {
			bufferedDeltasSent.set(true);
			packets = bufferedDeltas.toArray(new DeltasMessage[0]);
			bufferedDeltas.clear();
		}
		sendPacket(packets);
	}
	
	public void sendPacket(SWGPacket ... packets) {
		synchronized (getSendingLock()) {
			for (SWGPacket p : packets) {
				packetChain.broadcastAfter(new OutboundPacketIntent(this, p));
			}
		}
	}
	
	@Override
	public String toString() {
		String str = "Player[";
		str += "ID=" + userId + " / " + (creatureObject==null?"null":creatureObject.getObjectId());
		str += " NAME=" + username + " / " + (creatureObject==null?"null":creatureObject.getObjectName());
		str += " STATE=" + state;
		return str + "]";
	}
	
	@Override
	public int compareTo(@NotNull Player p) {
		if (creatureObject == null)
			return p.getCreatureObject() == null ? 0 : -1;
		else if (p.getCreatureObject() == null)
			return 1;
		return creatureObject.compareTo(p.getCreatureObject());
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof Player))
			return false;
		if (this == o)
			return true;
		if (creatureObject == null)
			return false;
		CreatureObject oCreature = ((Player) o).getCreatureObject();
		return creatureObject.equals(oCreature);
	}
	
	@Override
	public int hashCode() {
		if (creatureObject == null)
			return getUserId();
		return Long.valueOf(creatureObject.getObjectId()).hashCode() ^ getUserId();
	}
	
	public enum PlayerServer {
		NONE,
		LOGIN,
		ZONE
	}
	
}
