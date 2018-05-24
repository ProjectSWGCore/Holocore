package com.projectswg.holocore.services.player;

import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.holocore.intents.NotifyPlayersPacketIntent;
import com.projectswg.holocore.intents.PlayerEventIntent;
import com.projectswg.holocore.intents.network.ConnectionClosedIntent;
import com.projectswg.holocore.intents.network.ConnectionOpenedIntent;
import com.projectswg.holocore.resources.player.Player;
import com.projectswg.holocore.resources.player.Player.PlayerServer;
import com.projectswg.holocore.resources.player.PlayerEvent;
import com.projectswg.holocore.resources.player.PlayerState;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PlayerSessionService extends Service {
	
	private final Map<Long, Player> players;
	
	public PlayerSessionService() {
		this.players = new ConcurrentHashMap<>();
	}
	
	@IntentHandler
	private void handleConnectionOpenedIntent(ConnectionOpenedIntent coi) {
		Player p = coi.getPlayer();
		players.put(p.getNetworkId(), p);
		p.setPlayerState(PlayerState.CONNECTED);
		new PlayerEventIntent(p, PlayerEvent.PE_CONNECTED).broadcast();
	}
	
	@IntentHandler
	private void handleConnectionClosedIntent(ConnectionClosedIntent cci) {
		Player p = cci.getPlayer();
		players.remove(p.getNetworkId());
		p.setPlayerState(PlayerState.DISCONNECTED);
		p.setPlayerServer(PlayerServer.NONE);
		new PlayerEventIntent(p, PlayerEvent.PE_LOGGED_OUT).broadcast();
	}
	
	@IntentHandler
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
	
	private void notifyPlayers(SWGPacket ... packets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null)
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayers(NotifyPlayersPacketIntent.ConditionalNotify conditional, SWGPacket ... packets) {
		if (conditional == null) {
			notifyPlayers(packets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayers(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditionalNotify, SWGPacket ... packets) {
		if (conditionalNotify == null) {
			notifyPlayers(networkIds, packets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			if (p.getCreatureObject() != null && conditionalNotify.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayers(List<Long> networkIds, SWGPacket... packets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			if (p.getCreatureObject() != null)
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayersAtPlanet(NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, SWGPacket ... packets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(terrain, packets);
			return;
		}
		
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayersAtPlanet(Terrain terrain, SWGPacket ... packets) {
		iteratePlayers((key, p) -> {
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayersAtPlanet(List<Long> networkIds, NotifyPlayersPacketIntent.ConditionalNotify conditional, Terrain terrain, SWGPacket ... packets) {
		if (conditional == null) {
			notifyPlayersAtPlanet(networkIds, terrain, packets);
			return;
		}
		
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			assert p != null;
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain && conditional.meetsCondition(p))
				p.sendPacket(packets);
		});
	}
	
	private void notifyPlayersAtPlanet(List<Long> networkIds, Terrain terrain, SWGPacket ... packets) {
		networkIds.forEach(id -> {
			Player p = getPlayerFromNetworkId(id);
			assert p != null;
			if (p.getCreatureObject() != null && p.getCreatureObject().getTerrain() == terrain)
				p.sendPacket(packets);
		});
	}
	
	private Player getPlayerFromNetworkId(long networkId) {
		return players.get(networkId);
	}
	
	private void iteratePlayers(BiConsumer<Long, Player> consumer) {
		players.forEach(consumer);
	}
	
}
