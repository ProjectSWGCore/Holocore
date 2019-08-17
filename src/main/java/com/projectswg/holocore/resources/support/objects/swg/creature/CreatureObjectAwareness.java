/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CreatureObjectAwareness {
	
	private final CreatureObject creature;
	private final Set<SWGObject> aware;
	private final Set<Long> awareIds;
	private final Comparator<SWGObject> objectComparator;
	private final AtomicReference<SWGPacket> finalTeleportPacket;
	
	public CreatureObjectAwareness(CreatureObject creature) {
		this.creature = creature;
		this.aware = new HashSet<>();
		this.awareIds = new HashSet<>();
		this.objectComparator = Comparator.comparingInt(CreatureObjectAwareness::getObjectDepth).thenComparingDouble(this::getDistance);
		this.finalTeleportPacket = new AtomicReference<>(null);
	}
	
	public synchronized void setTeleportDestination(SWGObject parent, Location location) {
		if (parent == null)
			finalTeleportPacket.set(new DataTransform(creature.getObjectId(), 0, creature.getNextUpdateCount(), location, 0));
		else
			finalTeleportPacket.set(new DataTransformWithParent(creature.getObjectId(), 0, creature.getNextUpdateCount(), parent.getObjectId(), location, 0));
	}
	
	public synchronized void flush(@NotNull Player target) {
		Set<SWGObject> newAware = creature.getAware();
		flushCreates(target, newAware);
		
		SWGPacket finalTeleportPacket = this.finalTeleportPacket.getAndSet(null);
		if (finalTeleportPacket != null)
			creature.sendSelf(finalTeleportPacket);
		
		flushDestroys(target, newAware);
	}
	
	private void flushCreates(@NotNull Player target, Set<SWGObject> newAware) {
		List<SWGObject> create = new ArrayList<>();
		List<SWGObject> added = new ArrayList<>();
		
		// Create Deltas
		for (SWGObject createCandidate : newAware) {
			if (aware.contains(createCandidate))
				continue;
			added.add(createCandidate);
		}
		getCreateList(create, added);
		
		// Create the objects on the client
		LinkedList<SWGObject> createStack = new LinkedList<>();
		for (SWGObject obj : create) {
			if (isBundledObject(obj, obj.getParent()))
				popStackUntil(target, createStack, obj.getParent());
			else
				popStackAll(target, createStack);
			createStack.add(obj);
			createObject(obj, target);
		}
		popStackAll(target, createStack);
		
		// Server awareness update
		for (SWGObject add : create) { // Using "create" here because it's filtered to ensure no crashes
			aware.add(add);
			awareIds.add(add.getObjectId());
			add.addObserver(creature);
		}
		
		// Hope we didn't screw anything up
		assert aware.contains(creature.getSlottedObject("ghost")) : "not aware of ghost " + creature;
		assert aware.contains(creature) : "not aware of creature";
	}
	
	private void flushDestroys(@NotNull Player target, Set<SWGObject> newAware) {
		List<SWGObject> destroy = new ArrayList<>();
		List<SWGObject> removed = new ArrayList<>();
		
		// Create Deltas
		for (SWGObject destroyCandidate : aware) {
			if (newAware.contains(destroyCandidate))
				continue;
			removed.add(destroyCandidate);
		}
		getDestroyList(destroy, removed);
		
		// Remove destroyed objects so that nobody tries to send a packet to us after we send the destroy
		for (Iterator<SWGObject> it = aware.iterator(); it.hasNext(); ) {
			SWGObject currentAware = it.next();
			for (SWGObject remove : destroy) { // Since the "create" is filtered, aware could also have been filtered
				if (isParent(currentAware, remove)) {
					it.remove();
					awareIds.remove(currentAware.getObjectId());
					remove.removeObserver(creature);
					break;
				}
			}
		}
		
		// Destroy the objects on the client
		for (SWGObject obj : destroy) {
			destroyObject(obj, target);
		}
		
		// Hope we didn't screw anything up
		assert aware.contains(creature.getSlottedObject("ghost")) : "not aware of ghost " + creature;
		assert aware.contains(creature) : "not aware of creature";
	}
	
	public synchronized void resetObjectsAware() {
		for (SWGObject obj : aware) {
			obj.removeObserver(creature);
		}
		aware.clear();
		awareIds.clear();
	}
	
	public synchronized boolean isAware(long objectId) {
		return awareIds.contains(objectId);
	}
	
	public synchronized boolean isAware(SWGObject obj) {
		return aware.contains(obj);
	}
	
	private void getCreateList(List<SWGObject> list, List<SWGObject> added) {
		added.sort(objectComparator);
		for (SWGObject obj : added) {
			SWGObject parent = obj.getParent();
			if (parent != null && !aware.contains(parent) && !list.contains(parent)) {
				assert !(obj instanceof CellObject);
				continue;
			}
			assert !(obj instanceof BuildingObject) || added.containsAll(obj.getContainedObjects()) : "All cells must be sent with the building";
			if (!isBundledObject(obj, parent) || aware.contains(parent)) {
				list.add(obj);
			} else {
				int parentIndex = list.indexOf(parent);
				assert parentIndex != -1 : "parent isn't added along with child";
				list.add(parentIndex+1, obj);
			}
		}
	}
	
	private void getDestroyList(List<SWGObject> list, List<SWGObject> removed) {
		removed.sort(objectComparator);
		for (SWGObject obj : removed) {
			// Don't delete our own parent nor child objects if we're deleting their parent (optimization)
			if (isParent(obj) || list.contains(obj.getParent()))
				continue;
			list.add(obj);
		}
	}
	
	private boolean isParent(SWGObject obj) {
		return isParent(creature, obj);
	}
	
	private double getDistance(SWGObject obj) {
		if (obj.getParent() != null)
			return 0;
		
		return creature.getWorldLocation().distanceTo(obj.getLocation());
	}
	
	private boolean isBundledObject(SWGObject obj, SWGObject parent) {
		return parent != null && (obj.getSlotArrangement() == -1 || obj.getBaselineType() == BaselineType.PLAY || parent == creature);
	}
	
	private void createObject(@NotNull SWGObject obj, @NotNull Player target) {
		long id = obj.getObjectId();
		{ // SceneCreateObjectByCrc
			SceneCreateObjectByCrc create = new SceneCreateObjectByCrc();
			create.setObjectId(id);
			create.setLocation(obj.getLocation());
			create.setObjectCrc(obj.getCrc());
			target.sendPacket(create);
		}
		{ // Baselines
			boolean owner = obj.getOwner() == target;
			
			target.sendPacket(obj.createBaseline3(target));
			target.sendPacket(obj.createBaseline6(target));
			
			if (owner) {
				target.sendPacket(obj.createBaseline1(target));
				target.sendPacket(obj.createBaseline4(target));
				target.sendPacket(obj.createBaseline8(target));
				target.sendPacket(obj.createBaseline9(target));
			}
		}
		{ // Miscellaneous
			if (obj instanceof CellObject)
				target.sendPacket(new UpdateCellPermissionMessage((byte) 1, id));
			// ? UpdatePostureMessage for PlayerObject ?
			if (obj instanceof CreatureObject && obj.isGenerated()) {
				CreatureObject creature = (CreatureObject) obj;
				target.sendPacket(new UpdatePostureMessage(creature.getPosture().getId(), id));
				target.sendPacket(new UpdatePvpStatusMessage(creature.getPvpFaction(), id, this.creature.getPvpFlagsFor(creature)));
			}
		}
		{ // UpdateContainmentMessage
			SWGObject parent = obj.getParent();
			if (parent != null)
				target.sendPacket(new UpdateContainmentMessage(obj.getObjectId(), parent.getObjectId(), obj.getSlotArrangement()));
		}
	}
	
	private static void popStackAll(Player target, LinkedList<SWGObject> createStack) {
		SWGObject parent;
		while ((parent = createStack.pollLast()) != null) {
			target.sendPacket(new SceneEndBaselines(parent.getObjectId()));
		}
	}
	
	private static void popStackUntil(Player target, LinkedList<SWGObject> createStack, SWGObject parent) {
		SWGObject last;
		while ((last = createStack.peekLast()) != null) {
			if (last == parent)
				break;
			createStack.pollLast();
			target.sendPacket(new SceneEndBaselines(last.getObjectId()));
		}
	}
	
	private static void destroyObject(@NotNull SWGObject obj, @NotNull Player target) {
		target.sendPacket(new SceneDestroyObject(obj.getObjectId()));
	}
	
	private static int getObjectDepth(SWGObject obj) {
		return obj == null ? 0 : 1 + getObjectDepth(obj.getParent());
	}
	
	private static boolean isParent(SWGObject child, SWGObject testParent) {
		SWGObject parent = child;
		while (parent != null) {
			if (parent == testParent)
				return true;
			parent = parent.getParent();
		}
		return false;
	}
	
}
