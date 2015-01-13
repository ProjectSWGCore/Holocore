package services.objects;

import intents.GalacticPacketIntent;
import intents.PlayerEventIntent;
import intents.swgobject_events.SWGObjectEventIntent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import main.ProjectSWG;
import network.packets.swg.zone.HeartBeatMessage;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.insertion.SelectCharacter;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.ObjectController;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.ObjectData;
import resources.client_info.visitors.SlotArrangementData;
import resources.client_info.visitors.SlotDescriptorData;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.intangible.IntangibleObject;
import resources.objects.player.PlayerObject;
import resources.objects.quadtree.QuadTree;
import resources.objects.tangible.TangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.player.PlayerManager;

public class ObjectManager extends Manager {
	
	private static final double AWARE_RANGE = 200;
	
	private ClientFactory clientFac;
	
	private ObjectDatabase<SWGObject> objects;
	private Map <Terrain, QuadTree <SWGObject>> quadTree;
	private long maxObjectId;
	
	public ObjectManager() {
		objects = new ObjectDatabase<SWGObject>("odb/objects.db");
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
		maxObjectId = 1;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(SWGObjectEventIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(-8192, -8192, 8192, 8192));
		}
		objects.loadToCache();
		long startLoad = System.nanoTime();
		System.out.println("ObjectManager: Loading " + objects.size() + " objects from ObjectDatabase...");
		objects.traverseCache(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				obj.setOwner(null);
				Location l = obj.getLocation();
				if (l.getTerrain() != null) {
					QuadTree <SWGObject> tree = quadTree.get(l.getTerrain());
					if (tree != null) {
						tree.put(l.getX(), l.getZ(), obj);
					} else {
						System.err.println("ObjectManager: Unable to load QuadTree for object " + obj.getObjectId() + " and terrain: " + l.getTerrain());
					}
				}
				if (obj.getObjectId() >= maxObjectId) {
					maxObjectId = obj.getObjectId() + 1;
				}
			}
		});
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", objects.size(), loadTime);
		clientFac = new ClientFactory();

		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		objects.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				obj.setOwner(null);
			}
		});
		objects.save();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			if (gpi.getPacket() instanceof SelectCharacter) {
				zoneInCharacter(gpi.getPlayerManager(), gpi.getGalaxy().getName(), gpi.getNetworkId(), ((SelectCharacter)gpi.getPacket()).getCharacterId());
			} else if (gpi.getPacket() instanceof ObjectController) {
				ObjectController controller = (ObjectController) gpi.getPacket();
				if (controller.getControllerData() instanceof DataTransform) {
					DataTransform trans = (DataTransform) controller.getControllerData();
					SWGObject obj = getObject(controller.getObjectId());
					Location oldLocation = obj.getLocation();
					Location newLocation = trans.getLocation();
					newLocation.setTerrain(oldLocation.getTerrain());
					moveObject(trans, obj, oldLocation, newLocation);
				}
			}
		} else if (i instanceof PlayerEventIntent) {
			if (((PlayerEventIntent)i).getEvent() == PlayerEvent.PE_DISAPPEAR) {
				((PlayerEventIntent)i).getPlayer().getCreatureObject().clearAware();
			}
		}
	}
	
	public SWGObject getObject(long objectId) {
		synchronized (objects) {
			return objects.get(objectId);
		}
	}
	
	public SWGObject createObject(String template) {
		return createObject(template, null);
	}
	
	public SWGObject createObject(String template, Location l) {
		synchronized (objects) {
			long objectId = getNextObjectId();
			SWGObject obj = createObjectFromTemplate(objectId, template);
			if (obj == null) {
				System.err.println("ObjectManager: Unable to create object with template " + template);
				return null;
			}
			addObjectAttributes(obj, template);
			obj.setTemplate(template);
			obj.setLocation(l);
//			addToQuadtree(obj, l);
			moveObject(null, obj, null, l);
			objects.put(objectId, obj);
			return obj;
		}
	}
	
	private void moveObject(DataTransform transform, SWGObject obj, Location oldLocation, Location newLocation) {
		if (oldLocation != null && oldLocation.getTerrain() != null) { // Remove from QuadTree
			double x = oldLocation.getX();
			double y = oldLocation.getZ();
			quadTree.get(oldLocation.getTerrain()).remove(x, y, obj);
		}
		if (newLocation != null && newLocation.getTerrain() != null) { // Add to QuadTree, update awareness
			obj.setLocation(newLocation);
			updateAwarenessForObject(obj);
			quadTree.get(newLocation.getTerrain()).put(newLocation.getX(), newLocation.getZ(), obj);
		}
		if (transform != null)
			obj.sendDataTransforms(transform);
	}
	
	private void updateAwarenessForObject(SWGObject obj) {
		Location location = obj.getLocation();
		List <Player> updatedAware = new ArrayList<Player>();
		double x = location.getX();
		double y = location.getZ();
		QuadTree<SWGObject> tree = quadTree.get(location.getTerrain());
		for (SWGObject inRange : tree.getWithinRange(x, y, AWARE_RANGE)) {
			if (inRange.getOwner() != null && inRange.getObjectId() != obj.getObjectId()) {
				updatedAware.add(inRange.getOwner());
			}
		}
		obj.updateAwareness(updatedAware);
	}
	
	private void addObjectAttributes(SWGObject obj, String template) {
		ObjectData attributes = (ObjectData) clientFac.getInfoFromFile(ClientFactory.formatToSharedFile(template));
		
		obj.setStf((String) attributes.getAttribute(ObjectData.OBJ_STF));
		obj.setDetailStf((String) attributes.getAttribute(ObjectData.DETAIL_STF));
		obj.setVolume((Integer) attributes.getAttribute(ObjectData.VOLUME_LIMIT));
		
		addSlotsToObject(obj, attributes);
	}
	
	private void addSlotsToObject(SWGObject obj, ObjectData attributes) {

		if ((String) attributes.getAttribute(ObjectData.SLOT_DESCRIPTOR) != null) {
			// These are the slots that the object *HAS*
			SlotDescriptorData descriptor = (SlotDescriptorData) clientFac.getInfoFromFile((String) attributes.getAttribute(ObjectData.SLOT_DESCRIPTOR));
			
			for (String slotName : descriptor.getSlots()) {
				obj.addObjectSlot(slotName, null);
			}
		}
		
		if ((String) attributes.getAttribute(ObjectData.ARRANGEMENT_FILE) != null) {
			// This is what slots the object *USES*			
			SlotArrangementData arrangementData = (SlotArrangementData) clientFac.getInfoFromFile((String) attributes.getAttribute(ObjectData.ARRANGEMENT_FILE));
			
			obj.setArrangment(arrangementData.getArrangement());
		}
	}
	
	private void zoneInCharacter(PlayerManager playerManager, String galaxy, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player != null) {
			verifyPlayerObjectsSet(player, characterId);
			long objId = player.getCreatureObject().getObjectId();
			Race race = ((CreatureObject) player.getCreatureObject()).getRace();
			Location l = player.getCreatureObject().getLocation();
			long time = (long)(ProjectSWG.getCoreTime()/1E3);
			sendPacket(player, new HeartBeatMessage());
			sendPacket(player, new ChatServerStatus(true));
			sendPacket(player, new VoiceChatStatus());
			sendPacket(player, new ParametersMessage());
			sendPacket(player, new ChatOnConnectAvatar());
			sendPacket(player, new CmdStartScene(false, objId, race, l, time));
//			player.getCreatureObject().createObject(player);
			CreatureObject creature = (CreatureObject) player.getCreatureObject();
			player.sendPacket(new UpdatePvpStatusMessage(creature.getPvpType(), creature.getPvpFactionId(), creature.getObjectId()));
			creature.createObject(player);
			creature.clearAware();
			moveObject(null, creature, creature.getLocation(), creature.getLocation());
			updateAwarenessForObject(creature);
			new PlayerEventIntent(player, galaxy, PlayerEvent.PE_ZONE_IN).broadcast();
		}
	}
	
	private void verifyPlayerObjectsSet(Player player, long characterId) {
		if (player.getCreatureObject() != null && player.getPlayerObject() != null)
			return;
		SWGObject creature = objects.get(characterId);
		if (creature == null) {
			System.err.println("ObjectManager: Failed to start zone - CreatureObject could not be fetched from database");
			throw new NullPointerException("CreatureObject for ID: " + characterId + " cannot be null!");
		}
		player.setCreatureObject(creature); // CreatureObject contains the player object!
		creature.setOwner(player);
		
		if (player.getPlayerObject() == null) {
			System.err.println("FATAL: " + player.getUsername() + "'s CreatureObject has a null ghost!");
		}
	}
	
	private long getNextObjectId() {
		synchronized (objects) {
			return maxObjectId++;
		}
	}
	
	private String getFirstTemplatePart(String template) {
		int ind = template.indexOf('/');
		if (ind == -1)
			return "";
		return template.substring(0, ind);
	}
	
	private SWGObject createObjectFromTemplate(long objectId, String template) {
		if (!template.startsWith("object/"))
			return null;
		if (!template.endsWith(".iff"))
			return null;
		template = template.substring(7, template.length()-7-4);
		switch (getFirstTemplatePart(template)) {
			case "creature": return createCreatureObject(objectId, template);
			case "player": return createPlayerObject(objectId, template);
			case "tangible": return createTangibleObject(objectId, template);
			case "intangible": return createIntangibleObject(objectId, template);
			case "waypoint": return createWaypointObject(objectId, template);
			case "weapon": return createWeaponObject(objectId, template);
			case "building": break;
			case "cell": break;
		}
		return null;
	}
	
	private CreatureObject createCreatureObject(long objectId, String template) {
		return new CreatureObject(objectId);
	}
	
	private PlayerObject createPlayerObject(long objectId, String template) {
		return new PlayerObject(objectId);
	}
	
	private TangibleObject createTangibleObject(long objectId, String template) {
		return new TangibleObject(objectId);
	}
	
	private IntangibleObject createIntangibleObject(long objectId, String template) {
		return new IntangibleObject(objectId);
	}
	
	private WaypointObject createWaypointObject(long objectId, String template) {
		return new WaypointObject(objectId);
	}
	
	private WeaponObject createWeaponObject(long objectId, String template) {
		return new WeaponObject(objectId);
	}
	
	public SWGObject getObjectById(long id) {
		return objects.get(id);
	}
}
