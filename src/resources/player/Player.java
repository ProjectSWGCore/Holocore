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
package resources.player;

import intents.network.OutboundPacketIntent;
import network.packets.Packet;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import services.player.PlayerManager;
import utilities.IntentChain;

public class Player implements Comparable<Player> {
	
	private final IntentChain packetChain;
	private Service playerManager;
	
	private long networkId;
	private volatile PlayerState state		= PlayerState.DISCONNECTED;
	
	private String username			= "";
	private int userId				= 0;
	private int connectionId		= 0;
	private AccessLevel accessLevel	= AccessLevel.PLAYER;
	private PlayerServer server		= PlayerServer.NONE;
	
	private String galaxyName		= "";
	private CreatureObject creatureObject= null;
	private long lastInboundMessage	= 0;
	
	public Player() {
		this(null, 0);
	}
	
	public Player(Service playerManager, long networkId) {
		this.packetChain = new IntentChain();
		this.playerManager = playerManager;
		setNetworkId(networkId);
	}

	public PlayerManager getPlayerManager() {
		return (PlayerManager) playerManager;
	}

	public void setPlayerManager(Service playerManager) {
		this.playerManager = playerManager;
	}

	public void setNetworkId(long networkId) {
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
	
	public void setConnectionId(int connId) {
		this.connectionId = connId;
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
			return creatureObject.getName();
		return "";
	}
	
	public int getUserId() {
		return userId;
	}
	
	public int getConnectionId() {
		return connectionId;
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
	
	public void sendPacket(Packet ... packets) {
		for (Packet p : packets) {
			packetChain.broadcastAfter(new OutboundPacketIntent(p, networkId));
		}
	}
	
	@Override
	public String toString() {
		String str = "Player[";
		str += "ID=" + userId + " / " + (creatureObject==null?"null":creatureObject.getObjectId());
		str += " NAME=" + username + " / " + (creatureObject==null?"null":creatureObject.getName());
		str += " STATE=" + state;
		return str + "]";
	}
	
	@Override
	public int compareTo(Player p) {
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
		if (oCreature == null)
			return false;
		return creatureObject.equals(oCreature);
	}
	
	@Override
	public int hashCode() {
		if (creatureObject == null)
			return getUserId();
		return Long.valueOf(creatureObject.getObjectId()).hashCode() ^ getUserId();
	}
	
	public static enum PlayerServer {
		NONE,
		LOGIN,
		ZONE
	}
	
}
