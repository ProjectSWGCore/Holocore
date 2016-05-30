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
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.zone.insertion.SelectCharacter;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIObject;
import resources.persistable.SWGObjectFactory;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import services.map.MapManager;
import services.player.PlayerManager;
import services.spawn.SpawnerService;
import services.spawn.StaticService;
import utilities.Scripts;

public class ObjectManager extends Manager {
	
	private final ObjectAwareness objectAwareness;
	private final MapManager mapManager;
	private final StaticService staticService;
	private final SpawnerService spawnerService;
	private final RadialService radialService;
	private final ClientBuildoutService clientBuildoutService;

	private final ObjectDatabase<SWGObject> database;
	private final Map <Long, SWGObject> objectMap;
	private final AtomicBoolean started;
	
	public ObjectManager() {
		objectAwareness = new ObjectAwareness();
		mapManager = new MapManager();
		staticService = new StaticService();
		spawnerService = new SpawnerService(this);
		radialService = new RadialService();
		clientBuildoutService = new ClientBuildoutService();
		
		database = new CachedObjectDatabase<>("odb/objects.db", SWGObjectFactory::create, SWGObjectFactory::save);
		objectMap = new Hashtable<>(16*1024);
		started = new AtomicBoolean(false);
		
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
	}
	
	@Override
	public boolean initialize() {
		loadClientObjects();
		if (!loadObjects())
			return false;
		return super.initialize();
	}
	
	private boolean loadObjects() {
		long startLoad = System.nanoTime();
		Log.i("ObjectManager", "Loading objects from ObjectDatabase...");
		synchronized (database) {
			if (!database.load() && database.fileExists())
				return false;
			database.traverse((obj) -> loadObject(obj));
		}
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		Log.i("ObjectManager", "Finished loading %d objects. Time: %fms", database.size(), loadTime);
		return true;
	}
	
	private void loadClientObjects() {
		Collection<SWGObject> objects = clientBuildoutService.loadClientObjects();
		for (SWGObject object : objects) {
			new ObjectCreatedIntent(object).broadcast();
		}
	}
	
	private void loadObject(SWGObject obj) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isPlayer())
			Scripts.invoke("objects/load_creature", "onLoad", obj);
		
		updateBuildoutParent(obj);
		addChildrenObjects(obj);
	}
	
	private void updateBuildoutParent(SWGObject obj) {
		if (obj.getParent() != null) {
			long id = obj.getParent().getObjectId();
			SWGObject parent = getObjectById(id);
			obj.moveToContainer(parent);
			if (parent == null)
				Log.e("ObjectManager", "Parent for %s is null! ParentID: %d", obj, id);
		}
	}
	
	private void addChildrenObjects(SWGObject obj) {
		new ObjectCreatedIntent(obj).broadcast();
		for (SWGObject child : obj.getContainedObjects()) {
			addChildrenObjects(child);
		}
	}
	
	@Override
	public boolean start() {
		synchronized (objectMap) {
			for (SWGObject obj : objectMap.values()) {
				if (obj instanceof AIObject)
					((AIObject) obj).aiStart();
			}
			started.set(true);
		}
		return super.start();
	}
	
	@Override
	public boolean stop() {
		synchronized (objectMap) {
			started.set(false);
			for (SWGObject obj : objectMap.values()) {
				if (obj instanceof AIObject)
					((AIObject) obj).aiStop();
			}
		}
		return super.stop();
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
		}
	}
	
	private void processObjectCreatedIntent(ObjectCreatedIntent intent) {
		SWGObject obj = intent.getObject();
		putObject(obj);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isPlayer()) {
			synchronized (database) {
				if (database.add(obj))
					database.save();
			}
		}
		if (!(obj instanceof AIObject))
			return;
		synchronized (objectMap) {
			if (started.get())
				((AIObject) obj).aiStart();
		}
	}
	
	private void processDestroyObjectIntent(DestroyObjectIntent doi) {
		destroyObject(doi.getObject());
		if (!(doi.getObject() instanceof AIObject))
			return;
		synchronized (objectMap) {
			if (started.get())
				((AIObject) doi.getObject()).aiStop();
		}
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
	
	private void putObject(SWGObject object) {
		synchronized (objectMap) {
			SWGObject replaced = objectMap.put(object.getObjectId(), object);
			if (replaced != null)
				Log.e(this, "Replaced object in object map! Old: %s  New: %s", replaced, object);
		}
	}

	private SWGObject destroyObject(SWGObject object) {
		for (SWGObject slottedObj : object.getSlots().values()) {
			if (slottedObj != null)
				destroyObject(slottedObj);
		}
		
		for (SWGObject contained : object.getContainedObjects()) {
			if (contained != null)
				destroyObject(contained);
		}
		object.moveToContainer(null);
		objectAwareness.remove(object);
		object.clearAware();
		synchronized (database) {
			if (database.remove(object))
				database.save();
		}
		synchronized (objectMap) {
			objectMap.remove(object.getObjectId());
		}

		return object;
	}
	
	private void zoneInCharacter(PlayerManager playerManager, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player == null) {
			Log.e("ObjectManager", "Unable to zone in null player '%d'", netId);
			return;
		}
		SWGObject creatureObj = getObjectById(characterId);
		if (creatureObj == null) {
			Log.e("ObjectManager", "Failed to start zone - CreatureObject could not be fetched from database [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "You were not found in the database\nTry relogging to fix this problem", 10, TimeUnit.SECONDS);
			return;
		}
		if (!(creatureObj instanceof CreatureObject)) {
			Log.e("ObjectManager", "Failed to start zone - Object is not a CreatureObject [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "There has been an internal server error: Not a Creature.\nPlease delete your character and create a new one", 10, TimeUnit.SECONDS);
			return;
		}
		if (((CreatureObject) creatureObj).getPlayerObject() == null) {
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
