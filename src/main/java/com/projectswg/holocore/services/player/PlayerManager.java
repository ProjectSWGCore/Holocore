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
package com.projectswg.holocore.services.player;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.holocore.intents.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.network.ConnectionOpenedIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.Player.PlayerServer;
import com.projectswg.holocore.resources.player.PlayerEvent;
import com.projectswg.holocore.resources.player.PlayerState;
import com.projectswg.holocore.services.CoreManager;
import me.joshlarson.jlcommon.control.Manager;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class PlayerManager extends Manager {
	
	private final Map<Long, Player> players;
	private final Map<String, CreatureObject> charactersByFullName;
	private final Map<String, CreatureObject> charactersByFirstName;
	private final LoginService loginService;
	private final ZoneManager zoneService;
	
	public PlayerManager() {
		this.players = new ConcurrentHashMap<>();
		this.charactersByFullName = new ConcurrentHashMap<>();
		this.charactersByFirstName = new ConcurrentHashMap<>();
		this.loginService = new LoginService();
		this.zoneService = new ZoneManager();
		
		addChildService(loginService);
		addChildService(zoneService);
		
		registerForIntent(NotifyPlayersPacketIntent.class, this::handleNotifyPlayersPacketIntent);
		registerForIntent(ConnectionOpenedIntent.class, this::handleConnectionOpenedIntent);
		registerForIntent(ConnectionClosedIntent.class, this::handleConnectionClosedIntent);
		registerForIntent(ObjectCreatedIntent.class, this::handleObjectCreatedIntent);
		registerForIntent(DestroyObjectIntent.class, this::handleDestroyObjectIntent);
	}
	
	@Override
	public boolean initialize() {
		PlayerLookup.setPlayerManager(this);
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		PlayerLookup.setPlayerManager(null);
		return super.terminate();
	}
	
	public boolean playerExists(String name) {
		return zoneService.characterExistsForName(name);
	}
	
	public Player getPlayerByFullName(String name) {
		CreatureObject creature = getCharacterByFullName(name);
		if (creature == null)
			return null;
		return creature.getOwner();
	}
	
	public Player getPlayerByFirstName(String name) {
		CreatureObject creature = getCharacterByFirstName(name);
		if (creature == null)
			return null;
		return creature.getOwner();
	}
	
	public CreatureObject getCharacterByFullName(String name) {
		Assert.notNull(name);
		name = name.trim().toLowerCase(Locale.ENGLISH);
		Assert.test(!name.isEmpty());
		return charactersByFullName.get(name);
	}
	
	public CreatureObject getCharacterByFirstName(String name) {
		Assert.notNull(name);
		name = name.trim().toLowerCase(Locale.ENGLISH);
		Assert.test(!name.isEmpty());
		return charactersByFirstName.get(name);
	}
	
	public void notifyPlayers(SWGPacket ... SWGPackets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null)
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayers(NotifyPlayersPacketIntent.ConditionalNotify conditional, SWGPacket ... SWGPackets) {
		if (conditional == null) {
			notifyPlayers(SWGPackets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (conditional.meetsCondition(p))
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayers(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditionalNotify, SWGPacket ... SWGPackets) {
		if (conditionalNotify == null) {
			notifyPlayers(networkIds, SWGPackets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && conditionalNotify.meetsCondition(p))
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayers(List<Long> networkIds, SWGPacket ... SWGPackets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null)
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayersAtPlanet(NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, SWGPacket ... SWGPackets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(terrain, SWGPackets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayersAtPlanet(Terrain terrain, SWGPacket ... SWGPackets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayersAtPlanet(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, SWGPacket ... SWGPackets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(networkIds, terrain, SWGPackets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(SWGPackets);
		});
	}
	
	public void notifyPlayersAtPlanet(List<Long> networkIds, Terrain terrain, SWGPacket ... SWGPackets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			Assert.notNull(p);
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(SWGPackets);
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
		players.forEach(consumer);
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
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		if (!(obj instanceof CreatureObject))
			return;
		
		CreatureObject creature = (CreatureObject) obj;
		if (!creature.isPlayer())
			return;
		
		String name = creature.getObjectName();
		if (name.indexOf(' ') != -1)
			name = name.substring(0, name.indexOf(' '));
		name = name.toLowerCase(Locale.US);
		charactersByFirstName.put(name, creature);
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		if (!(obj instanceof CreatureObject))
			return;
		
		CreatureObject creature = (CreatureObject) obj;
		if (!creature.isPlayer())
			return;
		
		String name = creature.getObjectName();
		if (name.indexOf(' ') != -1)
			name = name.substring(0, name.indexOf(' '));
		name = name.toLowerCase(Locale.US);
		charactersByFirstName.remove(name);
	}
	
	public static class PlayerLookup {
		
		private static final AtomicReference<PlayerManager> PLAYER_MANAGER = new AtomicReference<>(null);
		
		private static void setPlayerManager(PlayerManager playerManager) {
			PLAYER_MANAGER.set(playerManager);
		}
		
		public static Player getPlayerByFullName(String name) {
			return PLAYER_MANAGER.get().getPlayerByFullName(name);
		}
		
		public static Player getPlayerByFirstName(String name) {
			return PLAYER_MANAGER.get().getPlayerByFirstName(name);
		}
		
		public static CreatureObject getCharacterByFullName(String name) {
			return PLAYER_MANAGER.get().getCharacterByFullName(name);
		}
		
		public static CreatureObject getCharacterByFirstName(String name) {
			return PLAYER_MANAGER.get().getCharacterByFirstName(name);
		}
		
	}
}
