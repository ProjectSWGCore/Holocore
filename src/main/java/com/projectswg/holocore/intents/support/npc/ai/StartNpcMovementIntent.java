package com.projectswg.holocore.intents.support.npc.ai;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StartNpcMovementIntent extends Intent {
	
	private final AIObject obj;
	private final SWGObject parent;
	private final Location destination;
	private final double speed;;
	
	public StartNpcMovementIntent(@NotNull AIObject obj, @Nullable SWGObject parent, @NotNull Location destination, double speed) {
		assert parent == null || parent instanceof CellObject : "parent was: " + parent;
		this.obj = obj;
		this.parent = parent;
		this.destination = destination;
		this.speed = speed;
	}
	
	@NotNull
	public AIObject getObject() {
		return obj;
	}
	
	@Nullable
	public SWGObject getParent() {
		return parent;
	}
	
	@NotNull
	public Location getDestination() {
		return destination;
	}
	
	public double getSpeed() {
		return speed;
	}
	
	public static void broadcast(@NotNull AIObject obj, @Nullable SWGObject parent, @NotNull Location destination, double speed) {
		new StartNpcMovementIntent(obj, parent, destination, speed).broadcast();
	}
	
}
