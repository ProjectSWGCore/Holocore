package intents;

import resources.control.Intent;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public abstract class GalacticIntent extends Intent {
	
	private ObjectManager objectManager;
	private PlayerManager playerManager;
	
	protected GalacticIntent(String type) {
		super(type);
	}
	
	protected GalacticIntent(GalacticIntent i) {
		super(i.getType());
		setObjectManager(i.getObjectManager());
		setPlayerManager(i.getPlayerManager());
	}
	
	public void setObjectManager(ObjectManager objectManager) {
		this.objectManager = objectManager;
	}
	
	public void setPlayerManager(PlayerManager playerManager) {
		this.playerManager = playerManager;
	}
	
	public ObjectManager getObjectManager() {
		return objectManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
}
