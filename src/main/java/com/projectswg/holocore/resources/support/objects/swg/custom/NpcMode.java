/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.npc.ai.CompileNpcMovementIntent;
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent;
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
		new MoveObjectIntent(obj, parent, location, getWalkSpeed()).broadcast();
	}
	
	public final void moveTo(Location location) {
		new MoveObjectIntent(obj, location, getWalkSpeed()).broadcast();
	}
	
	public final void walkTo(SWGObject parent, Location location) {
		new CompileNpcMovementIntent(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), parent, location, getWalkSpeed()), NavigationRouteType.TERMINATE, getWalkSpeed(), null).broadcast();
	}
	
	public final void walkTo(Location location) {
		new CompileNpcMovementIntent(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), location, getWalkSpeed()), NavigationRouteType.TERMINATE, getWalkSpeed(), null).broadcast();
	}
	
	public final void runTo(SWGObject parent, Location location) {
		new CompileNpcMovementIntent(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), parent, location, getRunSpeed()), NavigationRouteType.TERMINATE, getRunSpeed(), null).broadcast();
	}
	
	public final void runTo(Location location) {
		new CompileNpcMovementIntent(obj, NavigationPoint.from(obj.getParent(), obj.getLocation(), location, getRunSpeed()), NavigationRouteType.TERMINATE, getRunSpeed(), null).broadcast();
	}
	
}
