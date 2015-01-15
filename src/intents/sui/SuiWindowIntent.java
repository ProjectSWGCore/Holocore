package intents.sui;

import resources.control.Intent;
import resources.player.Player;
import resources.sui.SuiWindow;

public class SuiWindowIntent extends Intent {
	public static final String TYPE = "SuiWindowIntent";
	
	private SuiWindow window;
	private Player player;
	private SuiWindowEvent event;
	
	public SuiWindowIntent(Player player, SuiWindow window, SuiWindowEvent event) {
		super(TYPE);
		this.player = player;
		this.window = window;
		this.event = event;
	}

	public SuiWindow getWindow() { return this.window; }
	public Player getPlayer() { return this.player; }
	public SuiWindowEvent getEvent() { return event; }
	
	public enum SuiWindowEvent {
		NEW,
		UPDATE;
	}
}
