package intents.chat;

import resources.control.Intent;
import resources.player.Player;

public class SpatialChatIntent extends Intent {
	public static final String TYPE = "SpatialChatIntent";

	private Player player;
	private String message;
	private int chatType;
	private int moodId;
	
	public SpatialChatIntent(Player player, int chatType, String message, int moodId) {
		super(TYPE);
		this.player = player;
		this.message = message;
		this.chatType = chatType;
		this.moodId = moodId;
	}

	public Player getPlayer() { return player; }
	public String getMessage() { return message; }
	public int getChatType() { return chatType; }
	public int getMoodId() { return moodId; }
}
