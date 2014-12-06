package services.galaxy;

import intents.GalacticIntent;
import intents.GalacticPacketIntent;
import intents.InboundPacketIntent;
import resources.control.Intent;
import resources.control.Manager;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public class GalacticManager extends Manager {
	
	private final Object prevPacketIntentMutex = new Object();
	
	private ObjectManager objectManager;
	private PlayerManager playerManager;
	private GameManager gameManager;
	private Intent prevPacketIntent;
	
	public GalacticManager() {
		objectManager = new ObjectManager();
		playerManager = new PlayerManager();
		gameManager = new GameManager();
		prevPacketIntent = null;
		
		addChildService(objectManager);
		addChildService(playerManager);
		addChildService(gameManager);
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
			prepareGalacticIntent(i);
			i.broadcast();
		}
	}
	
	public void broadcastGalacticIntentAfterIntent(GalacticIntent g, Intent i) {
		synchronized (g) {
			if (g.isBroadcasted())
				return;
			prepareGalacticIntent(g);
			g.broadcastAfterIntent(i);
		}
	}
	
	private void prepareGalacticIntent(GalacticIntent i) {
		i.setObjectManager(objectManager);
		i.setPlayerManager(playerManager);
	}
	
}
