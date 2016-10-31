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
import intents.network.GalacticPacketIntent;
import intents.object.DestroyObjectIntent;
import intents.object.MoveObjectIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.player.PlayerTransformedIntent;
import main.ProjectSWG;
import network.packets.Packet;
import network.packets.swg.zone.CmdSceneReady;
import network.packets.swg.zone.insertion.CmdStartScene;
import network.packets.swg.zone.object_controller.DataTransform;
import network.packets.swg.zone.object_controller.DataTransformWithParent;
import resources.Location;
import resources.Race;
import resources.Terrain;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.awareness.AwarenessHandler;
import resources.objects.awareness.DataTransformHandler;
import resources.objects.awareness.TerrainMap.TerrainMapCallback;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import resources.server_info.Log;

public class ObjectAwareness extends Service implements TerrainMapCallback {
	
	private final AwarenessHandler awarenessHandler;
	private final DataTransformHandler dataTransformHandler;
	
	public ObjectAwareness() {
		awarenessHandler = new AwarenessHandler(this);
		dataTransformHandler = new DataTransformHandler();
		
		registerForIntent(PlayerEventIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		registerForIntent(DestroyObjectIntent.TYPE);
		registerForIntent(ObjectTeleportIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
		registerForIntent(MoveObjectIntent.TYPE);
	}
	
	@Override
	public boolean terminate() {
		awarenessHandler.close();
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case PlayerEventIntent.TYPE:
				if (i instanceof PlayerEventIntent)
					handlePlayerEventIntent((PlayerEventIntent) i);
				break;
			case ObjectCreatedIntent.TYPE:
				if (i instanceof ObjectCreatedIntent)
					handleObjectCreatedIntent((ObjectCreatedIntent) i);
				break;
			case DestroyObjectIntent.TYPE:
				if (i instanceof DestroyObjectIntent)
					handleDestroyObjectIntent((DestroyObjectIntent) i);
				break;
			case ObjectTeleportIntent.TYPE:
				if (i instanceof ObjectTeleportIntent)
					processObjectTeleportIntent((ObjectTeleportIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					processGalacticPacketIntent((GalacticPacketIntent) i);
				break;
			case MoveObjectIntent.TYPE:
				if (i instanceof MoveObjectIntent)
					processMoveObjectIntent((MoveObjectIntent) i);
				break;
			default:
				break;
		}
	}
	
	@Override
	public void onWithinRange(SWGObject obj, SWGObject inRange) {
		obj.addObjectAware(inRange);
	}
	
	@Override
	public void onOutOfRange(SWGObject obj, SWGObject outRange) {
		obj.removeObjectAware(outRange);
	}
	
	@Override
	public void onMoveSuccess(SWGObject obj) {
		
	}
	
	@Override
	public void onMoveFailure(SWGObject obj) {
		Log.e(this, "Move failure! %s", obj);
		obj.clearObjectsAware();
	}

	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		Player p = pei.getPlayer();
		CreatureObject creature = p.getCreatureObject();
		if (creature == null)
			return;
		switch (pei.getEvent()) {
			case PE_DISAPPEAR:
				disappearObject(creature, true, true);
				break;
			case PE_DESTROYED:
				creature.setOwner(null);
				break;
			case PE_ZONE_IN_CLIENT:
				startScene(creature);
				break;
			case PE_ZONE_IN_SERVER:
				p.sendPacket(new CmdSceneReady());
				break;
			default:
				break;
		}
	}
	
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject object = oci.getObject();
		if (object.getParent() == null)
			moveObject(object, object.getLocation());
	}
	
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		disappearObject(obj, true, true);
		obj.moveToContainer(null);
	}
	
	private void processObjectTeleportIntent(ObjectTeleportIntent oti) {
		SWGObject object = oti.getObject();
		Player owner = object.getOwner();
		object.setLocation(oti.getNewLocation());
		if (oti.getParent() != object.getParent())
			object.moveToContainer(oti.getParent());
		if (object instanceof CreatureObject && ((CreatureObject) object).isLoggedInPlayer())
			new RequestZoneInIntent(owner, (CreatureObject) object, false).broadcast();
	}
	
