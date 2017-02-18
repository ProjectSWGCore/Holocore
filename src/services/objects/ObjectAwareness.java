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

import intents.PlayerEventIntent;
import intents.RequestZoneInIntent;
import intents.network.CloseConnectionIntent;
import intents.network.GalacticPacketIntent;
import intents.object.ContainerTransferIntent;
import intents.object.DestroyObjectIntent;
import intents.object.MoveObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.PlayerTransformedIntent;
import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.HeartBeat;
import network.packets.swg.zone.ParametersMessage;
import network.packets.swg.zone.chat.ChatOnConnectAvatar;
import network.packets.swg.zone.chat.VoiceChatStatus;
import network.packets.swg.zone.insertion.ChatServerStatus;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Point3D;
import resources.Terrain;
import resources.config.ConfigFile;
import resources.control.Assert;
import resources.control.Intent;
import resources.control.Service;
import resources.network.DisconnectReason;
import resources.objects.SWGObject;
import resources.objects.awareness.AwarenessHandler;
import resources.objects.awareness.DataTransformHandler;
import resources.objects.awareness.TerrainMap.TerrainMapCallback;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.player.PlayerEvent;
import resources.player.PlayerState;
import resources.server_info.Log;

public class ObjectAwareness extends Service implements TerrainMapCallback {
	
	private static final Location GONE_LOCATION = new Location(0, 0, 0, Terrain.GONE);
	
	private final AwarenessHandler awarenessHandler;
	private final DataTransformHandler dataTransformHandler;
	
	public ObjectAwareness() {
		awarenessHandler = new AwarenessHandler(this);
		dataTransformHandler = new DataTransformHandler();
		dataTransformHandler.setSpeedCheck(getConfig(ConfigFile.FEATURES).getBoolean("SPEED-HACK-CHECK", true));
		
		registerForIntent(PlayerEventIntent.class, pei -> handlePlayerEventIntent(pei));
		registerForIntent(ObjectCreatedIntent.class, oci -> handleObjectCreatedIntent(oci));
		registerForIntent(DestroyObjectIntent.class, doi -> handleDestroyObjectIntent(doi));
		registerForIntent(ObjectTeleportIntent.class, oti -> processObjectTeleportIntent(oti));
		registerForIntent(GalacticPacketIntent.class, gpi -> processGalacticPacketIntent(gpi));
		registerForIntent(MoveObjectIntent.class, moi -> processMoveObjectIntent(moi));
		registerForIntent(ContainerTransferIntent.class, cti -> processContainerTransferIntent(cti));
		registerForIntent(RequestZoneInIntent.class, rzii -> handleZoneIn(rzii.getCreature(), rzii.getPlayer(), rzii.getCreature().getLocation(), rzii.getCreature().getParent()));
	}
	
	@Override
	public boolean terminate() {
		awarenessHandler.close();
		return super.terminate();
	}
	
	@Override
	public void onWithinRange(SWGObject obj, SWGObject inRange) {
		Assert.notNull(obj);
		Assert.notNull(inRange);
		Assert.isNull(obj.getParent());
		Assert.isNull(inRange.getParent());
		Assert.notNull(obj.getTerrain());
		Assert.notNull(inRange.getTerrain());
		obj.addObjectAware(inRange);
	}
	
	@Override
	public void onOutOfRange(SWGObject obj, SWGObject outRange) {
		Assert.notNull(obj);
		Assert.notNull(outRange);
		obj.removeObjectAware(outRange);
	}
	
	@Override
	public void onMoveSuccess(SWGObject obj) {
		
	}
	
