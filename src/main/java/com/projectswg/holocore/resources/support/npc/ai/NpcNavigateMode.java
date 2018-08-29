package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;

public class NpcNavigateMode extends NpcMode {
	
	private final NavigationPoint destination;
	private final AtomicBoolean startedMovement;
	
	public NpcNavigateMode(@NotNull AIObject obj, @NotNull NavigationPoint destination) {
		super(obj);
		this.destination = destination;
		this.startedMovement = new AtomicBoolean(false);
	}
	
	@Override
	public void act() {
		if (destination.distanceTo(getAI()) > 1E-3) {
			if (!startedMovement.getAndSet(true)) {
				StartNpcMovementIntent.broadcast(getAI(), destination.getParent(), destination.getLocation(), destination.getSpeed());
			}
			queueNextLoop(1000);
		} else {
			ScheduleNpcModeIntent.broadcast(getAI(), null);
		}
	}
}
