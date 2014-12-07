package intents;

import resources.Galaxy;
import resources.control.Intent;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public abstract class GalacticIntent extends Intent {
	
	private ObjectManager objectManager;
	private PlayerManager playerManager;
	private Galaxy galaxy;
	
	protected GalacticIntent(String type) {
		super(type);
	}
	
	protected GalacticIntent(GalacticIntent i) {
		super(i.getType());
		setObjectManager(i.getObjectManager());
		setPlayerManager(i.getPlayerManager());
		setGalaxy(i.getGalaxy());
	}
	
	public void setObjectManager(ObjectManager objectManager) {
		this.objectManager = objectManager;
	}
	
	public void setPlayerManager(PlayerManager playerManager) {
		this.playerManager = playerManager;
	}
	
	public void setGalaxy(Galaxy galaxy) {
		this.galaxy = galaxy;
	}
	
	public ObjectManager getObjectManager() {
		return objectManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
	public Galaxy getGalaxy() {
		return galaxy;
	}
	
}
