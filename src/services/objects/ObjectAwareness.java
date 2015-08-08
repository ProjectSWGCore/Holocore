/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
package services.objects;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import network.packets.swg.zone.UpdateContainmentMessage;
import resources.Location;
import resources.Terrain;
import resources.objects.SWGObject;
import resources.objects.quadtree.QuadTree;

public class ObjectAwareness {
	
	private static final double AWARE_RANGE = 1024;
	
	private final Map <Terrain, QuadTree <SWGObject>> quadTree;
	
	public ObjectAwareness() {
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
	}
	
	public boolean initialize() {
		loadQuadTree();
		return true;
	}
	
	private void loadQuadTree() {
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(16, -8192, -8192, 8192, 8192));
		}
	}
	
	/**
	 * Adds the specified object to the awareness quadtree
	 * @param object the object to add
	 */
	public void add(SWGObject object) {
		update(object);
		Location l = object.getLocation();
		if (invalidLocation(l))
			return;
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			tree.put(l.getX(), l.getZ(), object);
		}
	}
	
	/**
	 * Removes the specified object from the awareness quadtree
	 * @param object the object to remove
	 */
	public void remove(SWGObject object) {
		Location l = object.getLocation();
		if (invalidLocation(l))
			return;
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			tree.remove(l.getX(), l.getZ(), object);
		}
	}
	
	/**
	 * This function is used for moving objects within the world, which
	 * includes moving from a cell to the world.
	 * @param object the object to move
	 * @param nLocation the new location
	 */
	public void move(SWGObject object, Location nLocation) {
		if (object.getParent() != null) {
			object.getParent().removeObject(object); // Moving from cell to world
			object.sendObserversAndSelf(new UpdateContainmentMessage(object.getObjectId(), 0, object.getSlotArrangement()));
		} else {
			remove(object); // World to World
		}
		object.setLocation(nLocation);
		add(object);
	}
	
	/**
	 * This function is used for moving objects to or within containers,
	 * probably a cell. This handles the logic for removing the object from the
	 * previous cell and adding it to the new one, if necessary.
	 * @param object the object to move
	 * @param nParent the new parent the object will be in
	 * @param nLocation the new location relative to the parent
	 */
	public void move(SWGObject object, SWGObject nParent, Location nLocation) {
		SWGObject parent = object.getParent();
		if (parent != null && nParent != parent) {
			parent.removeObject(object); // Moving from cell to cell, for instance
		} else if (parent == null) {
			remove(object); // Moving from world to cell
		}
		if (object.getParent() == null) { // Should have been updated in removeObject()
			nParent.addObject(object); // If necessary, add to new cell
			object.sendObserversAndSelf(new UpdateContainmentMessage(object.getObjectId(), nParent.getObjectId(), object.getSlotArrangement()));
		}
		object.setLocation(nLocation);
		update(object);
	}
	
	/**
	 * Updates the specified object after it has been moved, or to verify that
	 * the awareness is up to date
	 * @param obj the object to update
	 */
	public void update(SWGObject obj) {
		if (obj.isBuildout())
			return;
		Location l = obj.getWorldLocation();
		if (invalidLocation(l))
			return;
		Set <SWGObject> objectAware = new HashSet<SWGObject>();
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			List <SWGObject> range = tree.getWithinRange(l.getX(), l.getZ(), AWARE_RANGE);
			for (SWGObject inRange : range) {
				if (isValidInRange(obj, inRange, l))
					objectAware.add(inRange);
			}
		}
		obj.updateObjectAwareness(objectAware);
	}
	
	private QuadTree <SWGObject> getTree(Location l) {
		return quadTree.get(l.getTerrain());
	}
	
	private boolean invalidLocation(Location l) {
		return l == null || l.getTerrain() == null;
	}
	
	private boolean isValidInRange(SWGObject obj, SWGObject inRange, Location objLoc) {
		if (inRange.getObjectId() == obj.getObjectId())
			return false;
		Location inRangeLoc = inRange.getWorldLocation();
		double distSquared = distanceSquared(objLoc, inRangeLoc);
		if (inRange.getLoadRange() != 0 && distSquared > square(inRange.getLoadRange()))
			return false;
		if (inRange.getLoadRange() == 0 && distSquared > square(200))
			return false;
		return true;
	}
	
	private double distanceSquared(Location l1, Location l2) {
		return square(l1.getX()-l2.getX()) + square(l1.getY()-l2.getY()) + square(l1.getZ()-l2.getZ());
	}
	
	private double square(double x) {
		return x * x;
	}
	
}