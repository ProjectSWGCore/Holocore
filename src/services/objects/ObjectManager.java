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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget;

import intents.network.GalacticPacketIntent;
import intents.object.DestroyObjectIntent;
import intents.object.ObjectCreatedIntent;
import resources.objects.SWGObject;
import resources.objects.building.BuildingObject;
import resources.objects.cell.CellObject;
import resources.objects.creature.CreatureObject;
import resources.objects.custom.AIObject;
import resources.persistable.SWGObjectFactory;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import resources.server_info.StandardLog;
import services.map.MapManager;
import services.spawn.SpawnerService;
import services.spawn.StaticService;

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
		spawnerService = new SpawnerService();
		radialService = new RadialService();
		clientBuildoutService = new ClientBuildoutService();
		staticItemService = new StaticItemService();
		
		database = new CachedObjectDatabase<>("odb/objects.db", SWGObjectFactory::create, SWGObjectFactory::save);
		objectMap = new ConcurrentHashMap<>(128*1024);
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
		ObjectLookup.setObjectManager(this);
		objectMap.putAll(clientBuildoutService.loadClientObjects());
		if (!loadObjects())
			return false;
		synchronized (database) {
			database.traverse((obj) -> loadObject(obj));
		}
		objectMap.forEach((id, obj) -> new ObjectCreatedIntent(obj).broadcast());
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		for (SWGObject obj : objectMap.values()) {
			if (obj instanceof AIObject)
				((AIObject) obj).aiStart();
		}
		started.set(true);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		started.set(false);
		for (SWGObject obj : objectMap.values()) {
			if (obj instanceof AIObject)
				((AIObject) obj).aiStop();
		}
		return super.stop();
	}
	
	@Override
	public boolean terminate() {
		synchronized (database) {
			database.close();
		}
		ObjectLookup.setObjectManager(null);
		return super.terminate();
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
		if (started.get())
			((AIObject) obj).aiStart();
	}
	
	private void processDestroyObjectIntent(DestroyObjectIntent doi) {
		destroyObject(doi.getObject());
		if (!(doi.getObject() instanceof AIObject))
			return;
		if (started.get())
			((AIObject) doi.getObject()).aiStop();
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent gpi) {
		SWGPacket SWGPacket = gpi.getPacket();
		if (SWGPacket instanceof IntendedTarget) {
			IntendedTarget intendedTarget = (IntendedTarget) SWGPacket;
			CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
			long targetId = intendedTarget.getTargetId();
			
			creatureObject.setIntendedTargetId(targetId);
			creatureObject.setLookAtTargetId(targetId);
		}
	}
	
	public SWGObject getObjectById(long objectId) {
		return objectMap.get(objectId);
	}
	
	private void putObject(SWGObject object) {
		SWGObject replaced = objectMap.put(object.getObjectId(), object);
		if (replaced != null && replaced != object)
			Log.e("Replaced object in object map! Old: %s  New: %s", replaced, object);
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
		objectMap.remove(object.getObjectId());
		
		return object;
	}
	
	public static class ObjectLookup {
		
		private static final AtomicReference<ObjectManager> OBJECT_MANAGER = new AtomicReference<>(null);
		
		private static void setObjectManager(ObjectManager objManager) {
			OBJECT_MANAGER.set(objManager);
		}
		
		public static SWGObject getObjectById(long id) {
			return OBJECT_MANAGER.get().getObjectById(id);
		}
		
	}
	
}
