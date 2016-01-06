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
import intents.network.GalacticPacketIntent;
import intents.object.ObjectCreateIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.PlayerTransformedIntent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.zone.UpdateContainmentMessage;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.buildout.BuildoutArea;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.quadtree.QuadTree;
import resources.objects.staticobject.StaticObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.Log;

public class ObjectAwareness extends Service {
	
	private static final double AWARE_RANGE = 1024;
	private static final int DEFAULT_LOAD_RANGE = (int) square(200); // Squared for speed
	
	private final Map <Terrain, QuadTree <SWGObject>> quadTree;
	
	public ObjectAwareness() {
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectCreateIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		loadQuadTree();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			case ObjectCreateIntent.TYPE:
				if (i instanceof ObjectCreateIntent)
					handleObjectCreateIntent((ObjectCreateIntent) i);
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
			case PE_FIRST_ZONE:
				break;
			case PE_ZONE_IN:
				creature.clearAware();
				startScene(creature, creature.getLocation());
				update(creature);
				break;
			default:
				break;
		}
	}
	
	private void handleObjectCreateIntent(ObjectCreateIntent oci) {
		SWGObject obj = oci.getObject();
		if (isInAwareness(obj))
			add(obj);
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		if (isInAwareness(obj))
			add(obj);
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		Player owner = object.getOwner();
		boolean creature = object instanceof CreatureObject && owner != null;
		Location old = object.getLocation();
		object.setLocation(oti.getNewLocation());
		if (oti.getParent() != null) {
			if (creature)
				startScene((CreatureObject) object, oti.getNewLocation());
			move(object, oti.getParent(), oti.getNewLocation());
		} else {
			if (creature)
				startScene((CreatureObject) object, oti.getNewLocation());
			moveFromOld(object, old);
		}
		new PlayerEventIntent(object.getOwner(), PlayerEvent.PE_ZONE_IN).broadcast();
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent i) {
		Packet packet = i.getPacket();
		if (packet instanceof DataTransform) {
			DataTransform trans = (DataTransform) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			moveObject(obj, trans);
		} else if (packet instanceof DataTransformWithParent) {
			DataTransformWithParent transformWithParent = (DataTransformWithParent) packet;
			SWGObject object = i.getObjectManager().getObjectById(transformWithParent.getObjectId());
			SWGObject parent = i.getObjectManager().getObjectById(transformWithParent.getCellId());
			moveObject(object, parent, transformWithParent);
		}
	}
	
	private void startScene(CreatureObject object, Location newLocation) {
		long time = (long) (ProjectSWG.getCoreTime() / 1E3);
		Race race = ((CreatureObject)object).getRace();
		sendPacket(object.getOwner(), new CmdStartScene(false, object.getObjectId(), race, newLocation, time, (int)(System.currentTimeMillis()/1E3)));
		recursiveCreateObject(object, object.getOwner());
	}
	
	private void recursiveCreateObject(SWGObject obj, Player p) {
		SWGObject parent = obj.getParent();
		if (parent != null)
			recursiveCreateObject(parent, p);
		obj.createObject(p);
	}
	
	private void moveObject(SWGObject obj, DataTransform transform) {
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		BuildoutArea area = obj.getBuildoutArea();
		if (area != null && area.isAdjustCoordinates())
			newLocation.translatePosition(area.getX1(), 0, area.getZ1());
		if (obj instanceof CreatureObject)
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), null, obj.getLocation(), newLocation).broadcast();
		move(obj, newLocation);
		if (area != null && area.isAdjustCoordinates())
			newLocation.translatePosition(-area.getX1(), 0, -area.getZ1());
		obj.sendDataTransforms(transform);

		// TODO: State checks before sending a data transform message to ensure the move is valid
	}
	
	private void moveObject(SWGObject obj, SWGObject parent, DataTransformWithParent transformWithParent) {
		Location newLocation = transformWithParent.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		if (parent == null) {
			System.err.println("ObjectManager: Could not find parent for transform! Cell: " + transformWithParent.getCellId());
			Log.e("ObjectManager", "Could not find parent for transform! Cell: %d  Object: %s", transformWithParent.getCellId(), obj);
			return;
		}
		if (obj instanceof CreatureObject)
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), newLocation).broadcast();
		move(obj, parent, newLocation);
		obj.sendParentDataTransforms(transformWithParent);
	}
	
	private void loadQuadTree() {
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(16, -8192, -8192, 8192, 8192));
		}
	}
	
	private boolean isInAwareness(SWGObject object) {
		return object.getParent() == null && !(object instanceof StaticObject);
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
		removeFromLocation(object, object.getLocation());
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
	
	private void moveFromOld(SWGObject object, Location oldLocation) {
		if (object.getParent() != null) {
			object.getParent().removeObject(object); // Moving from cell to world
			object.sendObserversAndSelf(new UpdateContainmentMessage(object.getObjectId(), 0, object.getSlotArrangement()));
		} else {
			removeFromLocation(object, oldLocation); // World to World
		}
		add(object);
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
		if (obj.hasSlot("ghost") && obj.getOwner() == null)
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