	private void processGalacticPacketIntent(GalacticPacketIntent i) {
		Packet packet = i.getPacket();
		if (packet instanceof DataTransform) {
			DataTransform trans = (DataTransform) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			if (obj instanceof CreatureObject) {
				Location requestedLocation = new Location(trans.getLocation());
				requestedLocation.setTerrain(obj.getTerrain());
				moveObjectWithTransform(obj, requestedLocation, trans.getSpeed(), trans.getUpdateCounter());
			}
		} else if (packet instanceof DataTransformWithParent) {
			DataTransformWithParent trans = (DataTransformWithParent) packet;
			SWGObject obj = i.getObjectManager().getObjectById(trans.getObjectId());
			SWGObject parent = i.getObjectManager().getObjectById(trans.getCellId());
			if (obj instanceof CreatureObject) {
				Location requestedLocation = new Location(trans.getLocation());
				requestedLocation.setTerrain(obj.getTerrain());
				moveObjectWithTransform(obj, parent, requestedLocation, trans.getSpeed(), trans.getUpdateCounter());
			}
		}
	}
	
	private void processMoveObjectIntent(MoveObjectIntent i) {
		if (i.getParent() == null)
			moveObjectWithTransform(i.getObject(), i.getNewLocation(), i.getSpeed(), i.getUpdateCounter());
		else
			moveObjectWithTransform(i.getObject(), i.getParent(), i.getNewLocation(), i.getSpeed(), i.getUpdateCounter());
	}
	
	private void startScene(CreatureObject creature) {
		Location loc = creature.getWorldLocation();
		long time = ProjectSWG.getGalacticTime();
		Race race = ((CreatureObject) creature).getRace();
		boolean ignoreSnapshots = loc.getTerrain() == Terrain.DEV_AREA;
		Player owner = creature.getOwner();
		creature.resetAwareness();
		owner.sendPacket(new CmdStartScene(ignoreSnapshots, creature.getObjectId(), race, loc, time, (int)(System.currentTimeMillis()/1E3)));
		recursiveCreateObject(creature, owner);
		if (creature.getParent() != null) {
			for (SWGObject obj : creature.getSuperParent().getObjectsAware()) {
				obj.createObject(creature);
				creature.createObject(obj);
			}
		}
	}
	
	private void recursiveCreateObject(SWGObject obj, Player owner) {
		SWGObject parent = obj.getParent();
		if (parent != null)
			recursiveCreateObject(parent, owner);
		else
			obj.createObject(owner, true);
	}
	
	private void moveObject(SWGObject obj, Location requestedLocation) {
		if (requestedLocation == null)
			awarenessHandler.disappearObject(obj, true, true);
		else
			awarenessHandler.moveObject(obj, requestedLocation);
	}
	
	private void moveObject(SWGObject obj, SWGObject parent, Location requestedLocation) {
		if (requestedLocation == null)
			awarenessHandler.disappearObject(obj, true, true);
		else
			awarenessHandler.moveObject(obj, parent, requestedLocation);
	}
	
	private void moveObjectWithTransform(SWGObject obj, Location requestedLocation, double speed, int update) {
		if (!dataTransformHandler.handleMove(obj, requestedLocation, speed, update))
			return;
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), null, obj.getLocation(), requestedLocation).broadcast();
		moveObject(obj, requestedLocation);
	}
	
	private void moveObjectWithTransform(SWGObject obj, SWGObject parent, Location requestedLocation, double speed, int update) {
		if (!dataTransformHandler.handleMove(obj, parent, requestedLocation, speed, update))
			return;
		if (obj instanceof CreatureObject && ((CreatureObject) obj).isLoggedInPlayer())
			new PlayerTransformedIntent((CreatureObject) obj, obj.getParent(), parent, obj.getLocation(), requestedLocation).broadcast();
		moveObject(obj, parent, requestedLocation);
	}
	
	private void disappearObject(SWGObject obj, boolean disappearObjects, boolean disappearCustom) {
		awarenessHandler.disappearObject(obj, disappearObjects, disappearCustom);
	}
	
}