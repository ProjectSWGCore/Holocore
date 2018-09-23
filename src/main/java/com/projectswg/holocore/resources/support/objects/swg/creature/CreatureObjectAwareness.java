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

import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class CreatureObjectAwareness {
	
	private final CreatureObject creature;
	private final Set<SWGObject> aware;
	private final Set<SWGObject> pendingAdd;
	private final Set<SWGObject> pendingRemove;
	
	public CreatureObjectAwareness(CreatureObject creature) {
		this.creature = creature;
		this.aware = new HashSet<>();
		this.pendingAdd = new HashSet<>();
		this.pendingRemove = new HashSet<>();
	}
	
	public synchronized void addAware(@NotNull SWGObject obj) {
		if (pendingRemove.remove(obj) || aware.contains(obj))
			return;
		pendingAdd.add(obj);
		if (obj instanceof BuildingObject) { // Client is picky about buildings - just need to ensure the cells are sent with the building no matter what
			obj.getContainedObjects().forEach(this::addAware);
		}
	}
	
	public synchronized void removeAware(@NotNull SWGObject obj) {
		assert obj != creature;
		if (pendingAdd.remove(obj) || !aware.contains(obj) || obj.getBaselineType() == BaselineType.SCLT)
			return;
		pendingRemove.add(obj);
	}
	
	public synchronized void flushAware() {
		Player target = creature.getOwnerShallow();
		List<SWGObject> create = getCreateList();
		List<SWGObject> destroy = getDestroyList();
		
		aware.addAll(create);
		aware.removeAll(destroy);
		pendingAdd.removeAll(create);
		pendingRemove.clear();
		
		if (target != null) {
			for (SWGObject obj : destroy) {
				destroyObject(obj, target);
			}
			
			LinkedList<SWGObject> createStack = new LinkedList<>();
			for (SWGObject obj : create) {
				popStackUntil(target, createStack, obj.getSlotArrangement() == -1 ? obj.getParent() : null);
				createStack.add(obj);
				createObject(obj, target);
			}
			popStackUntil(target, createStack, null);
			assert aware.contains(creature.getSlottedObject("ghost")) : "not aware of ghost " + creature;
		}
		
		assert aware.contains(creature) || pendingAdd.contains(creature): "not aware of creature";
	}
	
	public synchronized void resetObjectsAware() {
		aware.clear();
		pendingAdd.clear();
		pendingRemove.clear();
		pendingAdd.addAll(creature.getAware());
	}
	
	public synchronized boolean isAware(SWGObject obj) {
		return aware.contains(obj);
	}
	
	List<SWGObject> getCreateList() {
		List<SWGObject> list = new ArrayList<>();
		List<SWGObject> sortedDepth = new ArrayList<>(pendingAdd);
		sortedDepth.sort(Comparator.comparingInt(CreatureObjectAwareness::getObjectDepth).thenComparingDouble(this::getDistance));
		for (SWGObject obj : sortedDepth) {
			SWGObject parent = obj.getParent();
			if (parent == null || aware.contains(parent) || obj.getSlotArrangement() != -1) {
				list.add(obj);
			} else {
				if (!aware.contains(parent) && !pendingAdd.contains(parent))
					continue;
				int parentIndex = list.indexOf(parent);
				if (parentIndex == -1)
					continue;
				list.add(parentIndex+1, obj);
			}
		}
		return list;
	}
	
	List<SWGObject> getDestroyList() {
		List<SWGObject> list = new ArrayList<>(pendingRemove);
		list.removeIf(this::isParent);
		list.sort(Comparator.comparingInt(CreatureObjectAwareness::getObjectDepth).reversed());
		for (Iterator<SWGObject> it = list.iterator(); it.hasNext(); ) {
			SWGObject obj = it.next();
			SWGObject parent = obj.getParent();
			if (list.contains(parent))
				it.remove();
		}
		return list;
	}
	
	private boolean isParent(SWGObject obj) {
		SWGObject parent = creature.getParent();
		while (parent != null) {
			if (parent == obj)
				return true;
			parent = parent.getParent();
		}
		return false;
	}
	
	private double getDistance(SWGObject obj) {
		if (obj.getParent() != null)
			return 0;
		
		return creature.getWorldLocation().distanceTo(obj.getLocation());
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
			boolean creature = obj instanceof CreatureObject;
			boolean owner = obj.getOwner() == target;
			
			if (owner && creature)
				target.sendPacket(obj.createBaseline1(target));
			
			target.sendPacket(obj.createBaseline3(target));
			
			if (owner && creature)
				target.sendPacket(obj.createBaseline4(target));
			
			target.sendPacket(obj.createBaseline6(target));
			
			if (owner) {
				target.sendPacket(obj.createBaseline8(target));
				target.sendPacket(obj.createBaseline9(target));
			}
		}
		{ // Miscelaneous
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
	
	private static void popStackUntil(Player target, LinkedList<SWGObject> createStack, SWGObject parent) {
		while (!createStack.isEmpty() && createStack.getLast() != parent) {
			target.sendPacket(new SceneEndBaselines(createStack.pollLast().getObjectId()));
		}
	}
	
	private static void destroyObject(@NotNull SWGObject obj, @NotNull Player target) {
		target.sendPacket(new SceneDestroyObject(obj.getObjectId()));
	}
	
	private static int getObjectDepth(SWGObject obj) {
		return obj == null ? 0 : 1 + getObjectDepth(obj.getParent());
	}
	
}
