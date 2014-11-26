package intents;

import resources.control.Intent;
import resources.player.Player;
import resources.player.PlayerEvent;

public class PlayerEventIntent extends Intent {
	
	public static final String TYPE = "PlayerEventIntent";
	
	private Player player;
	private PlayerEvent event;
	
	public PlayerEventIntent(Player p, PlayerEvent event) {
		super(TYPE);
		setPlayer(p);
		setEvent(event);
	}
	
	public void setPlayer(Player p) {
		this.player = p;
	}
	
	public void setEvent(PlayerEvent event) {
		this.event = event;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public PlayerEvent getEvent() {
		return event;
	}
	
}
