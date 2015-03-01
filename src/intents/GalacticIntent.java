package intents;

import resources.Galaxy;
import resources.control.Intent;
import services.galaxy.GalacticManager;
import services.objects.ObjectManager;
import services.player.PlayerManager;

public abstract class GalacticIntent extends Intent {
	
	private GalacticManager galacticManager;
	private Galaxy galaxy;
	
	protected GalacticIntent(String type) {
		super(type);
	}
	
	protected GalacticIntent(GalacticIntent i) {
		super(i.getType());
		setGalacticManager(i.getGalacticManager());
		setGalaxy(i.getGalaxy());
	}
	
	public void setGalacticManager(GalacticManager galacticManager) {
		this.galacticManager = galacticManager;
	}
	
	public void setGalaxy(Galaxy galaxy) {
		this.galaxy = galaxy;
	}
	
	public ObjectManager getObjectManager() {
		return galacticManager.getObjectManager();
	}
	
	public PlayerManager getPlayerManager() {
		return galacticManager.getPlayerManager();
	}
	
	public GalacticManager getGalacticManager() {
		return galacticManager;
	}
	
	public Galaxy getGalaxy() {
		return galaxy;
	}
	
}
