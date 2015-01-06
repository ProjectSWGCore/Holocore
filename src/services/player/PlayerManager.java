package services.player;

import java.util.HashMap;
import java.util.Map;

import network.packets.Packet;
import network.packets.swg.login.ClientIdMsg;
import intents.GalacticPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.player.Player;

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
			if (player == null && type == ServerType.LOGIN) {
				player = new Player(this, networkId);
				players.put(networkId, player);
			}
			if (player != null) {
				if (type == ServerType.LOGIN)
					loginService.handlePacket(player, packet);
				else if (type == ServerType.ZONE)
					zoneService.handlePacket(gpi, player, networkId, packet);
			}
		}
	}
	
	public Player getPlayerByCreatureName(String name) {
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null && p.getCreatureObject().getName().equals(name))
					return p;
			}
		}
		return null;
	}
	
	public Player getPlayerByCreatureFirstName(String name) {
		synchronized (players) {
			for (Player p : players.values()) {
				if (p.getCreatureObject() != null) {
					String cName = p.getCreatureObject().getName();
					if (cName.startsWith(name) && (cName.length() == name.length() || cName.charAt(name.length()) == ' ')) {
						return p;
					}
				}
			}
		}
		return null;
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