	@Override
	public void onMoveFailure(SWGObject obj) {
		Assert.notNull(obj);
		obj.clearObjectsAware();
	}

	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		switch (pei.getEvent()) {
			case PE_ZONE_IN_CLIENT:
				Assert.notNull(creature);
				moveObject(creature, creature.getParent(), creature.getLocation());
				break;
			case PE_DISAPPEAR:
				Assert.notNull(creature);
				disappearObject(creature, true, true);
				break;
			case PE_DESTROYED:
				Assert.notNull(creature);
				creature.setOwner(null);
				break;
			default:
				break;
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		Assert.notNull(obj);
		Assert.notNull(obj.getTerrain());
		moveObject(obj, obj.getParent(), obj.getLocation());
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		disappearObject(obj, true, true);
		obj.setLocation(GONE_LOCATION);
		obj.moveToContainer(null);
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject obj = oti.getObject();
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer()) {
			handleZoneIn((CreatureObject) obj, obj.getOwner(), oti.getNewLocation(), oti.getParent());
		} else {
			moveObject(obj, oti.getParent(), oti.getNewLocation());
		}
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent i) {
		Packet packet = i.getPacket();
		if (packet instanceof DataTransform) {
			handleDataTransform((DataTransform) packet, i.getObjectManager());
		} else if (packet instanceof DataTransformWithParent) {
			handleDataTransformWithParent((DataTransformWithParent) packet, i.getObjectManager());
		} else if (packet instanceof CmdSceneReady) {
			handleCmdSceneReady(i.getPlayer(), (CmdSceneReady) packet);
		}
	}
	
	private void processMoveObjectIntent(MoveObjectIntent i) {
		moveObjectWithTransform(i.getObject(), i.getParent(), i.getNewLocation(), i.getSpeed(), i.getUpdateCounter());
	}
	
	private void processContainerTransferIntent(ContainerTransferIntent i) {
		SWGObject obj = i.getObject();
		Assert.notNull(obj);
		Assert.notNull(obj.getTerrain());
		moveObject(obj, i.getContainer(), obj.getLocation());
	}
	
	private void handleZoneIn(CreatureObject creature, Player player, Location loc, SWGObject parent) {
		creature.setOwner(player);
		// Fresh login or teleport/travel
		PlayerState state = player.getPlayerState();
		if (state != PlayerState.LOGGED_IN && state != PlayerState.ZONED_IN) {
			new CloseConnectionIntent(player.getNetworkId(), DisconnectReason.APPLICATION).broadcast();
			return;
		}
		boolean firstZone = state == PlayerState.LOGGED_IN;
		player.setPlayerState(PlayerState.ZONING_IN);
		if (parent == null) {
			Log.i(this, "Zoning in %s with character %s to %s %s", player.getUsername(), player.getCharacterName(), loc.getPosition(), loc.getTerrain());
		} else {
			Assert.notNull(parent.getParent()); // Character must be in a cell, inside a building
			SWGObject superParent = parent.getSuperParent();
			Point3D world = superParent.getLocation().getPosition();
			Log.i(this, "Zoning in %s with character %s at %s in %s/%s %s", player.getUsername(), player.getCharacterName(), loc.getPosition(), superParent, world, superParent.getTerrain());
		}
		resetAwarenessOnZone(creature, firstZone);
		creature.setLocation(loc);
		creature.moveToContainer(parent);
		startZone(creature, firstZone);
		recursiveCreateObject(creature, player);
		if (parent != null) {
			for (SWGObject obj : creature.getObjectsAware()) {
				obj.createObject(creature);
				creature.createObject(obj);
			}
		}
		Assert.notNull(creature.getTerrain());
	}
	
	private void resetAwarenessOnZone(CreatureObject creature, boolean firstZone) {
		if (firstZone) {
			for (Player observer : creature.getObservers()) {
				creature.destroyObject(observer);
			}
			creature.resetAwareness();
			creature.clearCustomAware(false);
		} else {
			creature.clearObjectsAware();
		}
	}
	
	private void startZone(CreatureObject creature, boolean firstZone) {
		Player player = creature.getOwner();
		long time = ProjectSWG.getGalacticTime();
		int realTime = (int) (System.currentTimeMillis()/1E3);
		Intent firstZoneIntent = null;
		if (firstZone) {
			player.sendPacket(new HeartBeat());
			player.sendPacket(new ChatServerStatus(true));
			player.sendPacket(new VoiceChatStatus());
			player.sendPacket(new ParametersMessage());
			player.sendPacket(new ChatOnConnectAvatar());
			firstZoneIntent = new PlayerEventIntent(player, PlayerEvent.PE_FIRST_ZONE);
			firstZoneIntent.broadcast();
		}
		player.sendPacket(new CmdStartScene(true, creature.getObjectId(), creature.getRace(), creature.getWorldLocation(), time, realTime));
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_CLIENT).broadcastAfterIntent(firstZoneIntent);
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		Assert.test(player.getPlayerState() == PlayerState.ZONING_IN);
		player.setPlayerState(PlayerState.ZONED_IN);
		Log.i("ZoneService", "%s with character %s zoned in from %s", player.getUsername(), player.getCharacterName(), p.getSocketAddress());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_SERVER).broadcast();
		player.sendPacket(new CmdSceneReady());
	}
	
	private void recursiveCreateObject(SWGObject obj, Player owner) {
		SWGObject parent = obj.getParent();
		if (parent != null)
			recursiveCreateObject(parent, owner);
		else
			obj.createObject(owner);
	}
	
	private void handleDataTransform(DataTransform dt, ObjectManager objectManager) {
		SWGObject obj = objectManager.getObjectById(dt.getObjectId());
		Assert.test(obj instanceof CreatureObject);
		Location requestedLocation = new Location(dt.getLocation());
		requestedLocation.setTerrain(obj.getTerrain());
		moveObjectWithTransform(obj, null, requestedLocation, dt.getSpeed(), dt.getUpdateCounter());
	}
	
	private void handleDataTransformWithParent(DataTransformWithParent dt, ObjectManager objectManager) {
		SWGObject obj = objectManager.getObjectById(dt.getObjectId());
		SWGObject parent = objectManager.getObjectById(dt.getCellId());
		Assert.test(obj instanceof CreatureObject);
		if (parent == null) {
			Log.w(this, "Unknown data transform parent! Obj: %d/%s  Parent: %d", dt.getObjectId(), obj, dt.getCellId());
			return;
		}
		Location requestedLocation = new Location(dt.getLocation());
		requestedLocation.setTerrain(obj.getTerrain());
		moveObjectWithTransform(obj, parent, requestedLocation, dt.getSpeed(), dt.getUpdateCounter());
	}
	
	private void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		Assert.notNull(requestedLocation.getTerrain());
		if (parent == null)
			awarenessHandler.moveObject(obj, requestedLocation);
		else
			awarenessHandler.moveObject(obj, parent, requestedLocation);
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed, int update) {
		SWGObject oldParent = obj.getParent();
		Location oldLocation = obj.getLocation();
		moveObject(obj, parent, requestedLocation);
		if (parent == null)
			dataTransformHandler.handleMove(obj, speed, update);
		else
			dataTransformHandler.handleMove(obj, parent, speed, update);
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, oldParent, parent, oldLocation, requestedLocation).broadcast();
	}
	
	private void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		awarenessHandler.disappearObject(obj, disappearObjects, disappearCustom);
	}
	
}