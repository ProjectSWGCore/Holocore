/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.services.objects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.projectswg.common.control.Manager;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget;

import com.projectswg.holocore.intents.network.GalacticPacketIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.building.BuildingObject;
import com.projectswg.holocore.resources.objects.cell.CellObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.custom.AIObject;
import com.projectswg.holocore.resources.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.server_info.CachedObjectDatabase;
import com.projectswg.holocore.resources.server_info.ObjectDatabase;
import com.projectswg.holocore.resources.server_info.StandardLog;
import com.projectswg.holocore.services.map.MapManager;
import com.projectswg.holocore.services.spawn.SpawnerService;
import com.projectswg.holocore.services.spawn.StaticService;

import javax.annotation.CheckForNull;

public class ObjectManager extends Manager {
	
	private final ClientBuildoutService clientBuildoutService;
	
	private final ObjectDatabase<SWGObject> database;
	private final Map <Long, SWGObject> objectMap;
	private final AtomicBoolean started;
	
	public ObjectManager() {
		clientBuildoutService = new ClientBuildoutService();
		
		database = new CachedObjectDatabase<>("odb/objects.db", SWGObjectFactory::create, SWGObjectFactory::save);
		objectMap = new ConcurrentHashMap<>(256*1024, 0.8f, Runtime.getRuntime().availableProcessors());
		started = new AtomicBoolean(false);
		
		addChildService(new ObjectAwareness());
		addChildService(new MapManager());
		addChildService(new StaticService());
		addChildService(new RadialService());
		addChildService(new SpawnerService());
		addChildService(clientBuildoutService);
		addChildService(new StaticItemService());
		
		registerForIntent(GalacticPacketIntent.class, this::processGalacticPacketIntent);
		registerForIntent(ObjectCreatedIntent.class, this::processObjectCreatedIntent);
		registerForIntent(DestroyObjectIntent.class, this::processDestroyObjectIntent);
	}
	
	@Override
	public boolean initialize() {
		ObjectLookup.setObjectManager(this);
		
		clientBuildoutService.getClientObjects().forEach(obj -> objectMap.put(obj.getObjectId(), obj));
		
		if (!loadObjects())
			return false;
		synchronized (database) {
			database.traverse(this::loadObject);
		}
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		objectMap.forEach((id, obj) -> {
			if (obj instanceof AIObject)
				((AIObject) obj).aiStart();
		});
		
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
		ObjectCreatedIntent.broadcast(obj);
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

	private void destroyObject(SWGObject object) {
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
	}
	
	public static class ObjectLookup {
		
		private static final AtomicReference<ObjectManager> OBJECT_MANAGER = new AtomicReference<>(null);
		
		private static void setObjectManager(ObjectManager objManager) {
			OBJECT_MANAGER.set(objManager);
		}
		
		@CheckForNull
		public static SWGObject getObjectById(long id) {
			return OBJECT_MANAGER.get().getObjectById(id);
		}
		
	}
	
}
