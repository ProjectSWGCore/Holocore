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
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.object.ObjectCreateIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectIdRequestIntent;
import intents.object.ObjectIdResponseIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.DeleteCharacterIntent;
import intents.player.PlayerTransformedIntent;
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
import resources.buildout.BuildoutArea;
import resources.containers.ContainerPermissions;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.map.MapManager;
import services.player.PlayerManager;
import services.spawn.SpawnerService;
import services.spawn.StaticService;

public class ObjectManager extends Manager {
	
	private final ObjectAwareness objectAwareness;
	private final MapManager mapManager;
	private final StaticService staticService;
	private final SpawnerService spawnerService;
	private final RadialService radialService;
	private final ClientBuildoutService clientBuildoutService;

	private final ObjectDatabase<SWGObject> database;
	private final Map <Long, SWGObject> objectMap;
	private long maxObjectId;
	
	public ObjectManager() {
		objectAwareness = new ObjectAwareness();
		mapManager = new MapManager();
		staticService = new StaticService(this);
		spawnerService = new SpawnerService(this);
		radialService = new RadialService();
		clientBuildoutService = new ClientBuildoutService();
		
		database = new CachedObjectDatabase<SWGObject>("odb/objects.db");
		objectMap = new Hashtable<>(16*1024);
		maxObjectId = 1;

		addChildService(objectAwareness);
		addChildService(mapManager);
		addChildService(staticService);
		addChildService(radialService);
		addChildService(spawnerService);
		addChildService(clientBuildoutService);
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(ObjectIdRequestIntent.TYPE);
		registerForIntent(ObjectCreateIntent.TYPE);
		registerForIntent(DeleteCharacterIntent.TYPE);
		boolean init = super.initialize();
		loadClientObjects();
		loadObjects();
		return init;
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
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i("ObjectManager", "Finished loading %d objects. Time: %fms", database.size(), loadTime);
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", database.size(), loadTime);
	}
	
	private void loadClientObjects() {
		long start = System.nanoTime();
		for (SWGObject obj : clientBuildoutService.loadClientObjects().values()) {
			synchronized (objectMap) {
				if (obj.getObjectId() >= maxObjectId) {
					maxObjectId = obj.getObjectId() + 1;
				}
			}
			objectMap.put(obj.getObjectId(), obj);
			new ObjectCreatedIntent(obj).broadcast();
		}
		double loadTime = (System.nanoTime() - start) / 1E6;
		System.out.printf("ClientObjectLoader: Finished loading client objects. Time: %fms%n", loadTime);
		Log.i("ClientObjectLoader", "Finished loading client objects. Time: %fms", loadTime);
	}
	
