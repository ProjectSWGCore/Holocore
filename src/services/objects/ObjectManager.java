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

import intents.GalacticPacketIntent;
import intents.ObjectTeleportIntent;
import intents.PlayerEventIntent;
import intents.swgobject_events.SWGObjectEventIntent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import main.ProjectSWG;
import network.packets.Packet;
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
import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.Location;
import resources.Posture;
import resources.Race;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Manager;
import resources.objects.SWGObject;
import resources.objects.buildouts.BuildoutLoader;
import resources.objects.creature.CreatureMood;
import resources.objects.creature.CreatureObject;
import resources.objects.quadtree.QuadTree;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.CachedObjectDatabase;
import resources.server_info.ObjectDatabase;
import resources.server_info.ObjectDatabase.Traverser;
import services.player.PlayerManager;

public class ObjectManager extends Manager {
	
	private static final double AWARE_RANGE = 200;
	
	private final Map <Long, List <SWGObject>> buildoutObjects;
	private final ObjectDatabase<SWGObject> objects;
	private final Map <Terrain, QuadTree <SWGObject>> quadTree;
	private long maxObjectId;
	
	public ObjectManager() {
		buildoutObjects = new HashMap<Long, List<SWGObject>>();
		objects = new CachedObjectDatabase<SWGObject>("odb/objects.db");
		quadTree = new HashMap<Terrain, QuadTree<SWGObject>>();
		maxObjectId = 1;
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(SWGObjectEventIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		loadQuadTree();
		loadObjects();
		loadBuildouts();
		return super.initialize();
	}
	
	private void loadQuadTree() {
		for (Terrain t : Terrain.values()) {
			quadTree.put(t, new QuadTree<SWGObject>(-8192, -8192, 8192, 8192));
		}
	}
	
	private void loadObjects() {
		long startLoad = System.nanoTime();
		System.out.println("ObjectManager: Loading objects from ObjectDatabase...");
		objects.load();
		objects.traverse(new Traverser<SWGObject>() {
			@Override
			public void process(SWGObject obj) {
				loadObject(obj);
				if (obj.getObjectId() >= maxObjectId) {
					maxObjectId = obj.getObjectId() + 1;
				}
			}
		});
		double loadTime = (System.nanoTime() - startLoad) / 1E6;
		System.out.printf("ObjectManager: Finished loading %d objects. Time: %fms%n", objects.size(), loadTime);
	}
	
	private void loadBuildouts() {
		boolean enableBuildouts = false;
		if (enableBuildouts) {
			long startLoad = System.nanoTime();
			System.out.println("ObjectManager: Loading buildouts...");
			List <SWGObject> buildouts = null;
//			buildouts = BuildoutLoader.loadAllBuildouts();
			buildouts = BuildoutLoader.loadBuildoutsForTerrain(Terrain.CORELLIA);
			for (SWGObject obj : buildouts) {
				loadBuildout(obj);
			}
			double loadTime = (System.nanoTime() - startLoad) / 1E6;
			System.out.printf("ObjectManager: Finished loading buildouts. Time: %fms%n", loadTime);
		} else {
			System.out.println("ObjectManager: Buildouts not loaded. Reason: Disabled!");
		}
	}
	
	private void loadBuildout(SWGObject obj) {
		loadObject(obj);
		List <SWGObject> idCollisions = buildoutObjects.get(obj.getObjectId());
		if (idCollisions == null)
			buildoutObjects.put(obj.getObjectId(), idCollisions = new ArrayList<SWGObject>());
		boolean duplicate = false;
		for (SWGObject dup : idCollisions) {
			if (dup.getLocation().equals(obj.getLocation()) && dup.getTemplate().equals(obj.getTemplate())) {
				duplicate = true;
				break;
			}
		}
		if (!duplicate)
			idCollisions.add(obj);
	}
	
	private void loadObject(SWGObject obj) {
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
			processGalacticPacketIntent((GalacticPacketIntent) i);
		} else if (i instanceof PlayerEventIntent) {
			if (((PlayerEventIntent)i).getEvent() == PlayerEvent.PE_DISAPPEAR) {
				SWGObject obj = ((PlayerEventIntent)i).getPlayer().getCreatureObject();
				obj.setOwner(null);
				obj.clearAware();
			}
		}else if(i instanceof ObjectTeleportIntent){
			processObjectTeleportIntent((ObjectTeleportIntent) i);
		}
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
			
		removeFromQuadTree(object);
		object.setLocation(oti.getNewLocation());

		
		if(object instanceof CreatureObject && object.getOwner() != null){
			sendPacket(object.getOwner(), new CmdStartScene(false, object.getObjectId(), ((CreatureObject)object).getRace(), object.getLocation(), (long)(ProjectSWG.getCoreTime()/1E3)));
			((CreatureObject)object).createObject(object.getOwner());
			((CreatureObject)object).clearAware();
		}
		updateAwarenessForObject(object);
		addToQuadTree(object);
				
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
			}
		}
	}
	
	public SWGObject getObjectById(long objectId) {
		synchronized (objects) {
			return objects.get(objectId);
		}
	}
	
	public SWGObject deleteObject(long objId) {
		synchronized (objects) {
			SWGObject obj = objects.remove(objId);
			if (obj == null)
				return null;
			Location loc = obj.getLocation();
			if (loc != null && loc.getTerrain() != null) {
				QuadTree <SWGObject> tree = quadTree.get(loc.getTerrain());
				synchronized (tree) {
					tree.remove(loc.getX(), loc.getZ(), obj);
				}
			}
			for (SWGObject child : obj.getChildren())
				if (child != null)
					deleteObject(child.getObjectId());
			for (SWGObject slot : obj.getSlots().values())
				if (slot != null)
					deleteObject(slot.getObjectId());
			return obj;
		}
	}
	
	public SWGObject createObject(String template) {
		return createObject(template, null);
	}
	
	public SWGObject createObject(String template, Location l) {
		synchronized (objects) {
			long objectId = getNextObjectId();
			SWGObject obj = ObjectCreator.createObjectFromTemplate(objectId, template);
			if (obj == null) {
				System.err.println("ObjectManager: Unable to create object with template " + template);
				return null;
			}
			obj.setLocation(l);
			updateAwarenessForObject(obj);
			addToQuadTree(obj);
			objects.put(objectId, obj);
			return obj;
		}
	}
	
	private void addToQuadTree(SWGObject obj) {
		if (obj == null || obj instanceof WaypointObject)
			return;
		Location loc = obj.getLocation();
		if (loc == null || loc.getTerrain() == null)
			return;
		QuadTree <SWGObject> tree = quadTree.get(loc.getTerrain());
		synchronized (tree) {
			tree.put(loc.getX(), loc.getZ(), obj);
		}
	}
	
	private void removeFromQuadTree(SWGObject obj) {
		if (obj == null)
			return;
		Location loc = obj.getLocation();
		if (loc == null || loc.getTerrain() == null)
			return;
		double x = loc.getX();
		double y = loc.getZ();
		QuadTree <SWGObject> tree = quadTree.get(loc.getTerrain());
		synchronized (tree) {
			tree.remove(x, y, obj);
		}
	}
	
	private void moveObject(SWGObject obj, DataTransform transform) {
		if (transform == null)
			return;
		removeFromQuadTree(obj);
		Location newLocation = transform.getLocation();
		newLocation.setTerrain(obj.getLocation().getTerrain());
		obj.setLocation(newLocation);
		
		updateAwarenessForObject(obj);
		addToQuadTree(obj);
		
		if (obj instanceof CreatureObject && transform.getSpeed() > 1E-3) {
			if(((CreatureObject) obj).getPosture() == Posture.PRONE){
				((CreatureObject) obj).setPosture(Posture.PRONE);
			}else{
				((CreatureObject) obj).setPosture(Posture.UPRIGHT);
			}
			((CreatureObject) obj).sendObservers(new PostureUpdate(obj.getObjectId(), ((CreatureObject) obj).getPosture()));
		}
		obj.sendDataTransforms(transform);
	}
	
	private void updateAwarenessForObject(SWGObject obj) {
		Location location = obj.getLocation();
		if (location == null || location.getTerrain() == null)
			return;
		List <SWGObject> objectAware = new LinkedList<SWGObject>();
		double x = location.getX();
		double y = location.getZ();
		QuadTree<SWGObject> tree = quadTree.get(location.getTerrain());
		synchronized (tree) {
			List <SWGObject> range = tree.getWithinRange(x, y, AWARE_RANGE);
			for (SWGObject inRange : range) {
				if (inRange.getObjectId() != obj.getObjectId()) {
					if (inRange instanceof CreatureObject) {
						if (inRange.getOwner() != null)
							objectAware.add(inRange);
					} else
						objectAware.add(inRange);
				}
			}
		}
		obj.updateObjectAwareness(objectAware);
	}
	
	private void zoneInCharacter(PlayerManager playerManager, String galaxy, long netId, long characterId) {
		Player player = playerManager.getPlayerFromNetworkId(netId);
		if (player == null)
			return;
		player.setPlayerState(PlayerState.ZONING_IN);
		verifyPlayerObjectsSet(player, characterId);
		CreatureObject creature = player.getCreatureObject();
		creature.setMoodId(CreatureMood.NONE.getMood());
		player.getPlayerObject().setStartPlayTime((int) System.currentTimeMillis());
		long objId = creature.getObjectId();
		Race race = creature.getRace();
		Location l = creature.getLocation();
		long time = (long)(ProjectSWG.getCoreTime()/1E3);
		sendPacket(player, new HeartBeatMessage());
		sendPacket(player, new ChatServerStatus(true));
		sendPacket(player, new VoiceChatStatus());
		sendPacket(player, new ParametersMessage());
		sendPacket(player, new ChatOnConnectAvatar());
		sendPacket(player, new CmdStartScene(false, objId, race, l, time));
		sendPacket(player, new UpdatePvpStatusMessage(creature.getPvpType(), creature.getPvpFactionId(), creature.getObjectId()));
		creature.createObject(player);
		creature.clearAware();
		updateAwarenessForObject(creature);
		System.out.println("[" + player.getUsername() + "] " + player.getCharacterName() + " is zoning in");
		new PlayerEventIntent(player, galaxy, PlayerEvent.PE_ZONE_IN).broadcast();
	}
	
	private void verifyPlayerObjectsSet(Player player, long characterId) {
		if (player.getCreatureObject() != null && player.getPlayerObject() != null)
			return;
		SWGObject creature = objects.get(characterId);
		if (creature == null) {
			System.err.println("ObjectManager: Failed to start zone - CreatureObject could not be fetched from database [Character: " + characterId + "  User: " + player.getUsername() + "]");
			return;
		}
		if (!(creature instanceof CreatureObject)) {
			System.err.println("ObjectManager: Failed to start zone - Object is not a CreatureObject for ID " + characterId);
			return;
		}
		player.setCreatureObject((CreatureObject) creature);
		creature.setOwner(player);
		
		if (player.getPlayerObject() == null) {
			System.err.println("FATAL: " + player.getUsername() + "'s CreatureObject has a null ghost!");
		}
		
		player.getPlayerObject().setOwner(player);
	}
	
	private long getNextObjectId() {
		synchronized (objects) {
			return maxObjectId++;
		}
	}

}
