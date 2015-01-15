package services.player;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import network.packets.Packet;
import network.packets.swg.login.ClientIdMsg;
import network.packets.swg.zone.insertion.SelectCharacter;
import intents.CloseConnectionIntent;
import intents.GalacticPacketIntent;
import intents.PlayerEventIntent;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;

public class PlayerManager extends Manager {
	
	private final Map <Long, Player> players;
	private final LoginService loginService;
	private final ZoneService zoneService;
	
	public PlayerManager() {
		loginService = new LoginService();
		zoneService = new ZoneService();
		
		players = new HashMap<Long, Player>();
		
		addChildService(loginService);
		addChildService(zoneService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			Packet packet = gpi.getPacket();
			ServerType type = gpi.getServerType();
			long networkId = gpi.getNetworkId();
			Player player = null;
			if (type == ServerType.ZONE && packet instanceof ClientIdMsg)
				player = transitionLoginToZone(networkId, gpi.getGalaxy().getId(), (ClientIdMsg) packet);
			else
				player = players.get(networkId);
			if (player != null && type == ServerType.ZONE && packet instanceof SelectCharacter)
				removeDuplicatePlayers(player, ((SelectCharacter)packet).getCharacterId());
			if (type == ServerType.LOGIN && player == null) {
				player = new Player(this, networkId);
				players.put(networkId, player);
			}
			if (player != null) {
				player.updateLastPacketTimestamp();
				if (type == ServerType.LOGIN)
					loginService.handlePacket(gpi, player, packet);
				else if (type == ServerType.ZONE)
					zoneService.handlePacket(gpi, player, networkId, packet);
			}
		} else if (i instanceof PlayerEventIntent) {
			if (((PlayerEventIntent)i).getEvent() == PlayerEvent.PE_DISAPPEAR) {
				PlayerEventIntent pei = (PlayerEventIntent) i;
				Player p = pei.getPlayer();
				if (p.getPlayerState() == PlayerState.DISCONNECTED) {
					players.remove(p.getNetworkId());
					new CloseConnectionIntent(p.getNetworkId()).broadcast();
				}
			}
		}
	}
	
	public Player getPlayerByCreatureName(String name) {
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null && p.getCreatureObject().getName().equalsIgnoreCase(name))
					return p;
			}
		}
		return null;
	}
	
	public Player getPlayerByCreatureFirstName(String name) {
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null) {
					String cName = p.getCreatureObject().getName().toLowerCase();
					if (cName.startsWith(name) && (cName.length() == name.length() || cName.charAt(name.length()) == ' ')) {
						return p;
					}
				}
			}
		}
		return null;
	}
	
	public long getCharacterIdByName(String name) {
		try {
			ResultSet result = loginService.getCharacter(name);
			if (result.next()) {
				return result.getLong(0);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return 0;
	}
	
	private void removeDuplicatePlayers(Player player, long charId) {
		synchronized (players) {
			Iterator <Player> it = players.values().iterator();
			while (it.hasNext()) {
				Player p = it.next();
				if (p != player && p.getCreatureObject().getObjectId() == charId)
					it.remove();
			}
		}
	}
	
	private Player transitionLoginToZone(final long networkId, final int galaxyId, ClientIdMsg clientId) {
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
					players.put(networkId, p);
					return p;
				}
			}
		}
		return null;
	}
	
	public Player getPlayerFromNetworkId(long networkId) {
		return players.get(networkId);
	}
	
}
