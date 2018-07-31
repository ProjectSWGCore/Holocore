package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import org.jetbrains.annotations.NotNull;

public class NpcNavigateMode extends NpcMode {
	
	private final NavigationPoint destination;
	
	public NpcNavigateMode(@NotNull AIObject obj, @NotNull NavigationPoint destination) {
		super(obj);
		this.destination = destination;
	}
	
	@Override
	public void onModeStart() {
		StartNpcMovementIntent.broadcast(getAI(), destination.getParent(), destination.getLocation(), destination.getSpeed());
	}
	
	@Override
	public void act() {
		if (destination.distanceTo(getAI()) > 1E-3) {
			queueNextLoop(1000);
		} else {
			ScheduleNpcModeIntent.broadcast(getAI(), null);
		}
	}
}
