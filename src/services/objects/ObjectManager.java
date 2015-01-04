package services.objects;

import intents.GalacticPacketIntent;
import intents.swgobject_events.SWGObjectEventIntent;

import java.util.ArrayList;
import java.util.Arrays;
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
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(-5000, -5000, 5000, 5000));
		}
		objects.loadToCache();
		long startLoad = System.nanoTime();
		System.out.println("ObjectManager: Loading " + objects.size() + " objects from ObjectDatabase...");
		objects.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
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
		objects.save();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof GalacticPacketIntent) {
			GalacticPacketIntent gpi = (GalacticPacketIntent) i;
			if (gpi.getPacket() instanceof SelectCharacter) {
				zoneInCharacter(gpi.getPlayerManager(), gpi.getNetworkId(), ((SelectCharacter)gpi.getPacket()).getCharacterId());
			} else if (gpi.getPacket() instanceof ObjectController) {
				ObjectController controller = (ObjectController) gpi.getPacket();
				if (controller.getControllerData() instanceof DataTransform) {
					DataTransform trans = (DataTransform) controller.getControllerData();
					SWGObject obj = getObject(controller.getObjectId());
					Location oldLocation = obj.getLocation();
					Location newLocation = trans.getLocation();
					newLocation.setTerrain(oldLocation.getTerrain());
					moveObject(obj, oldLocation, newLocation);
				}
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
			addToQuadtree(obj, l);
//			moveObject(obj, null, l);
			objects.put(objectId, obj);
			return obj;
		}
	}
	
	// Adds an object to the quadtree and refreshes awareness lists in the object added as well as adds the Player to objects in range (if there is one)
	private void addToQuadtree(SWGObject obj, Location loc) {
		if (loc == null || loc.isNaN()) // Object is inside a container or it's a slot of an item
			return;

		double x = loc.getX(), z = loc.getY();
		
		List<Player> awarePlayers = new ArrayList<Player>();

		QuadTree<SWGObject> tree = quadTree.get(loc.getTerrain());
		for (SWGObject inRange : tree.getWithinRange(x, z, AWARE_RANGE)) {
			if (inRange != null && inRange.getOwner() != null && inRange.getObjectId() != obj.getObjectId()) {
				if (obj.getOwner() != null)
					inRange.updateAwareness(obj.getOwner(), true);
				
				awarePlayers.add(inRange.getOwner());
			}
		}
		if (awarePlayers.size() > 0)
			obj.updateAwareness(awarePlayers);
		
		quadTree.get(loc.getTerrain()).put(x, z, obj);
	}
	
	private void moveObject(SWGObject obj, Location oldLocation, Location newLocation) {
		double x = 0, y = 0;
		List <Player> updatedAware = new ArrayList<Player>();
		if (oldLocation != null && oldLocation.getTerrain() != null) { // Remove from QuadTree
			x = oldLocation.getX();
			y = oldLocation.getZ();
			quadTree.get(oldLocation.getTerrain()).remove(x, y, obj);
		}
		if (newLocation != null && newLocation.getTerrain() != null) { // Add to QuadTree, update awareness
			obj.setLocation(newLocation);
			x = newLocation.getX();
			y = newLocation.getZ();
			QuadTree<SWGObject> tree = quadTree.get(newLocation.getTerrain());
			for (SWGObject inRange : tree.getWithinRange(x, y, AWARE_RANGE))
				if (inRange.getOwner() != null && inRange.getObjectId() != obj.getObjectId())
					updatedAware.add(inRange.getOwner());
			quadTree.get(newLocation.getTerrain()).put(x, y, obj);
		}
		System.out.println(obj.getName() + " - " + Arrays.toString(updatedAware.toArray()));
		obj.updateAwareness(updatedAware);
		obj.sendDataTransforms();
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
	
	private void zoneInCharacter(PlayerManager playerManager, long netId, long characterId) {
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
