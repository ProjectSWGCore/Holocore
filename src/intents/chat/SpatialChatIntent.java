package intents.chat;

import resources.control.Intent;
import resources.player.Player;

public class SpatialChatIntent extends Intent {
	public static final String TYPE = "SpatialChatIntent";

	private Player player;
	private String message;
	private int chatType;
	
	public SpatialChatIntent(Player player, int chatType, String message) {
		super(TYPE);
		this.player = player;
		this.message = message;
		this.chatType = chatType;
	}

	public Player getPlayer() { return player; }
	public String getMessage() { return message; }
	public int getChatType() { return chatType; }
}
