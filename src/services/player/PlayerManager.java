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
package services.player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;

import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import network.packets.Packet;
import resources.Terrain;
import resources.control.Assert;
import resources.control.Manager;
import resources.player.Player;
import resources.player.Player.PlayerServer;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.SynchronizedMap;
import services.CoreManager;

public class PlayerManager extends Manager {
	
	private final Map<Long, Player> players;
	private final LoginService loginService;
	private final ZoneManager zoneService;
	
	public PlayerManager() {
		players = new SynchronizedMap<>();
		loginService = new LoginService();
		zoneService = new ZoneManager();
		
		addChildService(loginService);
		addChildService(zoneService);
		
		registerForIntent(NotifyPlayersPacketIntent.class, nppi -> handleNotifyPlayersPacketIntent(nppi));
		registerForIntent(ConnectionOpenedIntent.class, coi -> handleConnectionOpenedIntent(coi));
		registerForIntent(ConnectionClosedIntent.class, cci -> handleConnectionClosedIntent(cci));
	}

	public boolean playerExists(String name) {
		return zoneService.characterExistsForName(name);
	}
	
	public Player getPlayerByCreatureName(String name) {
		Assert.notNull(name);
		Assert.test(!name.trim().isEmpty());
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null && p.getCharacterName().equalsIgnoreCase(name))
					return p;
			}
		}
		return null;
	}
	
	public Player getPlayerByCreatureFirstName(String name) {
		Assert.notNull(name);
		name = name.trim().toLowerCase(Locale.ENGLISH);
		Assert.test(!name.isEmpty());
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() == null)
					continue;
				String cName = p.getCharacterName().toLowerCase(Locale.ENGLISH);
				if (cName.equals(name))
					return p;
				int spaceIndex = cName.indexOf(' ');
				if (spaceIndex != -1 && cName.substring(0, spaceIndex).equals(name))
					return p;
			}
		}
		return null;
	}
	
	public long getCharacterIdByName(String name) {
		Assert.notNull(name);
		Assert.test(!name.trim().isEmpty());
		return loginService.getCharacterId(name);
	}
	
	public void notifyPlayers(Packet ... packets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null)
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayers(NotifyPlayersPacketIntent.ConditionalNotify conditional, Packet ... packets) {
		if (conditional == null) {
			notifyPlayers(packets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayers(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditionalNotify, Packet ... packets) {
		if (conditionalNotify == null) {
			notifyPlayers(networkIds, packets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && conditionalNotify.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayers(List<Long> networkIds, Packet ... packets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null)
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayersAtPlanet(NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, Packet ... packets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(terrain, packets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayersAtPlanet(Terrain terrain, Packet ... packets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayersAtPlanet(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, Packet ... packets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(networkIds, terrain, packets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	public void notifyPlayersAtPlanet(List<Long> networkIds, Terrain terrain, Packet ... packets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(packets);
		});
	}
	
	public Player getPlayerFromNetworkId(long networkId) {
		return players.get(networkId);
	}
	
	private void handleNotifyPlayersPacketIntent(NotifyPlayersPacketIntent nppi) {
		if (nppi.getNetworkIds() != null) {
			if (nppi.getTerrain() != null)
				notifyPlayersAtPlanet(nppi.getNetworkIds(), nppi.getCondition(), nppi.getTerrain(), nppi.getPacket());
			else
				notifyPlayers(nppi.getNetworkIds(), nppi.getCondition(), nppi.getPacket());
		} else {
			if (nppi.getTerrain() != null)
				notifyPlayersAtPlanet(nppi.getCondition(), nppi.getTerrain(), nppi.getPacket());
			else
				notifyPlayers(nppi.getCondition(), nppi.getPacket());
		}
	}
	
	private void iteratePlayers(BiConsumer<Long, Player> consumer) {
		synchronized (players) {
			players.forEach(consumer);
		}
	}
	
	private void handleConnectionOpenedIntent(ConnectionOpenedIntent coi) {
		Player p = new Player(this, coi.getNetworkId());
		p.setGalaxyName(CoreManager.getGalaxy().getName());
		Assert.isNull(players.put(coi.getNetworkId(), p));
		p.setPlayerState(PlayerState.CONNECTED);
		new PlayerEventIntent(p, PlayerEvent.PE_CONNECTED).broadcast();
	}
	
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci) {
		Player p = players.remove(cci.getNetworkId());
		Assert.notNull(p);
		p.setPlayerState(PlayerState.DISCONNECTED);
		p.setPlayerServer(PlayerServer.NONE);
		new PlayerEventIntent(p, PlayerEvent.PE_LOGGED_OUT).broadcast();
	}
}
