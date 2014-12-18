package intents.chat;

import resources.control.Intent;
import resources.player.Player;

public class SpatialChatIntent extends Intent {
	public static final String TYPE = "SpatialChatIntent";
	
	private Player player;
	private String message;
	
	public SpatialChatIntent(Player player, String message) {
		super(TYPE);
		this.player = player;
		this.message = message;
	}

	public Player getPlayer() { return player; }
	public String getMessage() { return message; }
}
