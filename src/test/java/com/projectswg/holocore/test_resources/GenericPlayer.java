/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/

package com.projectswg.holocore.test_resources;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.Assert.*;

public class GenericPlayer extends Player {
	
	private static final AtomicLong UNIQUE_ID = new AtomicLong(0);
	
	private final Map<Long, SWGObject> loadingObjects;
	private final Map<Long, SWGObject> objects;
	private final List<SWGPacket> packets;
	private final AtomicBoolean zoning;
	
	public GenericPlayer() {
		super(UNIQUE_ID.incrementAndGet());
		this.loadingObjects = new ConcurrentHashMap<>();
		this.objects = new ConcurrentHashMap<>();
		this.packets = new ArrayList<>();
		this.zoning = new AtomicBoolean(false);
	}
	
	@Override
	public void sendPacket(SWGPacket... packets) {
		for (SWGPacket packet : packets) {
			this.packets.add(packet);
			handlePacket(packet);
		}
	}
	
	@Nullable
	public SWGPacket getNextPacket() {
		if (packets.isEmpty())
			return null;
		return packets.remove(0);
	}
	
	@Nullable
	public <T extends SWGPacket> T getNextPacket(Class<T> type) {
		for (Iterator<SWGPacket> it = packets.iterator(); it.hasNext(); ) {
			SWGPacket next = it.next();
			if (type.isInstance(next)) {
				it.remove();
				return type.cast(next);
			}
		}
		return null;
	}
	
	private void handlePacket(SWGPacket packet) {
		switch (packet.getPacketType()) {
			case SCENE_CREATE_OBJECT_BY_CRC:
				handleSceneCreateObjectByCrc((SceneCreateObjectByCrc) packet);
				break;
			case SCENE_CREATE_OBJECT_BY_NAME:
				handleSceneCreateObjectByName((SceneCreateObjectByName) packet);
				break;
			case BASELINE:
				handleBaseline((Baseline) packet);
				break;
			case DELTA:
				handleDeltasMessage((DeltasMessage) packet);
				break;
			case SCENE_END_BASELINES:
				handleSceneEndBaselines((SceneEndBaselines) packet);
				break;
			case SCENE_DESTROY_OBJECT:
				handleSceneDestroyObject((SceneDestroyObject) packet);
				break;
			case CMD_START_SCENE:
				handleCommandStartScene((CmdStartScene) packet);
				break;
			case CMD_SCENE_READY:
				handleCommandSceneReady((CmdSceneReady) packet);
				break;
			case OBJECT_CONTROLLER:
				handleObjectController((ObjectController) packet);
				break;
			case UPDATE_TRANSFORMS_MESSAGE:
				handleUpdateTransform((UpdateTransformMessage) packet);
				break;
			case UPDATE_TRANSFORM_WITH_PARENT_MESSAGE:
				handleUpdateTransformWithParent((UpdateTransformWithParentMessage) packet);
				break;
			case UPDATE_CELL_PERMISSIONS_MESSAGE:
				handleUpdateCellPermission((UpdateCellPermissionMessage) packet);
				break;
			case UPDATE_PVP_STATUS_MESSAGE:
				handleUpdatePvpStatusMessage((UpdatePvpStatusMessage) packet);
				break;
			case UPDATE_CONTAINMENT_MESSAGE:
				handleUpdateContainment((UpdateContainmentMessage) packet);
				break;
			case UPDATE_POSTURE_MESSAGE:
				handleUpdatePosture((UpdatePostureMessage) packet);
				break;
			default:
				break;
		}
	}
	
