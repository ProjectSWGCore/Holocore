package com.projectswg.holocore.services.support.objects;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.IntendedTarget;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.persistable.SWGObjectFactory;
import com.projectswg.holocore.resources.support.data.server_info.CachedObjectDatabase;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.ObjectDatabase;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.data.server_info.loader.buildouts.AreaLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.buildouts.BuildoutLoader;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class ObjectStorageService extends Service {
	
	private final ObjectDatabase<SWGObject> database;
	private final Map<Long, SWGObject> objectMap;
	private final Map<Long, SWGObject> buildouts;
	private final Set<AIObject> aiObjects;
	private final AtomicBoolean started;
	
	public ObjectStorageService() {
		this.database = new CachedObjectDatabase<>("odb/objects.db", SWGObjectFactory::create, SWGObjectFactory::save);
		this.objectMap = new ConcurrentHashMap<>(256*1024, 0.8f, Runtime.getRuntime().availableProcessors());
		this.buildouts = new ConcurrentHashMap<>(256*1024, 0.8f, 1);
		this.aiObjects = ConcurrentHashMap.newKeySet();
		this.started = new AtomicBoolean(false);
	}
	
	@Override
	public boolean initialize() {
		ObjectLookup.setAuthority(this);
		
		{
			long startTime = StandardLog.onStartLoad("client objects");
			buildouts.putAll(BuildoutLoader.load(AreaLoader.load(createEventList())).getObjects());
			objectMap.putAll(buildouts);
			StandardLog.onEndLoad(buildouts.size(), "client objects", startTime);
		}
		
		{
			long startTime = StandardLog.onStartLoad("players");
			synchronized (database) {
				if (!database.load() && database.fileExists())
					return false;
			}
			StandardLog.onEndLoad(database.size(), "players", startTime);
		}
		return true;
	}
	
	@Override
	public boolean start() {
		buildouts.values().forEach(ObjectCreatedIntent::broadcast);
		synchronized (database) {
			database.traverse(this::loadObject);
		}
		
		started.set(true);
		aiObjects.forEach(AIObject::aiStart);
		return true;
	}
	
	@Override
	public boolean stop() {
		started.set(false);
		aiObjects.forEach(AIObject::aiStop);
		return true;
	}
	
	@Override
	public boolean terminate() {
		synchronized (database) {
			database.close();
		}
		ObjectLookup.setAuthority(null);
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
	
	@IntentHandler
	private void processObjectCreatedIntent(ObjectCreatedIntent intent) {
		SWGObject obj = intent.getObject();
		putObject(obj);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isPlayer()) {
			synchronized (database) {
				if (database.add(obj))
					database.save();
			}
		}
		if (obj instanceof AIObject) {
			aiObjects.add((AIObject) obj);
			if (started.get())
				((AIObject) obj).aiStart();
		}
	}
	
	@IntentHandler
	private void processDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		destroyObject(obj);
		if (obj instanceof AIObject) {
			aiObjects.remove(obj);
			if (started.get())
				((AIObject) obj).aiStop();
		}
	}
	
	@IntentHandler
	private void processGalacticPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof IntendedTarget) {
			IntendedTarget intendedTarget = (IntendedTarget) packet;
			CreatureObject creatureObject = gpi.getPlayer().getCreatureObject();
			long targetId = intendedTarget.getTargetId();
			
			creatureObject.setIntendedTargetId(targetId);
			creatureObject.setLookAtTargetId(targetId);
		}
	}
	
	private SWGObject getObjectById(long objectId) {
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
	
	private static List<String> createEventList() {
		List<String> events = new ArrayList<>();
		for (String event : DataManager.getConfig(ConfigFile.FEATURES).getString("EVENTS", "").split(",")) {
			if (event.isEmpty())
				continue;
			events.add(event.toLowerCase(Locale.US));
		}
		return events;
	}
	
	public static class ObjectLookup {
		
		private static final AtomicReference<ObjectStorageService> AUTHORITY = new AtomicReference<>(null);
		
		private static void setAuthority(ObjectStorageService authority) {
			AUTHORITY.set(authority);
		}
		
		@Nullable
		public static SWGObject getObjectById(long id) {
			return AUTHORITY.get().getObjectById(id);
		}
		
	}
	
}
