package services.galaxy;

import intents.GalacticIntent;
import intents.GalacticPacketIntent;
import intents.InboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import services.player.PlayerManager;

public class GalacticManager extends Manager {
	
	private final Object prevPacketIntentMutex = new Object();
	
	private PlayerManager playerManager;
	private Intent prevPacketIntent;
	
	public GalacticManager() {
		playerManager = new PlayerManager();
		prevPacketIntent = null;
		
		addChildService(playerManager);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(InboundPacketIntent.TYPE);
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof InboundPacketIntent) {
			synchronized (prevPacketIntentMutex) {
				GalacticPacketIntent g = new GalacticPacketIntent((InboundPacketIntent) i);
				if (prevPacketIntent == null)
					broadcastGalacticIntent(g);
				else
					broadcastGalacticIntentAfterIntent(g, i);
				prevPacketIntent = g;
			}
		}
	}
	
	public void broadcastGalacticIntent(GalacticIntent i) {
		synchronized (i) {
			if (i.isBroadcasted())
				return;
			i.setPlayerManager(playerManager);
			i.broadcast();
		}
	}
	
	public void broadcastGalacticIntentAfterIntent(GalacticIntent g, Intent i) {
		synchronized (g) {
			if (g.isBroadcasted())
				return;
			g.setPlayerManager(playerManager);
			g.broadcastAfterIntent(i);
		}
	}
	
}
