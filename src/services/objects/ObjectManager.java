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

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.DeleteCharacterIntent;
import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.zone.SceneDestroyObject;
import network.packets.swg.zone.insertion.SelectCharacter;
import resources.Location;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
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
	
	public ObjectManager() {
		objectAwareness = new ObjectAwareness();
		mapManager = new MapManager();
		staticService = new StaticService();
		spawnerService = new SpawnerService(this);
		radialService = new RadialService();
		clientBuildoutService = new ClientBuildoutService();
		
		database = new CachedObjectDatabase<SWGObject>("odb/objects.db");
		objectMap = new Hashtable<>(16*1024);

		addChildService(objectAwareness);
		addChildService(mapManager);
		addChildService(staticService);
		addChildService(radialService);
		addChildService(spawnerService);
		addChildService(clientBuildoutService);
		
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
		registerForIntent(DeleteCharacterIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		loadClientObjects();
		loadObjects();
		return super.initialize();
	}
	
	private void loadObjects() {
		long startLoad = System.nanoTime();
		Log.i("ObjectManager", "Loading objects from ObjectDatabase...");
		System.out.println("ObjectManager: Loading objects from ObjectDatabase...");
		synchronized (database) {
			database.load();
			database.traverse(new Traverser<SWGObject>() {
				@Override
				public void process(SWGObject obj) {
					loadObject(obj);
				}
			});
		}
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i("ObjectManager", "Finished loading %d objects. Time: %fms", database.size(), loadTime);
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", database.size(), loadTime);
	}
	
	private void loadClientObjects() {
		Collection<SWGObject> objects = clientBuildoutService.loadClientObjects();
		for (SWGObject object : objects) {
			putObject(object);
			new ObjectCreatedIntent(object).broadcast();
		}
	}
	
	private void loadObject(SWGObject obj) {
		obj.setOwner(null);
		// if creature is not a player
		if (!(obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedOutPlayer()))
			objectAwareness.add(obj);
		putObject(obj);
		updateBuildoutParent(obj);
		addChildrenObjects(obj);
		new ObjectCreatedIntent(obj).broadcast();
	}
	
	private void updateBuildoutParent(SWGObject obj) {
		if (obj.getParent() != null) {
			if (!obj.getParent().isGenerated()) {
				long id = obj.getParent().getObjectId();
				obj.getParent().removeObject(obj);
				SWGObject parent = getObjectById(id);
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
			putObject(child);
			addChildrenObjects(child);
		}
	}
	
	@Override
	public boolean terminate() {
		synchronized (database) {
			database.traverse((obj) -> obj.setOwner(null));
			database.close();
		}
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processGalacticPacketIntent((GalacticPacketIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					processObjectCreatedIntent((ObjectCreatedIntent) i);
				break;
			case DestroyObjectIntent.TYPE:
				if (i instanceof DestroyObjectIntent)
					processDestroyObjectIntent((DestroyObjectIntent) i);
				break;
			case DeleteCharacterIntent.TYPE:
				if (i instanceof DeleteCharacterIntent)
					deleteObject(((DeleteCharacterIntent) i).getCreature().getObjectId());
				break;
		}
	}
	
	private void processObjectCreatedIntent(ObjectCreatedIntent intent) {
		putObject(intent.getObject());
	}
	
	private void processDestroyObjectIntent(DestroyObjectIntent doi) {
		destroyObject(doi.getObject());
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		if (packet instanceof SelectCharacter) {
			PlayerManager pm = gpi.getPlayerManager();
			long characterId = ((SelectCharacter) packet).getCharacterId();
			zoneInCharacter(pm, gpi.getNetworkId(), characterId);
		}
	}
	
	public SWGObject getObjectById(long objectId) {
		synchronized (objectMap) {
			return objectMap.get(objectId);
		}
	}
	
	public SWGObject deleteObject(long objId) {
		synchronized (database) {
			database.remove(objId);
		}
		synchronized (objectMap) {
			SWGObject obj = objectMap.remove(objId);
			if (obj == null)
				return null;
			obj.clearAware();
			objectAwareness.remove(obj);
			Log.v("ObjectManager", "Deleted object %d [%s]", obj.getObjectId(), obj.getTemplate());
			return obj;
		}
	}
	
	private void putObject(SWGObject object) {
		ObjectCreator.updateMaxObjectId(object.getObjectId());
		synchronized (objectMap) {
			objectMap.put(object.getObjectId(), object);
		}
	}
	
	public SWGObject destroyObject(long objectId) {
		SWGObject object = getObjectById(objectId);

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
		SWGObject obj = ObjectCreator.createObjectFromTemplate(template);
		if (obj == null) {
			System.err.println("ObjectManager: Unable to create object with template " + template);
			return null;
		}
		obj.setLocation(l);
		if (parent != null) {
			parent.addObject(obj);
		}
		synchronized (database) {
			if (addToDatabase)
				database.put(obj.getObjectId(), obj);
		}
		Log.v("ObjectManager", "Created object %d [%s]", obj.getObjectId(), obj.getTemplate());
		new ObjectCreatedIntent(obj).broadcast();
		return obj;
	}
	
	private void zoneInCharacter(PlayerManager playerManager, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player == null) {
			Log.e("ObjectManager", "Unable to zone in null player '%d'", netId);
			return;
		}
		SWGObject creatureObj = getObjectById(characterId);
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
		new RequestZoneInIntent(player, (CreatureObject) creatureObj, true).broadcast();
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
	
}
