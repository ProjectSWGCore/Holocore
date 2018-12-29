package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.resources.support.npc.ai.NavigationPoint;
import com.projectswg.holocore.resources.support.npc.ai.NavigationRouteType;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

import java.util.Collection;

public abstract class NpcMode {
	
	private final AIObject obj;
	
	public NpcMode(AIObject obj) {
		this.obj = obj;
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
		return obj;
	}
	
	public final Spawner getSpawner() {
		return obj.getSpawner();
	}
	
	public final void queueNextLoop(long delay) {
		obj.queueNextLoop(delay);
	}
	
	public final double getWalkSpeed() {
		return obj.getMovementPercent() * obj.getMovementScale() * obj.getWalkSpeed();
	}
	
	public final double getRunSpeed() {
		return obj.getMovementPercent() * obj.getMovementScale() * obj.getRunSpeed();
	}
	
	public final void moveTo(SWGObject parent, Location location) {
		MoveObjectIntent.broadcast(obj, parent, location, getWalkSpeed());
	}
	
	public final void moveTo(Location location) {
		MoveObjectIntent.broadcast(obj, location, getWalkSpeed());
	}
	
	public final void walkTo(SWGObject parent, Location location) {
		CompileNpcMovementIntent.broadcast(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), parent, location, getWalkSpeed()), NavigationRouteType.TERMINATE, getWalkSpeed());
	}
	
	public final void walkTo(Location location) {
		CompileNpcMovementIntent.broadcast(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), location, getWalkSpeed()), NavigationRouteType.TERMINATE, getWalkSpeed());
	}
	
	public final void runTo(SWGObject parent, Location location) {
		CompileNpcMovementIntent.broadcast(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), parent, location, getRunSpeed()), NavigationRouteType.TERMINATE, getRunSpeed());
	}
	
	public final void runTo(Location location) {
		CompileNpcMovementIntent.broadcast(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), location, getRunSpeed()), NavigationRouteType.TERMINATE, getRunSpeed());
	}
	
}
