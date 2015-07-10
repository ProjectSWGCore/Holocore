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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import network.packets.Packet;
import network.packets.soe.SessionRequest;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.zone.insertion.SelectCharacter;
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.network.GalacticPacketIntent;
import intents.network.InboundPacketIntent;
import intents.player.ZonePlayerSwapIntent;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.Log;

public class PlayerManager extends Manager {
	
	private final Map <Long, Player> players;
	private final LoginService loginService;
	private final ZoneManager zoneService;
	
	public PlayerManager() {
		loginService = new LoginService();
		zoneService = new ZoneManager();
		
		players = new HashMap<Long, Player>();
		
		addChildService(loginService);
		addChildService(zoneService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundPacketIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(NotifyPlayersPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent)
			onInboundPacketIntent((InboundPacketIntent) i);
		else if (i instanceof GalacticPacketIntent)
			onGalacticPacketIntent((GalacticPacketIntent) i);
		else if (i instanceof PlayerEventIntent)
			onPlayerEventIntent((PlayerEventIntent) i);
		else if (i instanceof NotifyPlayersPacketIntent)
			onNotifyPlayersPacketIntent((NotifyPlayersPacketIntent) i);
	}

	public boolean playerExists(String name) {
		return zoneService.characterExistsForName(name);
	}

	public Player getPlayerByCreatureName(String name) {
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null && p.getCharacterName().equalsIgnoreCase(name))
					return p;
			}
		}
		return null;
	}
	
	public Player getPlayerByCreatureFirstName(String name) {
		if (name == null || name.isEmpty())
			return null;
		name = name.trim().toLowerCase(Locale.ENGLISH);
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null) {
					String cName = p.getCharacterName().toLowerCase(Locale.ENGLISH);
					if (cName.equals(name))
						return p;
					int spaceIndex = cName.indexOf(' ');
					if (spaceIndex != -1 && cName.substring(0, spaceIndex).equals(name))
						return p;
				}
			}
		}
		return null;
	}
	
	public long getCharacterIdByName(String name) {
		long id = 0;
		try {
			ResultSet result = loginService.getCharacter(name);
			if (result.next())
				id = result.getLong("id");
			result.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return id;
	}
	
	public void notifyPlayers(Packet... packets) {
		synchronized(players) {
			for (Player p : players.values()) {
				if (p != null && p.getCreatureObject() != null)
					p.sendPacket(packets);
			}
		}
	}

	public void notifyPlayers(NotifyPlayersPacketIntent.ConditionalNotify conditional, Packet... packets) {
		if (conditional == null) {
			notifyPlayers(packets);
			return;
		}

		synchronized (players) {
			for (Player player : players.values()) {
				if (conditional.meetsCondition(player)) {
					player.sendPacket(packets);
				}
			}
		}
	}

	public void notifyPlayers(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditionalNotify, Packet... packets) {
		if (conditionalNotify == null) {
			notifyPlayers(networkIds, packets);
			return;
		}

		synchronized (players) {
			networkIds.forEach(id -> {
				Player p = players.get(id);
				if (p != null && p.getCreatureObject() != null && conditionalNotify.meetsCondition(p))
					p.sendPacket(packets);
			});
		}
	}

	public void notifyPlayers(List<Long> networkIds, Packet... packets) {
		synchronized (players) {
			networkIds.forEach(id -> {
				Player p = players.get(id);
				if (p != null && p.getCreatureObject() != null)
					p.sendPacket(packets);
			});
		}
	}

	public void notifyPlayersAtPlanet(NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, Packet... packets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(terrain, packets);
			return;
		}

		synchronized(players) {
			for (Player p : players.values()) {
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getLocation().getTerrain() == terrain && conditional.meetsCondition(p))
					p.sendPacket(packets);
			}
		}
	}

	public void notifyPlayersAtPlanet(Terrain terrain, Packet... packets) {
		synchronized(players) {
			for (Player p : players.values()) {
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getLocation().getTerrain() == terrain)
					p.sendPacket(packets);
			}
		}
	}

	public void notifyPlayersAtPlanet(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, Packet... packets) {
		if(conditional == null) {
			notifyPlayersAtPlanet(networkIds, terrain, packets);
			return;
		}

		synchronized(players) {
			networkIds.forEach(id -> {
				Player p = players.get(id);
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getLocation().getTerrain() == terrain && conditional.meetsCondition(p))
					p.sendPacket(packets);
			});
		}
	}

	public void notifyPlayersAtPlanet(List<Long> networkIds, Terrain terrain, Packet... packets) {
		synchronized(players) {
			networkIds.forEach(id -> {
				Player p = players.get(id);
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getLocation().getTerrain() == terrain)
					p.sendPacket(packets);
			});
		}
	}

	public Player getPlayerFromNetworkId(long networkId) {
		synchronized (players) {
			return players.get(networkId);
		}
	}
	
	private void removeDuplicatePlayers(Player player, long charId) {
		synchronized (players) {
			Iterator <Player> it = players.values().iterator();
			while (it.hasNext()) {
				Player p = it.next();
				if (p != player && p.getCreatureObject() != null && p.getCreatureObject().getObjectId() == charId) {
					new ZonePlayerSwapIntent(p, player, p.getCreatureObject()).broadcast();
					it.remove();
				}
			}
		}
	}
	
	private Player transitionLoginToZone(long networkId, int galaxyId, String galaxyName, ClientIdMsg clientId) {
		final byte [] nToken = clientId.getSessionToken();
		synchronized (players) {
			for (Player p : players.values()) {
				byte [] pToken = p.getSessionToken();
				if (pToken.length != nToken.length)
					continue;
				boolean match = true;
				for (int t = 0; t < pToken.length && match; t++) {
					if (pToken[t] != nToken[t])
						match = false;
				}
				if (match) {
					players.remove(p.getNetworkId());
					p.setNetworkId(networkId);
					p.setGalaxyId(galaxyId);
					p.setGalaxyName(galaxyName);
					players.put(networkId, p);
					Log.i("PlayerManager", "Transitioned %s from login to zone", p.getUsername());
					return p;
				}
			}
		}
		return null;
	}
	
	private void onPlayerEventIntent(PlayerEventIntent pei) {
		synchronized (players) {
			if (pei.getEvent() == PlayerEvent.PE_DISAPPEAR) {
				Player p = pei.getPlayer();
				if (p.getPlayerState() == PlayerState.DISCONNECTED) {
					players.remove(p.getNetworkId());
				}
			}
		}
	}
	
	private void onInboundPacketIntent(InboundPacketIntent ipi) {
		Player player = getPlayerFromNetworkId(ipi.getNetworkId());
		if (player != null)
			player.updateLastPacketTimestamp();
	}
	
	private void onGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		ServerType type = gpi.getServerType();
		long networkId = gpi.getNetworkId();
		Player player = null;
		if (type == ServerType.ZONE && packet instanceof ClientIdMsg) {
			String galaxyName = gpi.getGalaxy().getName();
			int galaxyId = gpi.getGalaxy().getId();
			player = transitionLoginToZone(networkId, galaxyId, galaxyName, (ClientIdMsg) packet);
		} else
			player = getPlayerFromNetworkId(networkId);
		if (player != null && type == ServerType.ZONE && packet instanceof SelectCharacter)
			removeDuplicatePlayers(player, ((SelectCharacter)packet).getCharacterId());
		if (type == ServerType.LOGIN && player == null) {
			player = new Player(this, networkId);
			synchronized (players) {
				players.put(networkId, player);
			}
		}
		if (player != null) {
			if (type == ServerType.LOGIN)
				loginService.handlePacket(gpi, player, packet);
			else if (type == ServerType.ZONE)
				zoneService.handlePacket(gpi, player, networkId, packet);
		} else if (type == ServerType.ZONE && packet instanceof SessionRequest)
			zoneService.handlePacket(gpi, player, networkId, packet);
	}
	
	private void onNotifyPlayersPacketIntent(NotifyPlayersPacketIntent nppi) {
		if (nppi.getNetworkIds() != null) {
			if (nppi.getTerrain() != null) notifyPlayersAtPlanet(nppi.getNetworkIds(), nppi.getCondition(), nppi.getTerrain(), nppi.getPacket());
			else notifyPlayers(nppi.getNetworkIds(), nppi.getCondition(), nppi.getPacket());
		} else {
			if (nppi.getTerrain() != null) notifyPlayersAtPlanet(nppi.getCondition(), nppi.getTerrain(), nppi.getPacket());
			else notifyPlayers(nppi.getCondition(), nppi.getPacket());
		}
	}
}
