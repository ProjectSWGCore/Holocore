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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import network.packets.Packet;
import intents.NotifyPlayersPacketIntent;
import intents.PlayerEventIntent;
import intents.network.ConnectionClosedIntent;
import intents.network.ConnectionOpenedIntent;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Manager;
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
		
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(NotifyPlayersPacketIntent.TYPE);
		registerForIntent(ConnectionOpenedIntent.TYPE);
		registerForIntent(ConnectionClosedIntent.TYPE);
	}
	
	@Override
	public boolean terminate() {
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof PlayerEventIntent)
			onPlayerEventIntent((PlayerEventIntent) i);
		else if (i instanceof NotifyPlayersPacketIntent)
			onNotifyPlayersPacketIntent((NotifyPlayersPacketIntent) i);
		else if (i instanceof ConnectionOpenedIntent)
			onConnectionOpenedIntent((ConnectionOpenedIntent) i);
		else if (i instanceof ConnectionClosedIntent)
			onConnectionClosedIntent((ConnectionClosedIntent) i);
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
			Log.e(this, e);
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
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
					p.sendPacket(packets);
			}
		}
	}

	public void notifyPlayersAtPlanet(Terrain terrain, Packet... packets) {
		synchronized(players) {
			for (Player p : players.values()) {
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
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
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
					p.sendPacket(packets);
			});
		}
	}

	public void notifyPlayersAtPlanet(List<Long> networkIds, Terrain terrain, Packet... packets) {
		synchronized(players) {
			networkIds.forEach(id -> {
				Player p = players.get(id);
				if (p != null && p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
					p.sendPacket(packets);
			});
		}
	}

	public Player getPlayerFromNetworkId(long networkId) {
		synchronized (players) {
			return players.get(networkId);
		}
	}
	
	private void onPlayerEventIntent(PlayerEventIntent pei) {
		synchronized (players) {
			if (pei.getEvent() == PlayerEvent.PE_DESTROYED) {
				Player p = pei.getPlayer();
				if (p.getPlayerState() == PlayerState.DISCONNECTED) {
					players.remove(p.getNetworkId());
				}
			}
		}
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
	
	private void onConnectionOpenedIntent(ConnectionOpenedIntent coi) {
		Player p = new Player(this, coi.getNetworkId());
		synchronized (players) {
			players.put(coi.getNetworkId(), p);
		}
		p.setPlayerState(PlayerState.CONNECTED);
		new PlayerEventIntent(p, PlayerEvent.PE_CONNECTED).broadcast();
	}
	
	private void onConnectionClosedIntent(ConnectionClosedIntent cci) {
		Player p;
		synchronized (players) {
			p = players.remove(cci.getNetworkId());
		}
		if (p != null) {
			p.setPlayerState(PlayerState.DISCONNECTED);
			new PlayerEventIntent(p, PlayerEvent.PE_LOGGED_OUT).broadcast();
		}
	}
}
