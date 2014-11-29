package intents;

import resources.control.Intent;
import services.player.PlayerManager;

public abstract class GalacticIntent extends Intent {
	
	private PlayerManager playerManager;
	
	protected GalacticIntent(String type) {
		super(type);
	}
	
	protected GalacticIntent(GalacticIntent i) {
		super(i.getType());
		setPlayerManager(i.getPlayerManager());
	}
	
	public void setPlayerManager(PlayerManager playerManager) {
		this.playerManager = playerManager;
	}
	
	public PlayerManager getPlayerManager() {
		return playerManager;
	}
	
}
