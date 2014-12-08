package services.player;

import network.packets.Packet;
import network.packets.swg.login.ClientIdMsg;
import intents.GalacticPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import resources.network.ServerType;
import resources.player.Player;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;

public class PlayerManager extends Manager {
	
	private LoginService loginService;
	private ZoneService zoneService;
	
	private ObjectDatabase <Player> players;
	
	public PlayerManager() {
		loginService = new LoginService();
		zoneService = new ZoneService();
		
		players = new ObjectDatabase<Player>("odb/players.db");
		
		addChildService(loginService);
		addChildService(zoneService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		players.loadToCache();
		players.traverse(new Traverser<Player>() {
			@Override
			public void process(Player p) {
				p.setPlayerManager(PlayerManager.this);
			}
		});
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		players.save();
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
				transitionLoginToZone(networkId, gpi.getGalaxy().getId(), (ClientIdMsg) packet);
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
	
	private void transitionLoginToZone(final long networkId, final int galaxyId, ClientIdMsg clientId) {
		final byte [] nToken = clientId.getSessionToken();
		players.traverse(new Traverser<Player>() {
			@Override
			public void process(Player p) {
				byte [] pToken = p.getSessionToken();
				if (pToken.length != nToken.length)
					return;
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
				}
			}
		});
	}
	
	public Player getPlayerFromNetworkId(long networkId) {
		return players.get(networkId);
	}
	
}