	private void loadObject(SWGObject obj) {
		obj.setOwner(null);
		// if player is not a player
		if (!(obj instanceof CreatureObject && ((CreatureObject) obj).hasSlot("ghost")))
			objectAwareness.add(obj);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).getPlayerObject() != null) {
			if (!obj.hasSlot("bank")) {
				SWGObject missing = createObject(obj, "object/tangible/bank/shared_character_bank.iff", false);
				missing.setContainerPermissions(ContainerPermissions.INVENTORY);
			}
			
			if (!obj.hasSlot("mission_bag")) {
				SWGObject missing = createObject(obj, "object/tangible/mission_bag/shared_mission_bag.iff", false);
				missing.setContainerPermissions(ContainerPermissions.INVENTORY);
			}
				
			if (!obj.hasSlot("appearance_inventory")) {
				SWGObject missing = createObject(obj, "object/tangible/inventory/shared_appearance_inventory.iff", false);
				missing.setContainerPermissions(ContainerPermissions.INVENTORY);
			}
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
		database.traverse((obj) -> obj.setOwner(null));
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
					if (p.getCreatureObject() == null)
						break;
					p.getCreatureObject().clearAware();
					for (SWGObject obj : p.getCreatureObject().getObservers())
						p.getCreatureObject().destroyObject(obj.getOwner());
					p.getCreatureObject().setOwner(null);
					p.setCreatureObject(null);
					break;
				case PE_FIRST_ZONE:
					if (p.getCreatureObject().getParent() == null)
						p.getCreatureObject().createObject(p);
					break;
				case PE_ZONE_IN:
					p.getCreatureObject().clearAware();
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
		} else if (i instanceof DeleteCharacterIntent) {
			deleteObject(((DeleteCharacterIntent) i).getCreature().getObjectId());
		}
	}

	private void processObjectCreateIntent(ObjectCreateIntent intent) {
		SWGObject object = intent.getObject();
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
		if (object instanceof CreatureObject && object.getOwner() != null) {
			Packet startScene = new CmdStartScene(false, object.getObjectId(), ((CreatureObject)object).getRace(), oti.getNewLocation(), (long)(ProjectSWG.getCoreTime()/1E3));
			if (oti.getParent() != null) {
				objectAwareness.move(object, oti.getParent(), oti.getNewLocation());
				sendPacket(object.getOwner(), startScene);
			} else {
				objectAwareness.move(object, oti.getNewLocation());
				sendPacket(object.getOwner(), startScene);
				object.createObject(object.getOwner());
			}
			new PlayerEventIntent(object.getOwner(), PlayerEvent.PE_ZONE_IN).broadcast();
		} else {
			object.setLocation(oti.getNewLocation());
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
	
	public SWGObject createObject(SWGObject parent, String template) {
		return createObject(parent, template, null);
	}
	
	public SWGObject createObject(String template, Location l) {
		return createObject(template, l, true);
	}
	
	public SWGObject createObject(SWGObject parent, String template, boolean addToDatabase) {
		return createObject(parent, template, null, addToDatabase);
	}
	
	public SWGObject createObject(SWGObject parent, String template, Location l) {
		return createObject(parent, template, l, true);
	}
	
	public SWGObject createObject(String template, Location l, boolean addToDatabase) {
		return createObject(null, template, l, addToDatabase);
	}
	
	public SWGObject createObject(SWGObject parent, String template, Location l, boolean addToDatabase) {
		synchronized (objectMap) {
			long objectId = getNextObjectId();
			SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
			if (obj == null) {
				System.err.println("ObjectManager: Unable to create object with template " + template);
				return null;
			}
			obj.setLocation(l);
			objectMap.put(objectId, obj);
			if (parent != null) {
				parent.addObject(obj);
			}
			if (addToDatabase) {
				database.put(objectId, obj);
			}
			Log.i("ObjectManager", "Created object %d [%s]", obj.getObjectId(), obj.getTemplate());
			new ObjectCreatedIntent(obj).broadcast();
			return obj;
		}
	}
	
	private void moveObject(SWGObject obj, DataTransform transform) {
		if (transform == null)
			return;
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getTerrain());
		BuildoutArea area = obj.getBuildoutArea();
		if (area != null && area.isAdjustCoordinates())
			newLocation.translatePosition(area.getX1(), 0, area.getZ1());
		if (obj instanceof CreatureObject)
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), null, obj.getLocation(), newLocation).broadcast();
		objectAwareness.move(obj, newLocation);
		if (area != null && area.isAdjustCoordinates())
			newLocation.translatePosition(-area.getX1(), 0, -area.getZ1());
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
		if (obj instanceof CreatureObject)
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), newLocation).broadcast();
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
		if (creatureObj.getParent() != null) {
			objectAwareness.update(creatureObj);
			Log.d("ObjectManager", "Zoning in to %s/%s - %s", creatureObj.getParent(), creatureObj.getTerrain(), creatureObj.getLocation().getPosition());
		}else {
			objectAwareness.remove(creatureObj);
			objectAwareness.add(creatureObj);
			Log.d("ObjectManager", "Zoning in to %s - %s", creatureObj.getTerrain(), creatureObj.getLocation().getPosition());
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
