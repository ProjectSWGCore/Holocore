package com.projectswg.holocore.intents.support.npc.ai;

import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;

public class StopNpcMovementIntent extends Intent {
	
	private final AIObject obj;
	
	public StopNpcMovementIntent(@NotNull AIObject obj) {
		this.obj = obj;
	}
	
	@NotNull
	public AIObject getObject() {
		return obj;
	}
	
	public static void broadcast(@NotNull AIObject obj) {
		new StopNpcMovementIntent(obj).broadcast();
	}
	
}
