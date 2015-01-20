package services.galaxy;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.HeartBeatMessage;
import intents.GalacticPacketIntent;
import intents.PlayerEventIntent;
import resources.control.Intent;
import resources.control.Service;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;

public class ConnectionService extends Service {
	
	private static final double LD_THRESHOLD = TimeUnit.SECONDS.toMillis(30);
	private static final double DISAPPEAR_THRESHOLD = TimeUnit.MINUTES.toMillis(3);
	
	private final ScheduledExecutorService updateService;
	private final Runnable updateRunnable;
	private final List <Player> zonedInPlayers;
	
	public ConnectionService() {
		updateService = Executors.newSingleThreadScheduledExecutor();
		zonedInPlayers = new LinkedList<Player>();
		updateRunnable = new Runnable() {
			public void run() {
				synchronized (zonedInPlayers) {
					for (Player p : zonedInPlayers) {
						if (p.getTimeSinceLastPacket() > DISAPPEAR_THRESHOLD) {
							p.setPlayerState(PlayerState.DISCONNECTED);
							System.out.println("[" + p.getUsername() +"]" + p.getCharacterName() + " disappeared");
							new PlayerEventIntent(p, PlayerEvent.PE_DISAPPEAR).broadcast();
						} else if (p.getTimeSinceLastPacket() > LD_THRESHOLD) {
							if (p.getPlayerState() != PlayerState.LOGGED_OUT)
								System.out.println("[" + p.getUsername() +"]" + "Logged out " + p.getCharacterName());
							p.setPlayerState(PlayerState.LOGGED_OUT);
						}
					}
				}
			}
		};
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		updateService.scheduleAtFixedRate(updateRunnable, 10, 10, TimeUnit.SECONDS);
		return super.start();
	}
	
	@Override
	public boolean terminate() {
		updateService.shutdownNow();
		boolean success = false;
		try {
			success = updateService.awaitTermination(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		return super.terminate() && success;
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof PlayerEventIntent) {
			if (((PlayerEventIntent)i).getEvent() == PlayerEvent.PE_ZONE_IN) {
				Player p = ((PlayerEventIntent)i).getPlayer();
				synchronized (zonedInPlayers) {
					if (zonedInPlayers.contains(p))
						zonedInPlayers.remove(p);
					zonedInPlayers.add(p);
				}
			} else if (((PlayerEventIntent)i).getEvent() == PlayerEvent.PE_DISAPPEAR) {
				synchronized (zonedInPlayers) {
					zonedInPlayers.remove(((PlayerEventIntent)i).getPlayer());
				}
			}
		} else if (i instanceof GalacticPacketIntent) {
			if (((GalacticPacketIntent)i).getPacket() instanceof HeartBeatMessage) {
				GalacticPacketIntent gpi = (GalacticPacketIntent) i;
				Player p = gpi.getPlayerManager().getPlayerFromNetworkId(gpi.getNetworkId());
				if (p != null)
					p.sendPacket(gpi.getPacket());
			}
		}
	}
	
}
