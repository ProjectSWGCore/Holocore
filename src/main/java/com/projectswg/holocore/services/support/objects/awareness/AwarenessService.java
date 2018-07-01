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
package com.projectswg.holocore.services.support.objects.awareness;

import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.CmdSceneReady;
import com.projectswg.common.network.packets.swg.zone.HeartBeat;
import com.projectswg.common.network.packets.swg.zone.ParametersMessage;
import com.projectswg.common.network.packets.swg.zone.UpdateContainmentMessage;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnConnectAvatar;
import com.projectswg.common.network.packets.swg.zone.chat.VoiceChatStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatServerStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.global.zone.RequestZoneInIntent;
import com.projectswg.holocore.intents.support.objects.awareness.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.intents.support.objects.swg.*;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.awareness.DataTransformHandler;
import com.projectswg.holocore.resources.support.objects.awareness.ObjectAwareness;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.List;

public class AwarenessService extends Service {
	
	private static final Location GONE_LOCATION = Location.builder().setTerrain(Terrain.GONE).setPosition(0, 0, 0).build();
	
	private final ObjectAwareness awareness;
	private final DataTransformHandler dataTransformHandler;
	
	public AwarenessService() {
		this.awareness = new ObjectAwareness();
		dataTransformHandler = new DataTransformHandler();
		dataTransformHandler.setSpeedCheck(DataManager.getConfig(ConfigFile.FEATURES).getBoolean("SPEED-HACK-CHECK", true));
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		switch (pei.getEvent()) {
			case PE_DESTROYED:
				assert creature != null;
				creature.setOwner(null);
				awareness.destroyObject(creature);
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		oci.getObject().updateLoadRange();
		awareness.createObject(oci.getObject());
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		synchronized (obj.getAwarenessLock()) {
			obj.systemMove(null, GONE_LOCATION);
			awareness.destroyObject(doi.getObject());
		}
	}
	
	@IntentHandler
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject obj = oti.getObject();
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer()) {
			Location newLocation = oti.getNewLocation();
			// If we're on the same terrain and within the minimum terrain load range (1024m), or we're aware of the parent we're about to go to
			if ((newLocation.getTerrain() == obj.getTerrain() && oti.getParent() == null && newLocation.distanceTo(obj.getWorldLocation()) <= 1024) || (oti.getParent() != null && obj.getAware().contains(oti.getParent()))) {
				synchronized (obj.getAwarenessLock()) {
					obj.systemMove(oti.getParent(), newLocation);
					awareness.updateObject(obj);
				}
				if (oti.getParent() != null)
					obj.getOwner().sendPacket(new DataTransformWithParent(obj.getObjectId(), obj.getNextUpdateCount(), oti.getParent().getObjectId(), newLocation, 0));
				else
					obj.getOwner().sendPacket(new DataTransform(obj.getObjectId(), obj.getNextUpdateCount(), newLocation, 0));
			} else {
				handleZoneIn((CreatureObject) obj, oti.getNewLocation(), oti.getParent());
			}
		} else {
			synchronized (obj.getAwarenessLock()) {
				obj.systemMove(oti.getParent(), oti.getNewLocation());
				awareness.updateObject(obj);
			}
		}
	}
	
	@IntentHandler
	private void processGalacticPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof DataTransform) {
			handleDataTransform((DataTransform) packet);
		} else if (packet instanceof DataTransformWithParent) {
			handleDataTransformWithParent((DataTransformWithParent) packet);
		} else if (packet instanceof CmdSceneReady) {
			handleCmdSceneReady(gpi.getPlayer(), (CmdSceneReady) packet);
		}
	}
	
	@IntentHandler
	private void processMoveObjectIntent(MoveObjectIntent moi) {
		moveObjectWithTransform(moi.getObject(), moi.getParent(), moi.getNewLocation(), moi.getSpeed(), moi.getUpdateCounter());
	}
	
	@IntentHandler
	private void processContainerTransferIntent(ContainerTransferIntent cti) {
		SWGObject obj = cti.getObject();
		SWGObject oldContainer = cti.getOldContainer();
		SWGObject newContainer = cti.getContainer();
		if (oldContainer != null)
			awareness.updateObject(oldContainer);
		awareness.updateObject(cti.getObject());
		obj.sendObservers(new UpdateContainmentMessage(obj.getObjectId(), newContainer == null ? 0 : newContainer.getObjectId(), obj.getSlotArrangement()));
	}
	
	@IntentHandler
	private void handleForceAwarenessUpdateIntent(ForceAwarenessUpdateIntent faui) {
		awareness.updateObject(faui.getObject());
	}
	
	@IntentHandler
	private void handleRequestZoneInIntent(RequestZoneInIntent rzii) {
		handleZoneIn(rzii.getCreature(), rzii.getCreature().getLocation(), rzii.getCreature().getParent());
	}
	
	private void handleZoneIn(CreatureObject creature, Location loc, SWGObject parent) {
		Player player = creature.getOwner();
		
		// Fresh login or teleport/travel
		PlayerState state = player.getPlayerState();
		boolean firstZone = (state == PlayerState.LOGGED_IN);
		if (!firstZone && state != PlayerState.ZONED_IN) {
			CloseConnectionIntent.broadcast(player, DisconnectReason.SUSPECTED_HACK);
			return;
		}
		
		synchronized (creature.getAwarenessLock()) {
			// Safely clear awareness
			creature.setOwner(null);
			creature.setAware(AwarenessType.OBJECT, List.of());
			creature.setOwner(player);
			
			creature.systemMove(parent, loc);
			creature.resetObjectsAware();
			startZone(creature, firstZone);
			awareness.updateObject(creature);
		}
	}
	
	private void startZone(CreatureObject creature, boolean firstZone) {
		Player player = creature.getOwner();
		player.setPlayerState(PlayerState.ZONING_IN);
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
		Location loc = creature.getWorldLocation();
		player.sendPacket(new CmdStartScene(true, creature.getObjectId(), creature.getRace(), loc, ProjectSWG.getGalacticTime(), (int) (System.currentTimeMillis() / 1E3)));
		Log.i("Zoning in %s with character %s to %s %s", player.getUsername(), player.getCharacterName(), loc.getPosition(), loc.getTerrain());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_CLIENT).broadcastAfterIntent(firstZoneIntent);
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		assert player.getPlayerState() == PlayerState.ZONING_IN;
		player.setPlayerState(PlayerState.ZONED_IN);
		Log.i("%s with character %s zoned in from %s", player.getUsername(), player.getCharacterName(), p.getSocketAddress());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_SERVER).broadcast();
		player.sendPacket(new CmdSceneReady());
		player.sendBufferedDeltas();
	}
	
	private void handleDataTransform(DataTransform dt) {
		SWGObject obj = ObjectLookup.getObjectById(dt.getObjectId());
		if (!(obj instanceof CreatureObject)) {
			Log.w("DataTransform object not CreatureObject! Was: " + (obj == null ? "null" : obj.getClass()));
			return;
		}
		Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(obj.getTerrain()).build();
		moveObjectWithTransform(obj, null, requestedLocation, dt.getSpeed(), dt.getUpdateCounter());
	}
	
	private void handleDataTransformWithParent(DataTransformWithParent dt) {
		SWGObject obj = ObjectLookup.getObjectById(dt.getObjectId());
		SWGObject parent = ObjectLookup.getObjectById(dt.getCellId());
		if (!(obj instanceof CreatureObject)) {
			Log.w("DataTransformWithParent object not CreatureObject! Was: " + (obj == null ? "null" : obj.getClass()));
			return;
		}
		if (parent == null) {
			Log.w("Unknown data transform parent! Obj: %d/%s  Parent: %d", dt.getObjectId(), obj, dt.getCellId());
			return;
		}
		Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(obj.getTerrain()).build();
		moveObjectWithTransform(obj, parent, requestedLocation, dt.getSpeed(), dt.getUpdateCounter());
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed, int update) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isStatesBitmask(CreatureState.RIDING_MOUNT)) {
//			SWGObject vehicle = obj.getParent();
//			assert vehicle != null : "vehicle is null";
//			awareness.moveObject(vehicle, null, requestedLocation);
//			dataTransformHandler.handleMove(vehicle, speed, update);
//			awareness.moveObject(obj, null, requestedLocation);
		} else {
			synchronized (obj.getAwarenessLock()) {
				obj.systemMove(parent, requestedLocation);
				awareness.updateObject(obj);
			}
			if (parent == null) {
				dataTransformHandler.handleMove(obj, speed, update);
			} else {
				dataTransformHandler.handleMove(obj, parent, speed, update);
			}
		}
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), requestedLocation).broadcast();
	}
	
}
