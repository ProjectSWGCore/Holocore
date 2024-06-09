/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.support.objects;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.LookAtTarget;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuildoutLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.waypoint.WaypointObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ObjectStorageService extends Service {
	
	private final ScheduledThreadPool persistenceThread;
	private final Set<SWGObject> persistedObjects;
	private final Map<Long, SWGObject> objectMap;
	private final Map<Long, SWGObject> buildouts;
	private final Map<String, BuildingObject> buildingLookup;
	
	public ObjectStorageService() {
		this.persistenceThread = new ScheduledThreadPool(1, 3, "object-storage-service");
		this.persistedObjects = new CopyOnWriteArraySet<>();
		this.objectMap = new ConcurrentHashMap<>(256*1024, 0.8f, Runtime.getRuntime().availableProcessors());
		this.buildouts = new HashMap<>(128*1024, 1f);
		this.buildingLookup = new HashMap<>();
	}
	
	@Override
	public boolean initialize() {
		ObjectLookup.setObjectAuthority(this::getObjectById);
		BuildingLookup.setBuildingAuthority(buildingLookup::get);
		
		return initializeClientObjects() && initializeSavedObjects();
	}
	
	@Override
	public boolean start() {
		buildouts.values().forEach(obj -> new ObjectCreatedIntent(obj).broadcast());
		
		persistenceThread.start();
		persistenceThread.executeWithFixedDelay(TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5), this::saveObjects);
		return true;
	}
	
	@Override
	public boolean stop() {
		persistenceThread.stop();
		return persistenceThread.awaitTermination(1000);
	}
	
	@Override
	public boolean terminate() {
		ObjectLookup.setObjectAuthority(null);
		saveObjects();
		return true;
	}
	
	private boolean initializeSavedObjects() {
		long startTime = StandardLog.onStartLoad("server objects");
		List<MongoData> objectDocuments = PswgDatabase.INSTANCE.getObjects().getObjects();
		Map<Long, SWGObject> objects = new HashMap<>();
		for (MongoData doc : objectDocuments) {
			SWGObject obj = SWGObjectFactory.create(doc);
			objects.put(obj.getObjectId(), obj);
			if (obj instanceof PlayerObject) {
				for (WaypointObject waypoint : ((PlayerObject) obj).getWaypoints().values()) {
					objects.put(waypoint.getObjectId(), waypoint);
				}
			}
		}
		for (MongoData doc : objectDocuments) {
			long id = doc.getLong("id", 0);
			long parentId = doc.getLong("parent", 0);
			int cellNumber = doc.getInteger("parentCell", 0);
			assert id != 0;
			
			SWGObject obj = objects.get(id);
			if (parentId != 0) {
				SWGObject parent = objects.get(parentId);
				if (parent == null)
					parent = objectMap.get(parentId);
				
				if (parent instanceof BuildingObject) {
					if (cellNumber != 0)
						obj.moveToContainer(((BuildingObject) parent).getCellByNumber(cellNumber));
				} else {
					obj.moveToContainer(parent);
				}
			}
			objects.put(obj.getObjectId(), obj);
			if (obj.isPersisted())
				persistedObjects.add(obj);
		}
		
		objects.values().forEach(obj -> ObjectCreator.updateMaxObjectId(obj.getObjectId()));
		objects.values().forEach(obj -> new ObjectCreatedIntent(obj).broadcast());
		this.objectMap.putAll(objects);
		// TODO: Clear unreferenced objects from database
		StandardLog.onEndLoad(objects.size(), "server objects", startTime);
		return true;
	}
	
	private boolean initializeClientObjects() {
		long startTime = StandardLog.onStartLoad("client objects");
		BuildoutLoader loader = DataLoader.Companion.buildouts(createEventList());
		buildouts.putAll(loader.getObjects());
		objectMap.putAll(buildouts);
		buildingLookup.putAll(loader.getBuildings());
		StandardLog.onEndLoad(buildouts.size(), "client objects", startTime);
		return true;
	}
	
	private void saveObjects() {
		List<SWGObject> saveList = new ArrayList<>();
		persistedObjects.forEach(obj -> saveChildren(saveList, obj));
		PswgDatabase.INSTANCE.getObjects().addObjects(saveList);
	}
	
	private void saveChildren(Collection<SWGObject> saveList, @Nullable SWGObject obj) {
		if (obj == null)
			return;
		saveList.add(obj);
		
		obj.getChildObjects().forEach(child -> saveChildren(saveList, child));
	}
	
	@IntentHandler
	private void processObjectCreatedIntent(ObjectCreatedIntent intent) {
		SWGObject obj = intent.getObj();
		SWGObject replaced = objectMap.put(obj.getObjectId(), obj);
		if (replaced != null && replaced != obj)
			Log.e("Replaced object in object map! Old: %s  New: %s", replaced, obj);
		if (obj.isPersisted()) {
			if (persistedObjects.add(obj)) {
				List<SWGObject> saveList = new ArrayList<>();
				saveChildren(saveList, obj);
				PswgDatabase.INSTANCE.getObjects().addObjects(saveList);
			}
		}
	}
	
	@IntentHandler
	private void processDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObj();
		destroyObject(obj);
	}
	
	@IntentHandler
	private void processGalacticPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof LookAtTarget lookAtTarget) {
			CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
			long targetId = lookAtTarget.getTargetId();
			
			creatureObject.setLookAtTargetId(targetId);
		}
	}
	
	private SWGObject getObjectById(long objectId) {
		return objectMap.get(objectId);
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
		if (object.isPersisted())
			persistedObjects.remove(object);
		PswgDatabase.INSTANCE.getObjects().removeObject(object.getObjectId());
		objectMap.remove(object.getObjectId());
	}
	
	private List<String> createEventList() {
		List<String> events = new ArrayList<>();
		for (String event : PswgDatabase.INSTANCE.getConfig().getString(this, "events", "").split(",")) {
			if (event.isEmpty())
				continue;
			events.add(event.toLowerCase(Locale.US));
		}
		return events;
	}
	
	public static class ObjectLookup {
		
		private static final AtomicReference<Function<Long, SWGObject>> AUTHORITY = new AtomicReference<>(null);
		
		static void setObjectAuthority(Function<Long, SWGObject> authority) {
			AUTHORITY.set(authority);
		}
		
		public static boolean isDefined() {
			return AUTHORITY.get() != null;
		}
		
		@Nullable
		public static SWGObject getObjectById(long id) {
			return AUTHORITY.get().apply(id);
		}
		
	}
	
	public static class BuildingLookup {
		
		private static final AtomicReference<Function<String, BuildingObject>> AUTHORITY = new AtomicReference<>(null);
		
		static void setBuildingAuthority(Function<String, BuildingObject> authority) {
			AUTHORITY.set(authority);
		}
		
		@Nullable
		public static BuildingObject getBuildingByTag(String buildoutTag) {
			return AUTHORITY.get().apply(buildoutTag);
		}
		
	}
	
}
