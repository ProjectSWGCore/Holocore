package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject.ScheduledMode;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.Random;

public abstract class NpcMode {
	
	private final Random random;
	
	private AIObject obj;
	private ScheduledMode mode;
	
	public NpcMode() {
		this.random = new Random();
		this.obj = null;
		this.mode = null;
	}
	
	void attach(AIObject obj, ScheduledMode mode) {
		this.obj = obj;
		this.mode = mode;
	}
	
	public abstract void act();
	
	public void onPlayerEnterAware(CreatureObject player, double distance) {
		
	}
	
	public void onPlayerMoveInAware(CreatureObject player, double distance) {
		
	}
	
	public void onPlayerExitAware(CreatureObject player) {
		
	}
	
	public void onModeStart() {
		
	}
	
	public void onModeEnd() {
		
	}
	
	public Collection<CreatureObject> getNearbyPlayers() {
		return getAI().getNearbyPlayers();
	}
	
	public boolean isRooted() {
		switch (getAI().getPosture()) {
			case DEAD:
			case INCAPACITATED:
			case INVALID:
			case KNOCKED_DOWN:
			case LYING_DOWN:
			case SITTING:
				return true;
			case BLOCKING:
			case CLIMBING:
			case CROUCHED:
			case DRIVING_VEHICLE:
			case FLYING:
			case PRONE:
			case RIDING_CREATURE:
			case SKILL_ANIMATING:
			case SNEAKING:
			case UPRIGHT:
			default:
				// Rooted if there are no nearby players
				return getNearbyPlayers().isEmpty();
		}
	}
	
	public final AIObject getAI() {
		return Objects.requireNonNull(obj);
	}
	
	public final Random getRandom() {
		return random;
	}
	
	@Nullable
	public final NpcMode requestPeerMode(AIObject obj) {
		return obj.getMode(mode);
	}
	
	public final void queueNextLoop(long delay) {
		obj.queueNextLoop(delay);
	}
	
	public final boolean isExecuting() {
		return obj.getActiveMode() == mode;
	}
	
	public final void requestModeStart() {
		obj.requestModeStart(mode);
	}
	
	public final void requestModeEnd() {
		obj.requestModeEnd(mode);
	}
	
	public final double getWalkSpeed() {
		return obj.getMovementPercent() * obj.getMovementScale() * obj.getWalkSpeed();
	}
	
	public final double getRunSpeed() {
		return obj.getMovementPercent() * obj.getMovementScale() * obj.getRunSpeed();
	}
	
	public final void walkTo(SWGObject parent, Location location) {
		MoveObjectIntent.broadcast(obj, parent, location, getWalkSpeed(), obj.getNextUpdateCount());
	}
	
	public final void walkTo(Location location) {
		MoveObjectIntent.broadcast(obj, obj.getParent(), location, getWalkSpeed(), obj.getNextUpdateCount());
	}
	
	public final void runTo(SWGObject parent, Location location) {
		MoveObjectIntent.broadcast(obj, parent, location, getRunSpeed(), obj.getNextUpdateCount());
	}
	
	public final void runTo(Location location) {
		MoveObjectIntent.broadcast(obj, obj.getParent(), location, getRunSpeed(), obj.getNextUpdateCount());
	}
	
}
