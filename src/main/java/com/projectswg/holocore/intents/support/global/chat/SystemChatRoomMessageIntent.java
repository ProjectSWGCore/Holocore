package com.projectswg.holocore.intents.support.global.chat;

import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class SystemChatRoomMessageIntent extends Intent {

	private final String roomPath;
	private final String message;

	public SystemChatRoomMessageIntent(@NotNull String roomPath, @NotNull String message) {
		this.roomPath = roomPath;
		this.message = message;
	}
	
	@NotNull
	public String getRoomPath() {
		return roomPath;
	}
	
	@NotNull
	public String getMessage() {
		return message;
	}
	
	public static void broadcast(@NotNull String roomPath, @NotNull String message) {
		new SystemChatRoomMessageIntent(roomPath, message).broadcast();
	}
}
