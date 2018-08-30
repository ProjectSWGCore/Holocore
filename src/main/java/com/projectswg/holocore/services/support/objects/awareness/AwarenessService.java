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
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.chat.ChatOnConnectAvatar;
import com.projectswg.common.network.packets.swg.zone.chat.VoiceChatStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.ChatServerStatus;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.common.network.packets.swg.zone.object_controller.TeleportAck;
import com.projectswg.holocore.ProjectSWG;
import com.projectswg.holocore.intents.gameplay.world.travel.pet.DismountIntent;
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent;
import com.projectswg.holocore.intents.support.global.zone.RequestZoneInIntent;
import com.projectswg.holocore.intents.support.objects.awareness.ForceAwarenessUpdateIntent;
import com.projectswg.holocore.intents.support.objects.swg.*;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.network.DisconnectReason;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.awareness.ObjectAwareness;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.control.Intent;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class AwarenessService extends Service {
	
	private static final Location GONE_LOCATION = Location.builder().setTerrain(Terrain.GONE).setPosition(0, 0, 0).build();
	
	private final ObjectAwareness awareness;
	private final Set<SWGObject> teleporting;
	
	public AwarenessService() {
		this.awareness = new ObjectAwareness();
		this.teleporting = ConcurrentHashMap.newKeySet();
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
		@NotNull SWGObject obj = oti.getObject();
		@Nullable SWGObject oldParent = oti.getOldParent();
		@Nullable SWGObject newParent = oti.getNewParent();
		@NotNull Location oldLocation = oti.getOldLocation();
		@NotNull Location newLocation = oti.getNewLocation();
		
		if (isPlayerZoneInRequired(obj, oldLocation, newLocation)) {
			handleZoneIn((CreatureObject) obj, newLocation, newParent);
			return;
		}
		
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			teleporting.add(obj);
		update(obj);
		update(oldParent);
		
		onObjectMoved(obj, oldParent, newParent, oldLocation, newLocation, true, 0);
	}
	
	@IntentHandler
	private void processInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof DataTransform) {
			handleDataTransform(gpi.getPlayer(), (DataTransform) packet);
		} else if (packet instanceof DataTransformWithParent) {
			handleDataTransformWithParent(gpi.getPlayer(), (DataTransformWithParent) packet);
		} else if (packet instanceof CmdSceneReady) {
			handleCmdSceneReady(gpi.getPlayer(), (CmdSceneReady) packet);
		} else if (packet instanceof TeleportAck) {
			teleporting.remove(gpi.getPlayer().getCreatureObject());
		}
	}
	
	@IntentHandler
	private void processMoveObjectIntent(MoveObjectIntent moi) {
		moveObjectWithTransform(moi.getObject(), moi.getParent(), moi.getNewLocation(), moi.getSpeed());
	}
	
	@IntentHandler
	private void processContainerTransferIntent(ContainerTransferIntent cti) {
		@NotNull SWGObject obj = cti.getObject();
		@Nullable SWGObject oldContainer = cti.getOldContainer();
		@Nullable SWGObject newContainer = cti.getContainer();
		
		update(cti.getObject());
		update(oldContainer);
		
		onObjectMoved(obj, oldContainer, newContainer, obj.getLocation(), obj.getLocation(), false, 0);
	}
	
	@IntentHandler
	private void handleForceAwarenessUpdateIntent(ForceAwarenessUpdateIntent faui) {
		update(faui.getObject());
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
		
		SWGObject oldParent = creature.getParent();
		Location oldLocation = creature.getLocation();
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
		onObjectMoved(creature, oldParent, parent, oldLocation, loc, false, 0);
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
		StandardLog.onPlayerEvent(this, player, "zoning in at %s %s", loc.getTerrain(), loc.getPosition());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_CLIENT).broadcastAfterIntent(firstZoneIntent);
	}
	
	private void handleCmdSceneReady(Player player, CmdSceneReady p) {
		assert player.getPlayerState() == PlayerState.ZONING_IN;
		player.setPlayerState(PlayerState.ZONED_IN);
		StandardLog.onPlayerEvent(this, player, "zoned in from %s", p.getSocketAddress());
		new PlayerEventIntent(player, PlayerEvent.PE_ZONE_IN_SERVER).broadcast();
		player.sendPacket(new CmdSceneReady());
		player.sendBufferedDeltas();
	}
	
	private void handleDataTransform(Player player, DataTransform dt) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransform without being logged in");
			return;
		}
		if (creature.getObjectId() != dt.getObjectId()) {
			StandardLog.onPlayerError(this, player, "sent a DataTransform for another object [%d]", dt.getObjectId());
			return;
		}
		if (teleporting.contains(creature))
			return;
		
		Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(creature.getTerrain()).build();
		double speed = dt.getSpeed();
		
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			// If this is the primary rider, move the mount and all of the riders
			CreatureObject mount = (CreatureObject) creature.getParent();
			assert mount != null && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE) : "invalid parent for riding mount";
			
			if (mount.getOwnerId() == creature.getObjectId()) {
				moveObjectWithTransform(mount, null, requestedLocation, speed);
				for (SWGObject child : mount.getSlottedObjects()) {
					moveObjectWithTransform(child, mount, requestedLocation, speed);
				}
			}
		} else {
			moveObjectWithTransform(creature, null, requestedLocation, dt.getSpeed());
		}
	}
	
	private void handleDataTransformWithParent(Player player, DataTransformWithParent dt) {
		CreatureObject creature = player.getCreatureObject();
		if (creature == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent without being logged in");
			return;
		}
		if (creature.getObjectId() != dt.getObjectId()) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent for another object [%d]", dt.getObjectId());
			return;
		}
		SWGObject parent = ObjectLookup.getObjectById(dt.getCellId());
		if (parent == null) {
			StandardLog.onPlayerError(this, player, "sent a DataTransformWithParent with an unknown parent [%d]", dt.getCellId());
			return;
		}
		if (teleporting.contains(creature))
			return;
		
		if (creature.isStatesBitmask(CreatureState.RIDING_MOUNT)) {
			// If this is the primary rider, move the mount and all of the riders
			CreatureObject mount = (CreatureObject) creature.getParent();
			assert mount != null && mount.isStatesBitmask(CreatureState.MOUNTED_CREATURE) : "invalid parent for riding mount";
			mount.sendObservers(new DataTransform(mount.getObjectId(), 0, mount.getNextUpdateCount(), mount.getLocation(), 0));
			DismountIntent.broadcast(creature, mount);
		} else {
			Location requestedLocation = Location.builder(dt.getLocation()).setTerrain(creature.getTerrain()).build();
			moveObjectWithTransform(creature, parent, requestedLocation, dt.getSpeed());
		}
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed) {
		SWGObject oldParent = obj.getParent();
		Location oldLocation = obj.getLocation();
		synchronized (obj.getAwarenessLock()) {
			obj.systemMove(parent, requestedLocation);
			awareness.updateObject(obj);
		}
		
		if (oldParent != parent)
			update(oldParent);
		
		onObjectMoved(obj, oldParent, parent, oldLocation, requestedLocation, false, speed);
	}
	
	private void update(@Nullable SWGObject obj) {
		if (obj != null) {
			synchronized (obj.getAwarenessLock()) {
				awareness.updateObject(obj);
			}
		}
	}
	
	private static boolean isPlayerZoneInRequired(@NotNull SWGObject obj, @NotNull Location oldLocation, @NotNull Location newLocation) {
		if (!(obj instanceof CreatureObject))
			return false;
		if (!((CreatureObject) obj).isLoggedInPlayer())
			return false;
		return oldLocation.getTerrain() != newLocation.getTerrain();
	}
	
	private static void onObjectMoved(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @Nullable SWGObject newParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, oldParent, newParent, oldLocation, newLocation).broadcast();
		
		if (newParent != null) {
			onObjectMovedInParent(obj, oldParent, newParent, oldLocation, newLocation, forceSelfUpdate, speed);
		} else {
			onObjectMovedInWorld(obj, oldParent, oldLocation, newLocation, forceSelfUpdate, speed);
		}
	}
	
	private static void onObjectMovedInParent(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @NotNull SWGObject newParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		if (oldParent != newParent)
			obj.sendObservers(new UpdateContainmentMessage(obj.getObjectId(), newParent.getObjectId(), obj.getSlotArrangement()));
		
		// Slotted objects don't get position updates - they inherit their parent's location, plus a client-defined offset (e.g. armor, mounts)
		if (obj.getSlotArrangement() == -1) {
			int counter = obj.getNextUpdateCount();
			if (forceSelfUpdate)
				obj.sendSelf(new DataTransformWithParent(obj.getObjectId(), 0, counter, newParent.getObjectId(), newLocation, (byte) Math.round(speed)));
			
			if (!oldLocation.equals(newLocation))
				obj.sendObservers(new UpdateTransformWithParentMessage(obj.getObjectId(), newParent.getObjectId(), counter, newLocation, (byte) Math.round(speed)));
		}
	}
	
	private static void onObjectMovedInWorld(@NotNull SWGObject obj, @Nullable SWGObject oldParent, @NotNull Location oldLocation, @NotNull Location newLocation, boolean forceSelfUpdate, double speed) {
		int counter = obj.getNextUpdateCount();
		
		if (oldParent != null)
			obj.sendObservers(new UpdateContainmentMessage(obj.getObjectId(), 0, obj.getSlotArrangement()));
		
		if (forceSelfUpdate)
			obj.sendSelf(new DataTransform(obj.getObjectId(), 0, counter, newLocation, (byte) Math.round(speed)));
		
		if (!oldLocation.equals(newLocation))
			obj.sendObservers(new UpdateTransformMessage(obj.getObjectId(), counter, newLocation, (byte) Math.round(speed)));
	}
	
}