	private void handleSceneCreateObjectByCrc(SceneCreateObjectByCrc p) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(p.getObjectId(), CRC.getString(p.getObjectCrc()));
		obj.setLocation(p.getLocation());
		assertNull(loadingObjects.put(p.getObjectId(), obj));
		assertFalse(objects.containsKey(p.getObjectId()));
	}
	
	private void handleSceneCreateObjectByName(SceneCreateObjectByName p) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(p.getObjectId(), p.getTemplate());
		obj.setLocation(p.getLocation());
		assertNull(loadingObjects.put(p.getObjectId(), obj));
		assertFalse(objects.containsKey(p.getObjectId()));
	}
	
	private void handleBaseline(Baseline p) {
		assertTrue(loadingObjects.containsKey(p.getObjectId()));
		assertFalse(objects.containsKey(p.getObjectId()));
	}
	
	private void handleDeltasMessage(DeltasMessage p) {
		assertFalse(loadingObjects.containsKey(p.getObjectId()));
		assertTrue(objects.containsKey(p.getObjectId()));
		assertFalse(zoning.get());
	}
	
	private void handleSceneEndBaselines(SceneEndBaselines p) {
		SWGObject obj = loadingObjects.remove(p.getObjectId());
		assertNotNull(obj);
		assertNull(objects.put(obj.getObjectId(), obj));
	}
	
	private void handleSceneDestroyObject(SceneDestroyObject p) {
		assertNotNull(objects.remove(p.getObjectId()));
	}
	
	private void handleCommandStartScene(CmdStartScene p) {
		assertFalse(zoning.getAndSet(true));
		loadingObjects.clear();
		objects.clear();
	}
	
	private void handleCommandSceneReady(CmdSceneReady p) {
		assertTrue(zoning.getAndSet(false));
	}
	
	private void handleObjectController(ObjectController p) {
		switch (p.getControllerCrc()) {
			case DataTransform.CRC:
				handleDataTransform((DataTransform) p);
				break;
			case DataTransformWithParent.CRC:
				handleDataTransformWithParent((DataTransformWithParent) p);
				break;
			default:
				break;
		}
	}
	
	private void handleDataTransform(DataTransform p) {
		SWGObject obj = objects.get(p.getObjectId());
		assertNotNull(obj);
		obj.systemMove(null, Location.builder(p.getLocation()).setTerrain(obj.getTerrain()).build());
	}
	
	private void handleDataTransformWithParent(DataTransformWithParent p) {
		SWGObject obj = objects.get(p.getObjectId());
		SWGObject container = objects.get(p.getCellId());
		assertNotNull(obj);
		assertNotNull(container);
		obj.systemMove(container, Location.builder(p.getLocation()).setTerrain(obj.getTerrain()).build());
	}
	
	private void handleUpdateTransform(UpdateTransformMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		assertNotNull(obj);
		obj.systemMove(null, Location.builder(obj.getLocation()).setPosition(p.getX()/4.0, p.getY()/4.0, p.getZ()/4.0).build());
	}
	
	private void handleUpdateTransformWithParent(UpdateTransformWithParentMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		SWGObject container = objects.get(p.getCellId());
		assertNotNull(obj);
		assertNotNull(container);
		obj.systemMove(container, Location.builder(obj.getLocation()).setPosition(p.getX()/8.0, p.getY()/8.0, p.getZ()/8.0).build());
	}
	
	private void handleUpdateCellPermission(UpdateCellPermissionMessage p) {
		assertTrue(objects.get(p.getCellId()) instanceof CellObject);
	}
	
	private void handleUpdatePvpStatusMessage(UpdatePvpStatusMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		assertNotNull(obj);
		assertTrue(obj instanceof CreatureObject);
		((CreatureObject) obj).setPvpFaction(p.getPlayerFaction());
		((CreatureObject) obj).setPvpFlags(p.getPvpFlags());
	}
	
	private void handleUpdateContainment(UpdateContainmentMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		SWGObject container = objects.get(p.getContainerId());
		assertNotNull(obj);
		assertNotNull(container);
		assertEquals(container.getArrangementId(obj), p.getSlotIndex());
		obj.systemMove(container);
	}
	
	private void handleUpdatePosture(UpdatePostureMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		assertNotNull(obj);
		assertTrue(obj instanceof CreatureObject);
		Posture posture = Posture.getFromId(p.getPosture());
		assertNotNull(posture);
		assertNotEquals(Posture.INVALID, posture);
		((CreatureObject) obj).setPosture(posture);
	}
	
}
