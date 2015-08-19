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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.object.ObjectCreateIntent;
import intents.object.ObjectIdRequestIntent;
import intents.object.ObjectIdResponseIntent;
import intents.object.ObjectTeleportIntent;
import intents.PlayerEventIntent;
import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.insertion.SelectCharacter;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import network.packets.swg.zone.object_controller.ObjectController;
import resources.Location;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.buildouts.BuildoutLoader;
import resources.objects.buildouts.SnapshotLoader;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Config;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.map.MapManager;
import services.player.PlayerManager;
import services.spawn.StaticService;

public class ObjectManager extends Manager {

	private final MapManager mapService;
	private final StaticService staticService;
	private final RadialService radialService;

	private final ObjectDatabase<SWGObject> database;
	private final ObjectAwareness objectAwareness;
	private final Map <Long, SWGObject> objectMap;
	private long maxObjectId;
	
	public ObjectManager() {
		mapService = new MapManager();
		staticService = new StaticService(this);
		radialService = new RadialService();
		database = new CachedObjectDatabase<SWGObject>("odb/objects.db");
		objectAwareness = new ObjectAwareness();
		objectMap = new HashMap<>();
		maxObjectId = 1;

		addChildService(mapService);
		addChildService(staticService);
		addChildService(radialService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(ObjectIdRequestIntent.TYPE);
		objectAwareness.initialize();
		loadClientObjects();
		maxObjectId = 1000000000; // Gets over all the buildouts/snapshots
		loadObjects();
		return super.initialize();
	}
	
	private void loadObjects() {
		long startLoad = System.nanoTime();
		Log.i("ObjectManager", "Loading objects from ObjectDatabase...");
		System.out.println("ObjectManager: Loading objects from ObjectDatabase...");
		database.load();
		database.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				loadObject(obj);
				if (obj.getObjectId() >= maxObjectId) {
					maxObjectId = obj.getObjectId() + 1;
				}
			}
		});
		for (SWGObject obj : new ArrayList<>(objectMap.values())) {
			staticService.createSupportingObjects(obj);
		}
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i("ObjectManager", "Finished loading %d objects. Time: %fms", database.size(), loadTime);
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", database.size(), loadTime);
	}
	
	private void loadClientObjects() {
		Config c = getConfig(ConfigFile.PRIMARY);
		if (c.getBoolean("LOAD-OBJECTS", true)) {
			String terrainStr = c.getString("LOAD-OBJECTS-FOR", "");
			Terrain terrain = null;
			if (Terrain.doesTerrainExistForName(terrainStr))
				terrain = Terrain.getTerrainFromName(terrainStr);
			else if (!terrainStr.isEmpty()) {
				System.err.println("ObjectManager: Unknown terrain '" + terrain + "'");
				Log.e("ObjectManager", "Unknown terrain: %s", terrain);
			}
			loadBuildouts(terrain);
			loadSnapshots(terrain);
		} else {
			Log.w("ObjectManager", "Did not load client objects. Reason: Disabled.");
			System.out.println("ObjectManager: Did not load client objects. Reason: Disabled!");
		}
	}
	
	private void loadBuildouts(Terrain terrain) {
		long startLoad = System.nanoTime();
		System.out.println("ObjectManager: Loading buildouts...");
		Log.i("ObjectManager", "Loading buildouts...");
		BuildoutLoader loader = new BuildoutLoader();
		if (terrain != null)
			loader.loadBuildoutsForTerrain(terrain);
		else
			loader.loadAllBuildouts();
		List <SWGObject> buildouts = loader.getObjects();
		for (SWGObject obj : buildouts) {
			loadBuildout(obj);
		}
		objectMap.putAll(loader.getObjectTable());
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		System.out.printf("ObjectManager: Finished loading %d buildouts. Time: %fms%n", buildouts.size(), loadTime);
		Log.i("ObjectManager", "Finished loading buildouts. Time: %fms", loadTime);
	}
	
	private void loadSnapshots(Terrain terrain) {
		long startLoad = System.nanoTime();
		System.out.println("ObjectManager: Loading snapshots...");
		Log.i("ObjectManager", "Loading snapshots...");
		SnapshotLoader loader = new SnapshotLoader();
		if (terrain != null)
			loader.loadSnapshotsForTerrain(terrain);
		else
			loader.loadAllSnapshots();
		List <SWGObject> snapshots = loader.getObjects();
		for (SWGObject obj : snapshots) {
			loadSnapshot(obj);
		}
		objectMap.putAll(loader.getObjectTable());
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		System.out.printf("ObjectManager: Finished loading %d snapshots. Time: %fms%n", snapshots.size(), loadTime);
		Log.i("ObjectManager", "Finished loading snapshots. Time: %fms", loadTime);
	}
	
	private void loadBuildout(SWGObject obj) {
		if (obj instanceof TangibleObject || obj instanceof BuildingObject) {
			objectAwareness.add(obj);
		}
		if (obj.getObjectId() >= maxObjectId) {
			maxObjectId = obj.getObjectId() + 1;
		}
		mapService.addMapLocation(obj, MapManager.MapType.STATIC);
	}
	
	private void loadSnapshot(SWGObject obj) {
		if (obj instanceof TangibleObject || obj instanceof BuildingObject) {
			objectAwareness.add(obj);
		}
		if (obj.getObjectId() >= maxObjectId) {
			maxObjectId = obj.getObjectId() + 1;
		}
		mapService.addMapLocation(obj, MapManager.MapType.STATIC);
	}
	
	private void loadObject(SWGObject obj) {
		obj.setOwner(null);
		// if player is not a player
		if (!(obj instanceof CreatureObject && ((CreatureObject) obj).hasSlot("ghost")))
			objectAwareness.add(obj);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).getPlayerObject() != null) {
			if (!obj.hasSlot("bank"))
				obj.addObject(createObject("object/tangible/bank/shared_character_bank.iff", false));
			if (!obj.hasSlot("mission_bag"))
				obj.addObject(createObject("object/tangible/mission_bag/shared_mission_bag.iff", false));
		}
		objectMap.put(obj.getObjectId(), obj);
		updateBuildoutParent(obj);
		addChildrenObjects(obj);
	}
	
	private void updateBuildoutParent(SWGObject obj) {
		if (obj.getParent() != null) {
			if (obj.getParent().isBuildout()) {
				long id = obj.getParent().getObjectId();
				obj.getParent().removeObject(obj);
				SWGObject parent = objectMap.get(id);
				if (parent != null)
					parent.addObject(obj);
				else {
					System.err.println("Parent for " + obj + " is null! ParentID: " + id);
					Log.e("ObjectManager", "Parent for %s is null! ParentID: %d", obj, id);
				}
			} else {
				updateBuildoutParent(obj.getParent());
			}
		}
	}
	
	private void addChildrenObjects(SWGObject obj) {
		for (SWGObject child : obj.getContainedObjects()) {
			objectMap.put(child.getObjectId(), child);
			addChildrenObjects(child);
		}
	}
	
	@Override
	public boolean terminate() {
		database.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				obj.setOwner(null);
			}
		});
		database.close();
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
					for (SWGObject obj : p.getCreatureObject().getObservers())
						p.getCreatureObject().destroyObject(obj.getOwner());
					break;
				case PE_FIRST_ZONE:
					if (p.getCreatureObject().getParent() == null)
						p.getCreatureObject().createObject(p);
					break;
				case PE_ZONE_IN:
					p.getCreatureObject().clearAware();
					objectAwareness.update(p.getCreatureObject());
					break;
				default:
					break;
			}
		} else if (i instanceof ObjectTeleportIntent) {
			processObjectTeleportIntent((ObjectTeleportIntent) i);
		} else if (i instanceof ObjectIdRequestIntent) {
			processObjectIdRequestIntent((ObjectIdRequestIntent) i);
		} else if (i instanceof ObjectCreateIntent) {
			processObjectCreateIntent((ObjectCreateIntent) i);
		}
	}

	private void processObjectCreateIntent(ObjectCreateIntent intent) {
		SWGObject object = intent.getObject();

		if (intent.isAddToAwareness()) {
			objectAwareness.add(object);
		}

		objectMap.put(object.getObjectId(), object);
	}

	private void processObjectIdRequestIntent(ObjectIdRequestIntent intent) {
		List<Long> reservedIds = new ArrayList<>();
		for (int i = 0; i < intent.getAmount(); i++) {
			reservedIds.add(getNextObjectId());
		}

		new ObjectIdResponseIntent(intent.getIdentifier(), reservedIds).broadcast();
	}

	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		objectAwareness.move(object, oti.getNewLocation());
		
		if (object instanceof CreatureObject && object.getOwner() != null){
			sendPacket(object.getOwner(), new CmdStartScene(false, object.getObjectId(), ((CreatureObject)object).getRace(), object.getLocation(), (long)(ProjectSWG.getCoreTime()/1E3)));
			object.createObject(object.getOwner());
			new PlayerEventIntent(object.getOwner(), PlayerEvent.PE_ZONE_IN).broadcast();
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
			} else if (packet instanceof DataTransformWithParent) {
				DataTransformWithParent transformWithParent = (DataTransformWithParent) packet;
				SWGObject object = getObjectById(transformWithParent.getObjectId());
				moveObject(object, transformWithParent);
			}
		}
	}
	
	public SWGObject getObjectById(long objectId) {
		synchronized (objectMap) {
			return objectMap.get(objectId);
		}
	}
	
	public SWGObject deleteObject(long objId) {
		synchronized (objectMap) {
			SWGObject obj = objectMap.remove(objId);
			database.remove(objId);
			if (obj == null)
				return null;
			obj.clearAware();
			objectAwareness.remove(obj);
			Log.i("ObjectManager", "Deleted object %d [%s]", obj.getObjectId(), obj.getTemplate());
			return obj;
		}
	}

	public SWGObject destroyObject(long objectId) {
		SWGObject object = objectMap.get(objectId);

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
		return createObject(template, l, addToAwareness, true);
	}
	
	public SWGObject createObject(String template, Location l, boolean addToAwareness, boolean addToDatabase) {
		synchronized (objectMap) {
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
			objectMap.put(objectId, obj);
			if (addToDatabase) {
				database.put(objectId, obj);
			}
			Log.i("ObjectManager", "Created object %d [%s]", obj.getObjectId(), obj.getTemplate());
			return obj;
		}
	}
	
	private void moveObject(SWGObject obj, DataTransform transform) {
		if (transform == null)
			return;
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		objectAwareness.move(obj, newLocation);
		obj.sendDataTransforms(transform);

		// TODO: State checks before sending a data transform message to ensure the move is valid/change speed depending
		// on the active state (mainly for CreatureObject, override sendDataTransforms in the class?)
	}
	
	private void moveObject(SWGObject obj, DataTransformWithParent transformWithParent) {
		Location newLocation = transformWithParent.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		SWGObject parent = objectMap.get(transformWithParent.getCellId());
		if (parent == null) {
			System.err.println("ObjectManager: Could not find parent for transform! Cell: " + transformWithParent.getCellId());
			Log.e("ObjectManager", "Could not find parent for transform! Cell: %d  Object: %s", transformWithParent.getCellId(), obj);
			return;
		}
		objectAwareness.move(obj, parent, newLocation);
		obj.sendParentDataTransforms(transformWithParent);
	}
	
	private void zoneInCharacter(PlayerManager playerManager, String galaxy, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player == null) {
			Log.e("ObjectManager", "Unable to zone in null player '%d'", netId);
			return;
		}
		SWGObject creatureObj = objectMap.get(characterId);
		if (creatureObj == null) {
			System.err.println("ObjectManager: Failed to start zone - CreatureObject could not be fetched from database [Character: " + characterId + "  User: " + player.getUsername() + "]");
			Log.e("ObjectManager", "Failed to start zone - CreatureObject could not be fetched from database [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "You were not found in the database\nTry relogging to fix this problem", 10, TimeUnit.SECONDS);
			return;
		}
		if (!(creatureObj instanceof CreatureObject)) {
			System.err.println("ObjectManager: Failed to start zone - Object is not a CreatureObject for ID " + characterId);
			Log.e("ObjectManager", "Failed to start zone - Object is not a CreatureObject [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "There has been an internal server error: Not a Creature.\nPlease delete your character and create a new one", 10, TimeUnit.SECONDS);
			return;
		}
		if (((CreatureObject) creatureObj).getPlayerObject() == null) {
			System.err.println("ObjectManager: Failed to start zone - " + player.getUsername() + "'s CreatureObject has a null ghost!");
			Log.e("ObjectManager", "Failed to start zone - CreatureObject doesn't have a ghost [Character: %d  User: %s", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "There has been an internal server error: Null Ghost.\nPlease delete your character and create a new one", 10, TimeUnit.SECONDS);
			return;
		}
		if (creatureObj.getParent() != null)
			objectAwareness.update(creatureObj);
		else {
			objectAwareness.remove(creatureObj);
			objectAwareness.add(creatureObj);
		}
		new RequestZoneInIntent(player, (CreatureObject) creatureObj, galaxy).broadcast();
	}
	
	private void sendClientFatal(Player player, String title, String message, long timeToRead, TimeUnit time) {
		player.sendPacket(new ErrorMessage(title, message, false));
		ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
		service.schedule(new Runnable() {
			@Override
			public void run() {
				player.sendPacket(new ErrorMessage(title, message, true));
				service.shutdownNow();
			}
		}, timeToRead, time);
	}
	
	private long getNextObjectId() {
		synchronized (objectMap) {
			return maxObjectId++;
		}
	}

}
