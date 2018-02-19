package com.projectswg.holocore.intents;

import com.projectswg.common.control.Intent;
import com.projectswg.holocore.resources.objects.player.PlayerObject;

public class CivilWarPointIntent extends Intent {
	
	public static void broadcast(PlayerObject receiver, int points) {
		new CivilWarPointIntent(receiver, points).broadcast();
	}
	
	private final PlayerObject receiver;
	private final int points;
	
	private CivilWarPointIntent(PlayerObject receiver, int points) {
		this.receiver = receiver;
		this.points = points;
	}
	
	public PlayerObject getReceiver() {
		return receiver;
	}
	
	public int getPoints() {
		return points;
	}
	
}
