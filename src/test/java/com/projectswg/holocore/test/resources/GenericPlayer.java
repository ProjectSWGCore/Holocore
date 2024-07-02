/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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

package com.projectswg.holocore.test.resources;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetworkProtocol;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.*;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.building.UpdateCellPermissionMessage;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransform;
import com.projectswg.common.network.packets.swg.zone.object_controller.DataTransformWithParent;
import com.projectswg.common.network.packets.swg.zone.object_controller.ObjectController;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.cell.CellObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import static org.junit.jupiter.api.Assertions.*;

public class GenericPlayer extends Player {
	
	private static final AtomicLong UNIQUE_ID = new AtomicLong(0);
	
	private final Map<Long, SWGObject> loadingObjects;
	private final Map<Long, SWGObject> objects;
	private final List<SWGPacket> packets;
	private final AtomicBoolean zoning;
	private final ReentrantLock packetLock;
	private final Condition packetLockCondition;
	
	public GenericPlayer() {
		super(UNIQUE_ID.incrementAndGet(), null, p -> {});
		this.loadingObjects = new ConcurrentHashMap<>();
		this.objects = new ConcurrentHashMap<>();
		this.packets = new ArrayList<>();
		this.zoning = new AtomicBoolean(false);
		this.packetLock = new ReentrantLock();
		this.packetLockCondition = this.packetLock.newCondition();
	}
	
	@Override
	public void sendPacket(SWGPacket packet) {
		packetLock.lock();
		try {
			encodeAndDecode(packet);
			this.packets.add(packet);
			packetLockCondition.signalAll();
		} finally {
			packetLock.unlock();
		}
		handlePacket(packet);
	}

	private static void encodeAndDecode(SWGPacket packet) {
		try {
			// This doesn't actually test the encoding and decoding of the packet, but it does test that the packet can be encoded and decoded without throwing an exception
			NetBuffer data = NetworkProtocol.encode(packet);
			NetworkProtocol.decode(data);
			int remaining = data.remaining();
			if (remaining > 0) {
				throw new RuntimeException("Encoded packet buffer had more data that wasn't read during decoding. Bytes remaining: " + remaining);
			}
		} catch (Throwable t) {
			throw new RuntimeException("Failed to encode and decode packet", t);
		}
	}

	@Override
	public void sendPacket(SWGPacket packet1, SWGPacket packet2) {
		sendPacket(packet1);
		sendPacket(packet2);
	}
	
	@Override
	public void sendPacket(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3) {
		sendPacket(packet1);
		sendPacket(packet2);
		sendPacket(packet3);
	}
	
	@Override
	public void sendPacket(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4) {
		sendPacket(packet1);
		sendPacket(packet2);
		sendPacket(packet3);
		sendPacket(packet4);
	}
	
	@Override
	public void sendPacket(SWGPacket packet1, SWGPacket packet2, SWGPacket packet3, SWGPacket packet4, SWGPacket packet5) {
		sendPacket(packet1);
		sendPacket(packet2);
		sendPacket(packet3);
		sendPacket(packet4);
		sendPacket(packet5);
	}
	
	@Nullable
	public SWGPacket getNextPacket() {
		packetLock.lock();
		try {
			if (packets.isEmpty())
				return null;
			return packets.remove(0);
		} finally {
			packetLock.unlock();
		}
	}
	
	@Nullable
	public <T extends SWGPacket> T getNextPacket(Class<T> type) {
		packetLock.lock();
		try {
			for (Iterator<SWGPacket> it = packets.iterator(); it.hasNext(); ) {
				SWGPacket next = it.next();
				if (type.isInstance(next)) {
					it.remove();
					return type.cast(next);
				}
			}
		} finally {
			packetLock.unlock();
		}
		return null;
	}
	
	@Nullable
	public <T extends SWGPacket> T waitForNextPacket(Class<T> type) {
		return waitForNextPacket(type, 1, TimeUnit.SECONDS);
	}
	
	@Nullable
	public <T extends SWGPacket> T waitForNextPacket(Class<T> type, long timeout, TimeUnit unit) {
		packetLock.lock();
		try {
			long startTime = System.nanoTime();
			while (System.nanoTime() - startTime < unit.toNanos(timeout)) {
				for (Iterator<SWGPacket> it = packets.iterator(); it.hasNext(); ) {
					SWGPacket next = it.next();
					if (type.isInstance(next)) {
						it.remove();
						return type.cast(next);
					}
				}
				try {
					//noinspection ResultOfMethodCallIgnored
					packetLockCondition.awaitNanos(unit.toNanos(timeout) - (System.nanoTime() - startTime));
				} catch (InterruptedException e) {
					return null;
				}
			}
		} finally {
			packetLock.unlock();
		}
		return null;
	}
	@Nullable
	public DeltasMessage waitForNextObjectDelta(long objectId, int num, int update, long timeout, TimeUnit unit) {
		Class<? extends SWGPacket> type = DeltasMessage.class;
		packetLock.lock();
		try {
			long startTime = System.nanoTime();
			while (System.nanoTime() - startTime < unit.toNanos(timeout)) {
				for (Iterator<SWGPacket> it = packets.iterator(); it.hasNext(); ) {
					SWGPacket next = it.next();
					if (type.isInstance(next)) {
						it.remove();
						DeltasMessage deltasMessage = (DeltasMessage) next;
						
						if (deltasMessage.getObjectId() == objectId && deltasMessage.getNum() == num && deltasMessage.getUpdate() == update) {
							return deltasMessage;
						}
					}
				}
				try {
					//noinspection ResultOfMethodCallIgnored
					packetLockCondition.awaitNanos(unit.toNanos(timeout) - (System.nanoTime() - startTime));
				} catch (InterruptedException e) {
					return null;
				}
			}
		} finally {
			packetLock.unlock();
		}
		return null;
	}

	@Nullable
	public SWGPacket waitForNextPacket(Set<Class<? extends SWGPacket>> types, long timeout, TimeUnit unit) {
		packetLock.lock();
		try {
			long startTime = System.nanoTime();
			while (System.nanoTime() - startTime < unit.toNanos(timeout)) {
				for (Iterator<SWGPacket> it = packets.iterator(); it.hasNext(); ) {
					SWGPacket next = it.next();
					if (types.contains(next.getClass())) {
						it.remove();
						return next;
					}
				}
				try {
					//noinspection ResultOfMethodCallIgnored
					packetLockCondition.awaitNanos(unit.toNanos(timeout) - (System.nanoTime() - startTime));
				} catch (InterruptedException e) {
					return null;
				}
			}
		} finally {
			packetLock.unlock();
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
		assertInstanceOf(CellObject.class, objects.get(p.getCellId()));
	}
	
	private void handleUpdatePvpStatusMessage(UpdatePvpStatusMessage p) {
		SWGObject obj = objects.get(p.getObjectId());
		assertNotNull(obj);
		assertInstanceOf(CreatureObject.class, obj);
		((CreatureObject) obj).setFaction(ServerData.INSTANCE.getFactions().getFaction(p.getPlayerFaction().name().toLowerCase(Locale.US)));
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
		assertInstanceOf(CreatureObject.class, obj);
		Posture posture = Posture.getFromId(p.getPosture());
		assertNotNull(posture);
		assertNotEquals(Posture.INVALID, posture);
		((CreatureObject) obj).setPosture(posture);
	}
	
}
