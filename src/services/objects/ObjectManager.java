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

import intents.RequestZoneInIntent;
import intents.network.GalacticPacketIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import network.packets.Packet;
import network.packets.swg.ErrorMessage;
import network.packets.swg.zone.insertion.SelectCharacter;
import network.packets.swg.zone.object_controller.IntendedTarget;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIObject;
import resources.persistable.SWGObjectFactory;
import resources.player.Player;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.Log;
import resources.server_info.ObjectDatabase;
import resources.server_info.StandardLog;
import services.map.MapManager;
import services.player.PlayerManager;
import services.spawn.SpawnerService;
import services.spawn.StaticService;

import com.projectswg.common.control.Manager;

public class ObjectManager extends Manager {
	
	private final ObjectAwareness objectAwareness;
	private final MapManager mapManager;
	private final StaticService staticService;
	private final SpawnerService spawnerService;
	private final RadialService radialService;
	private final ClientBuildoutService clientBuildoutService;
	private final StaticItemService staticItemService;

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
		staticItemService = new StaticItemService();
		
		database = new CachedObjectDatabase<>("odb/objects.db", SWGObjectFactory::create, SWGObjectFactory::save);
		objectMap = new Hashtable<>(16*1024);
		started = new AtomicBoolean(false);
		
		addChildService(objectAwareness);
		addChildService(mapManager);
		addChildService(staticService);
		addChildService(radialService);
		addChildService(spawnerService);
		addChildService(clientBuildoutService);
		addChildService(staticItemService);
		
		registerForIntent(GalacticPacketIntent.class, gpi -> processGalacticPacketIntent(gpi));
		registerForIntent(ObjectCreatedIntent.class, oci -> processObjectCreatedIntent(oci));
		registerForIntent(DestroyObjectIntent.class, doi -> processDestroyObjectIntent(doi));
	}
	
	@Override
	public boolean initialize() {
		synchronized (objectMap) {
			objectMap.putAll(clientBuildoutService.loadClientObjects());
		}
		if (!loadObjects())
			return false;
		synchronized (database) {
			database.traverse((obj) -> loadObject(obj));
		}
		synchronized (objectMap) {
			objectMap.forEach((id, obj) -> new ObjectCreatedIntent(obj).broadcast());
		}
		return super.initialize();
	}
	
	private boolean loadObjects() {
		long startTime = StandardLog.onStartLoad("players");
		synchronized (database) {
			if (!database.load() && database.fileExists())
				return false;
		}
		StandardLog.onEndLoad(database.size(), "players", startTime);
		return true;
	}
	
	private void loadObject(SWGObject obj) {
		updateBuildoutParent(obj);
		addChildrenObjects(obj);
	}
	
	private void updateBuildoutParent(SWGObject obj) {
		if (obj.getParent() != null) {
			long id = obj.getParent().getObjectId();
			SWGObject parent = getObjectById(id);
			if (obj.getParent() instanceof CellObject && obj.getParent().getParent() != null) {
				BuildingObject building = (BuildingObject) obj.getParent().getParent();
				parent = ((BuildingObject) getObjectById(building.getObjectId())).getCellByNumber(((CellObject) obj.getParent()).getNumber());
			}
			obj.moveToContainer(parent);
			if (parent == null)
				Log.e("Parent for %s is null! ParentID: %d", obj, id);
		}
	}
	
	private void addChildrenObjects(SWGObject obj) {
		putObject(obj);
		for (SWGObject child : obj.getSlots().values()) {
			if (child != null)
				addChildrenObjects(child);
		}
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
			database.close();
		}
		return super.terminate();
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
			zoneInCharacter(pm, gpi.getPlayer(), characterId);
		} else if (packet instanceof IntendedTarget) {
			IntendedTarget intendedTarget = (IntendedTarget) packet;
			CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
			long targetId = intendedTarget.getTargetId();
			
			creatureObject.setIntendedTargetId(targetId);
			creatureObject.setLookAtTargetId(targetId);
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
			if (replaced != null && replaced != object)
				Log.e("Replaced object in object map! Old: %s  New: %s", replaced, object);
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
		synchronized (database) {
			if (database.remove(object))
				database.save();
		}
		synchronized (objectMap) {
			objectMap.remove(object.getObjectId());
		}

		return object;
	}
	
	private void zoneInCharacter(PlayerManager playerManager, Player player, long characterId) {
		SWGObject creatureObj = getObjectById(characterId);
		if (creatureObj == null) {
			Log.e("Failed to start zone - CreatureObject could not be fetched from database [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "You were not found in the database\nTry relogging to fix this problem", 10, TimeUnit.SECONDS);
			return;
		}
		if (!(creatureObj instanceof CreatureObject)) {
			Log.e("Failed to start zone - Object is not a CreatureObject [Character: %d  User: %s]", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "There has been an internal server error: Not a Creature.\nPlease delete your character and create a new one", 10, TimeUnit.SECONDS);
			return;
		}
		if (((CreatureObject) creatureObj).getPlayerObject() == null) {
			Log.e("Failed to start zone - CreatureObject doesn't have a ghost [Character: %d  User: %s", characterId, player.getUsername());
			sendClientFatal(player, "Failed to zone", "There has been an internal server error: Null Ghost.\nPlease delete your character and create a new one", 10, TimeUnit.SECONDS);
			return;
		}
		new RequestZoneInIntent(player, (CreatureObject) creatureObj).broadcast();
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
