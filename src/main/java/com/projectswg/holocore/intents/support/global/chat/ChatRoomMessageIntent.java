package com.projectswg.holocore.intents.support.global.chat;

import com.projectswg.holocore.resources.support.global.player.Player;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class ChatRoomMessageIntent extends Intent {
	
	private final Player player;
	private final String roomPath;
	private final String message;
	
	public ChatRoomMessageIntent(@NotNull Player player, @NotNull String roomPath, @NotNull String message) {
		this.player = player;
		this.roomPath = roomPath;
		this.message = message;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}
	
	@NotNull
	public String getRoomPath() {
		return roomPath;
	}
	
	@NotNull
	public String getMessage() {
		return message;
	}
	
	public static void broadcast(@NotNull Player player, @NotNull String roomPath, @NotNull String message) {
		new ChatRoomMessageIntent(player, roomPath, message).broadcast();
	}
}
