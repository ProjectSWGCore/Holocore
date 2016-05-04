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

import intents.PlayerEventIntent;
import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.object.UpdateObjectAwareness;
import intents.player.PlayerTransformedIntent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import network.packets.Packet;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Terrain;
import resources.buildout.BuildoutArea;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.quadtree.QuadTree;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.server_info.Log;

public class ObjectAwareness extends Service {
	
	private static final double AWARE_RANGE = 1024;
	private static final int DEFAULT_LOAD_RANGE = (int) square(200); // Squared for speed
	
	private final Map <Terrain, QuadTree <SWGObject>> quadTree;
	
	public ObjectAwareness() {
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(UpdateObjectAwareness.TYPE);
		loadQuadTree();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					handleObjectCreatedIntent((ObjectCreatedIntent) i);
				break;
			case ObjectTeleportIntent.TYPE:
				if (i instanceof ObjectTeleportIntent)
					processObjectTeleportIntent((ObjectTeleportIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processGalacticPacketIntent((GalacticPacketIntent) i);
				break;
			case UpdateObjectAwareness.TYPE:
				if (i instanceof UpdateObjectAwareness)
					processUpdateObjectAwarenessIntent((UpdateObjectAwareness) i);
			default:
				break;
		}
	}
	
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		if (creature == null)
			return;
		switch (pei.getEvent()) {
			case PE_DISAPPEAR:
				remove(creature);
				for (SWGObject obj : creature.getObservers())
					creature.destroyObject(obj.getOwner());
				creature.clearAware();
				creature.setOwner(null);
				p.setCreatureObject(null);
				break;
			case PE_ZONE_IN_SERVER:
				creature.clearAware(false);
				add(creature);
				update(creature);
				p.sendPacket(new CmdSceneReady());
				break;
			default:
				break;
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		if (isInAwareness(object)) {
			// We don't add logged out players to awareness when objects are being loaded from ODB.
			if(object instanceof CreatureObject && ((CreatureObject) object).isLoggedOutPlayer()) {
				return;
			}
			
			add(object);
			update(object);
		}
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		Player owner = object.getOwner();
		Location old = object.getLocation();
		object.setLocation(oti.getNewLocation());
		if (oti.getParent() != null) {
			move(object, oti.getParent(), oti.getNewLocation(), false);
		} else {
			moveFromOld(object, old, false);
		}
		if (object instanceof CreatureObject && ((CreatureObject) object).isLoggedInPlayer())
			new RequestZoneInIntent(owner, (CreatureObject) object, false).broadcast();
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent i) {
		Packet packet = i.getPacket();
		if (packet instanceof DataTransform) {
			DataTransform trans = (DataTransform) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			if (obj instanceof CreatureObject)
				moveObject((CreatureObject) obj, trans);
		} else if (packet instanceof DataTransformWithParent) {
			DataTransformWithParent transformWithParent = (DataTransformWithParent) packet;
			SWGObject object = i.getObjectManager().getObjectById(transformWithParent.getObjectId());
			SWGObject parent = i.getObjectManager().getObjectById(transformWithParent.getCellId());
			if (object instanceof CreatureObject)
				moveObject((CreatureObject) object, parent, transformWithParent);
		}
	}
	
	private void processUpdateObjectAwarenessIntent(UpdateObjectAwareness i) {
		SWGObject obj = i.getObject();
		Location l = obj.getLocation();
		QuadTree <SWGObject> tree = getTree(l);
		List<SWGObject> objects;
		synchronized (tree) {
			objects = tree.get(l.getX(), l.getZ());
		}
		Log.d(this, "Updated awareness for %s", obj);
		if (objects.contains(obj)) {
			if (!i.isInAwareness()) {
				remove(obj);
				obj.clearAware(false);
			}
			return;
		}
		add(obj);
		update(obj);
	}
	
	private void moveObject(CreatureObject obj, DataTransform transform) {
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		double time = ((CreatureObject) obj).getTimeSinceLastTransform() / 1000;
		obj.updateLastTransformTime();
		Location l = obj.getWorldLocation();
		double speed = Math.sqrt(square(l.getX()-newLocation.getX()) + square(l.getZ()-newLocation.getZ())) / time;
		if (speed > obj.getMovementScale()*7.3) {
			double angle = (newLocation.getX() == l.getX() ? 0 : Math.atan2(newLocation.getZ()-l.getZ(), newLocation.getX()-l.getX()));
			newLocation.setX(l.getX()+obj.getMovementScale()*7.3*time*Math.cos(angle));
			newLocation.setZ(l.getZ()+obj.getMovementScale()*7.3*time*Math.sin(angle));
			transform.setSpeed((float) (obj.getMovementScale()*7.3));
		}
		BuildoutArea area = obj.getBuildoutArea();
		if (area == null)
			System.err.println("Unknown buildout area at: " + obj.getWorldLocation());
		else
			newLocation = area.adjustLocation(newLocation);
		new PlayerTransformedIntent(obj, obj.getParent(), null, obj.getLocation(), newLocation).broadcast();
		move(obj, newLocation, true);
		if (area != null)
			newLocation = area.readjustLocation(newLocation);
		obj.sendDataTransforms(transform);
	}
	
	private void moveObject(CreatureObject obj, SWGObject parent, DataTransformWithParent transformWithParent) {
		Location newLocation = transformWithParent.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		if (parent == null) {
			System.err.println("ObjectManager: Could not find parent for transform! Cell: " + transformWithParent.getCellId());
			Log.e("ObjectManager", "Could not find parent for transform! Cell: %d  Object: %s", transformWithParent.getCellId(), obj);
			return;
		}
		double time = ((CreatureObject) obj).getTimeSinceLastTransform() / 1000;
		obj.updateLastTransformTime();
		Location l = obj.getWorldLocation();
		Location nWorld = new Location(newLocation.getX(), 0, newLocation.getZ(), parent.getTerrain());
		nWorld.translateLocation(parent.getWorldLocation());
		double speed = Math.sqrt(square(l.getX()-nWorld.getX()) + square(l.getZ()-nWorld.getZ())) / time;
		if (speed > obj.getMovementScale()*7.3) {
			double angle = (nWorld.getX() == l.getX() ? 0 : Math.atan2(nWorld.getZ()-l.getZ(), nWorld.getX()-l.getX())) + Math.PI;
			newLocation.setX(newLocation.getX()+obj.getMovementScale()*7.3*time*invertNormalizedValue(Math.cos(angle)));
			newLocation.setZ(newLocation.getZ()+obj.getMovementScale()*7.3*time*invertNormalizedValue(Math.sin(angle)));
			transformWithParent.setSpeed((float) (obj.getMovementScale()*7.3));
		}
		new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), newLocation).broadcast();
		move(obj, parent, newLocation, true);
		obj.sendParentDataTransforms(transformWithParent);
	}
	
	private void loadQuadTree() {
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(16, -8192, -8192, 8192, 8192));
		}
	}
	
	private boolean isInAwareness(SWGObject object) {
		return object.getParent() == null && object instanceof TangibleObject;
	}
	
	private double invertNormalizedValue(double x) {
		if (x < 0)
			return -1 - x;
		return 1-x;
	}
	
	/**
	 * Adds the specified object to the awareness quadtree
	 * @param object the object to add
	 */
	public void add(SWGObject object) {
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
		removeFromLocation(object, object.getLocation());
	}
	
	/**
	 * This function is used for moving objects within the world, which
	 * includes moving from a cell to the world.
	 * @param object the object to move
	 * @param nLocation the new location
	 * @param update boolean on whether or not to update the object's awareness
	 */
	private void move(SWGObject object, Location nLocation, boolean update) {
		if (object.getParent() != null) {
			object.getParent().removeObject(object); // Moving from cell to world
			object.sendObserversAndSelf(new UpdateContainmentMessage(object.getObjectId(), 0, object.getSlotArrangement()));
		} else {
			remove(object); // World to World
		}
		object.setLocation(nLocation);
		add(object);
		if (update)
			update(object);
	}
	
	/**
	 * This function is used for moving objects to or within containers,
	 * probably a cell. This handles the logic for removing the object from the
	 * previous cell and adding it to the new one, if necessary.
	 * @param object the object to move
	 * @param nParent the new parent the object will be in
	 * @param nLocation the new location relative to the parent
	 * @param update boolean on whether or not to update the object's awareness
	 */
	private void move(SWGObject object, SWGObject nParent, Location nLocation, boolean update) {
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
		if (update)
			update(object);
	}
	
	/**
	 * Updates the specified object after it has been moved, or to verify that
	 * the awareness is up to date
	 * @param obj the object to update
	 */
	private void update(SWGObject obj) {
		if (!obj.isGenerated())
			return;
		Location l = obj.getWorldLocation();
		if (invalidLocation(l))
			return;
		Set <SWGObject> objectAware = new HashSet<SWGObject>();
		QuadTree <SWGObject> tree = getTree(l);
		List <SWGObject> range;
		synchronized (tree) {
			range = tree.getWithinRange(l.getX(), l.getZ(), AWARE_RANGE);
		}
		for (SWGObject inRange : range) {
			if (isValidInRange(obj, inRange, l))
				objectAware.add(inRange);
		}
		obj.updateObjectAwareness(objectAware);
	}
	
	private void moveFromOld(SWGObject object, Location oldLocation, boolean update) {
		if (object.getParent() != null) {
			object.getParent().removeObject(object); // Moving from cell to world
			object.sendObserversAndSelf(new UpdateContainmentMessage(object.getObjectId(), 0, object.getSlotArrangement()));
		} else {
			removeFromLocation(object, oldLocation); // World to World
		}
		add(object);
		if (update)
			update(object);
	}
	
	private void removeFromLocation(SWGObject object, Location l) {
		if (invalidLocation(l))
			return;
		QuadTree <SWGObject> tree = getTree(l);
		synchronized (tree) {
			tree.remove(l.getX(), l.getZ(), object);
		}
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
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedOutPlayer())
			return false;
		int distSquared = distanceSquared(objLoc, inRange.getWorldLocation());
		int loadSquared = (int) (square(inRange.getLoadRange()) + 0.5);
		return (loadSquared != 0 || distSquared <= DEFAULT_LOAD_RANGE) && (loadSquared == 0 || distSquared <= loadSquared);
	}
	
	private int distanceSquared(Location l1, Location l2) {
		return (int) (square(l1.getX()-l2.getX()) + square(l1.getY()-l2.getY()) + square(l1.getZ()-l2.getZ()) + 0.5);
	}
	
	private static double square(double x) {
		return x * x;
	}
	
}