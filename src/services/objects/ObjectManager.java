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

import intents.ObjectTeleportIntent;
import intents.PlayerEventIntent;
import intents.ZoneInIntent;
import intents.network.GalacticPacketIntent;

import java.util.*;

import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.insertion.SelectCharacter;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.ObjectController;
import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.Location;
import resources.Posture;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.buildouts.BuildoutLoader;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Config;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.map.MapService;
import services.player.PlayerManager;

public class ObjectManager extends Manager {

	private final MapService mapService;

	private final Map <Long, List <SWGObject>> buildoutObjects;
	private final ObjectDatabase<SWGObject> objects;
	private final ObjectAwareness objectAwareness;
	private long maxObjectId;
	
	public ObjectManager() {
		mapService = new MapService();
		buildoutObjects = new HashMap<Long, List<SWGObject>>();
		objects = new CachedObjectDatabase<SWGObject>("odb/objects.db");
		objectAwareness = new ObjectAwareness();
		maxObjectId = 1;
	}
	
	@Override
	public boolean initialize() {
		addChildService(mapService);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		objectAwareness.initialize();
		loadObjects();
		loadBuildouts();
		return super.initialize();
	}
	
	private void loadObjects() {
		long startLoad = System.nanoTime();
		Log.i("ObjectManager", "Loading objects from ObjectDatabase...");
		System.out.println("ObjectManager: Loading objects from ObjectDatabase...");
		objects.load();
		objects.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				loadObject(obj);
				if (obj.getObjectId() >= maxObjectId) {
					maxObjectId = obj.getObjectId() + 1;
				}
			}
		});
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i("ObjectManager", "Finished loading %d objects. Time: %fms", objects.size(), loadTime);
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", objects.size(), loadTime);
	}
	
	private void loadBuildouts() {
		Config c = getConfig(ConfigFile.PRIMARY);
		if (c.getBoolean("LOAD-BUILDOUTS", false)) {
			long startLoad = System.nanoTime();
			System.out.println("ObjectManager: Loading buildouts...");
			Log.i("ObjectManager", "Loading buildouts...");
			List <SWGObject> buildouts = null;
			String terrain = c.getString("LOAD-BUILDOUTS-FOR", "");
			if (Terrain.doesTerrainExistForName(terrain))
				buildouts = BuildoutLoader.loadBuildoutsForTerrain(Terrain.getTerrainFromName(terrain));
			else {
				if (!terrain.isEmpty()) {
					System.err.println("ObjectManager: Unknown terrain '" + terrain + "'");
					Log.e("ObjectManager", "Unknown terrain: %s", terrain);
				}
				buildouts = BuildoutLoader.loadAllBuildouts();
			}
			for (SWGObject obj : buildouts) {
				loadBuildout(obj);
			}
			double loadTime = (System.nanoTime() - startLoad) / 1E6;
			System.out.printf("ObjectManager: Finished loading buildouts. Time: %fms%n", loadTime);
			Log.i("ObjectManager", "Finished loading buildouts. Time: %fms", loadTime);
		} else {
			Log.w("ObjectManager", "Did not load buildouts. Reason: Disabled.");
			System.out.println("ObjectManager: Buildouts not loaded. Reason: Disabled!");
		}
	}
	
	private void loadBuildout(SWGObject obj) {
		loadObject(obj);
		List <SWGObject> idCollisions = buildoutObjects.get(obj.getObjectId());
		if (idCollisions == null)
			buildoutObjects.put(obj.getObjectId(), idCollisions = new LinkedList<SWGObject>());
		boolean duplicate = false;
		for (SWGObject dup : idCollisions) {
			if (dup.getLocation().equals(obj.getLocation()) && dup.getTemplate().equals(obj.getTemplate())) {
				duplicate = true;
				break;
			}
		}
		if (!duplicate) {
			idCollisions.add(obj);
			if (obj instanceof TangibleObject) {
				objectAwareness.add(obj);
			}
		}

		mapService.addMapLocation(obj, MapService.MapType.STATIC);
	}
	
	private void loadObject(SWGObject obj) {
		obj.setOwner(null);
		objectAwareness.add(obj);
	}
	
	@Override
	public boolean terminate() {
		objects.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				obj.setOwner(null);
			}
		});
		objects.close();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			processGalacticPacketIntent((GalacticPacketIntent) i);
		} else if (i instanceof PlayerEventIntent) {
			Player p = ((PlayerEventIntent)i).getPlayer();
			switch (((PlayerEventIntent)i).getEvent()) {
				case PE_DISAPPEAR:
					p.getCreatureObject().clearAware();
					objectAwareness.remove(p.getCreatureObject());
					break;
				case PE_ZONE_IN:
					p.getCreatureObject().clearAware();
					objectAwareness.update(p.getCreatureObject());
					break;
				default:
					break;
			}
		}else if(i instanceof ObjectTeleportIntent){
			processObjectTeleportIntent((ObjectTeleportIntent) i);
		}
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		objectAwareness.move(object, oti.getNewLocation());
		
		if (object instanceof CreatureObject && object.getOwner() != null){
			sendPacket(object.getOwner(), new CmdStartScene(false, object.getObjectId(), ((CreatureObject)object).getRace(), object.getLocation(), (long)(ProjectSWG.getCoreTime()/1E3)));
			((CreatureObject)object).createObject(object.getOwner());
			((CreatureObject)object).clearAware();
		}
	}

	private void processGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		if (packet instanceof SelectCharacter) {
			PlayerManager pm = gpi.getPlayerManager();
			String galaxy = gpi.getGalaxy().getName();
			long characterId = ((SelectCharacter) packet).getCharacterId();
			zoneInCharacter(pm, galaxy, gpi.getNetworkId(), characterId);
		} else if (packet instanceof ObjectController) {
			if (packet instanceof DataTransform) {
				DataTransform trans = (DataTransform) packet;
				SWGObject obj = getObjectById(trans.getObjectId());
				moveObject(obj, trans);
			}
		}
	}
	
	public SWGObject getObjectById(long objectId) {
		synchronized (objects) {
			return objects.get(objectId);
		}
	}
	
	public SWGObject deleteObject(long objId) {
		synchronized (objects) {
			SWGObject obj = objects.remove(objId);
			if (obj == null)
				return null;
			obj.clearAware();
			objectAwareness.remove(obj);
			Log.i("ObjectManager", "Deleted object %d [%s]", obj.getObjectId(), obj.getTemplate());
			return obj;
		}
	}

	public SWGObject destroyObject(long objectId) {
		SWGObject object = objects.get(objectId);

		return (object != null ? destroyObject(object) : null);
	}

	public SWGObject destroyObject(SWGObject object) {

		long objId = object.getObjectId();

		for (SWGObject slottedObj : object.getSlots().values()) {
			if (slottedObj != null)
				destroyObject(slottedObj);
		}

		Iterator<SWGObject> containerIterator = object.getContainedObjects().iterator();
		while(containerIterator.hasNext()) {
			SWGObject containedObject = containerIterator.next();
			if (containedObject != null)
				destroyObject(containedObject);
		}

		// Remove object from the parent
		SWGObject parent = object.getParent();
		if (parent != null) {
			if (parent instanceof CreatureObject) {
				((CreatureObject) parent).removeEquipment(object);
			}
			object.sendObserversAndSelf(new SceneDestroyObject(objId));

			parent.removeObject(object);
		} else {
			object.sendObservers(new SceneDestroyObject(objId));
		}

		// Finally, remove from the awareness tree
		deleteObject(object.getObjectId());

		return object;
	}

	public SWGObject createObject(String template) {
		return createObject(template, null);
	}
	
	public SWGObject createObject(String template, boolean addToAwareness) {
		return createObject(template, null, addToAwareness);
	}
	
	public SWGObject createObject(String template, Location l) {
		return createObject(template, l, true);
	}
	
	public SWGObject createObject(String template, Location l, boolean addToAwareness) {
		synchronized (objects) {
			long objectId = getNextObjectId();
			SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
			if (obj == null) {
				System.err.println("ObjectManager: Unable to create object with template " + template);
				return null;
			}
			obj.setLocation(l);
			if (addToAwareness) {
				objectAwareness.add(obj);
			}
			objects.put(objectId, obj);
			Log.i("ObjectManager", "Created object %d [%s]", obj.getObjectId(), obj.getTemplate());
			return obj;
		}
	}
	
	private void moveObject(SWGObject obj, DataTransform transform) {
		if (transform == null)
			return;
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getLocation().getTerrain());
		objectAwareness.move(obj, newLocation);
		
		if (obj instanceof CreatureObject && transform.getSpeed() > 1E-3) {
			if(((CreatureObject) obj).getPosture() == Posture.PRONE){
				((CreatureObject) obj).setPosture(Posture.PRONE);
			}else{
				((CreatureObject) obj).setPosture(Posture.UPRIGHT);
			}
			((CreatureObject) obj).sendObserversAndSelf(new PostureUpdate(obj.getObjectId(), ((CreatureObject) obj).getPosture()));
		}
		obj.sendDataTransforms(transform);
	}
	
	private void zoneInCharacter(PlayerManager playerManager, String galaxy, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player == null)
			return;
		SWGObject creatureObj = objects.get(characterId);
		if (creatureObj == null) {
			System.err.println("ObjectManager: Failed to start zone - CreatureObject could not be fetched from database [Character: " + characterId + "  User: " + player.getUsername() + "]");
			Log.e("ObjectManager", "Failed to start zone - CreatureObject could not be fetched from database [Character: %d  User: %s]", characterId, player.getUsername());
			return;
		}
		if (!(creatureObj instanceof CreatureObject)) {
			System.err.println("ObjectManager: Failed to start zone - Object is not a CreatureObject for ID " + characterId);
			Log.e("ObjectManager", "Failed to start zone - Object is not a CreatureObject [Character: %d  User: %s]", characterId, player.getUsername());
			return;
		}
		if (((CreatureObject) creatureObj).getPlayerObject() == null) {
			System.err.println("ObjectManager: Failed to start zone - " + player.getUsername() + "'s CreatureObject has a null ghost!");
			Log.e("ObjectManager", "Failed to start zone - CreatureObject doesn't have a ghost [Character: %d  User: %s", characterId, player.getUsername());
			return;
		}
		objectAwareness.add(creatureObj);
		new ZoneInIntent(player, (CreatureObject) creatureObj, galaxy).broadcast();
	}
	
	private long getNextObjectId() {
		synchronized (objects) {
			return maxObjectId++;
		}
	}

}
