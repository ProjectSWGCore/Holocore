package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class NpcNavigateMode extends NpcMode {
	
	private final NavigationPoint destination;
	private final Location destinationWorldLocation;
	
	public NpcNavigateMode(@NotNull AIObject obj, @NotNull NavigationPoint destination) {
		super(obj);
		this.destination = destination;
		this.destinationWorldLocation = (destination.getParent() == null) ? destination.getLocation() : Location.builder(destination.getLocation()).translateLocation(destination.getParent().getWorldLocation()).build();
	}
	
	@Override
	public void onModeStart() {
		runTo(destination.getParent(), destination.getLocation());
	}
	
	@Override
	public void act() {
		Location cur = getAI().getWorldLocation();
		if (cur.distanceTo(destinationWorldLocation) < 1E-3) {
			ScheduleNpcModeIntent.broadcast(getAI(), null);
		} else {
			queueNextLoop(500);
		}
	}
}
