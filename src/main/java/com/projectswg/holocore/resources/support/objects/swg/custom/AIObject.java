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
package com.projectswg.holocore.resources.support.objects.swg.custom;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.encodables.tangible.PvpFlag;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;

public class AIObject extends CreatureObject {
	
	private final Set<CreatureObject> playersNearby;
	private final List<WeaponObject> primaryWeapons;
	private final List<WeaponObject> secondaryWeapons;
	private final EnumMap<ScheduledMode, NpcMode> modes;
	private final SWGObject hiddenInventory;
	
	private Spawner spawner;
	private ScheduledThreadPool executor;
	private ScheduledFuture<?> previousScheduled;
	private ScheduledMode mode;
	private String creatureId;
	
	public AIObject(long objectId) {
		super(objectId);
		this.playersNearby = new CopyOnWriteArraySet<>();
		this.primaryWeapons = new ArrayList<>();
		this.secondaryWeapons = new ArrayList<>();
		this.modes = new EnumMap<>(ScheduledMode.class);
		this.hiddenInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_character_inventory.iff");
		
		this.spawner = null;
		this.executor = null;
		this.previousScheduled = null;
		this.mode = ScheduledMode.DEFAULT;
		this.creatureId = null;
		setRunSpeed(7.3);
	}
	
	@Override
	public void onObjectMoveInAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO)
			return;
		CreatureObject player = (CreatureObject) aware;
		if (!player.isLoggedInPlayer())
			return;
		double distance = getWorldLocation().flatDistanceTo(aware.getWorldLocation());
		if (distance <= 300) {
			if (playersNearby.add(player)) {
				for (NpcMode mode : modes.values()) {
					mode.onPlayerEnterAware(player, distance);
				}
			} else {
				for (NpcMode mode : modes.values()) {
					mode.onPlayerMoveInAware(player, distance);
				}
			}
		} else {
			if (playersNearby.remove(player)) {
				for (NpcMode mode : modes.values()) {
					mode.onPlayerExitAware(player);
				}
			}
		}
	}
	
	@Override
	public boolean isEnemyOf(TangibleObject obj) {
		Posture myPosture = getPosture();
		if (myPosture == Posture.INCAPACITATED || myPosture == Posture.DEAD || !(obj instanceof CreatureObject))
			return false;
		Posture theirPosture = ((CreatureObject) obj).getPosture();
		return (theirPosture != Posture.INCAPACITATED || hasPvpFlag(PvpFlag.AGGRESSIVE)) && theirPosture != Posture.DEAD;
	}
	
	public void setSpawner(Spawner spawner) {
		this.spawner = spawner;
	}
	
	public void addPrimaryWeapon(WeaponObject weapon) {
		this.primaryWeapons.add(weapon);
		weapon.systemMove(hiddenInventory);
	}
	
	public void addSecondaryWeapon(WeaponObject weapon) {
		this.secondaryWeapons.add(weapon);
		weapon.systemMove(hiddenInventory);
	}
	
	@Override
	public void setEquippedWeapon(WeaponObject weapon) {
		WeaponObject equipped = getEquippedWeapon();
		if (equipped != null)
			equipped.systemMove(hiddenInventory);
		weapon.moveToContainer(this);
		super.setEquippedWeapon(weapon);
	}
	
	public Spawner getSpawner() {
		return spawner;
	}
	
	public List<WeaponObject> getPrimaryWeapons() {
		return Collections.unmodifiableList(primaryWeapons);
	}
	
	public List<WeaponObject> getSecondaryWeapons() {
		return Collections.unmodifiableList(secondaryWeapons);
	}
	
	public String getCreatureId() {
		return creatureId;
	}
	
	public void setCreatureId(String creatureId) {
		this.creatureId = creatureId;
	}
	
	public void setDefaultMode(NpcMode mode) {
		this.modes.put(ScheduledMode.DEFAULT, mode);
		mode.attach(this, ScheduledMode.DEFAULT);
	}
	
	public void setCombatMode(NpcMode mode) {
		this.modes.put(ScheduledMode.COMBAT, mode);
		mode.attach(this, ScheduledMode.COMBAT);
	}
	
	public void startCombatMode() {
		if (modes.containsKey(ScheduledMode.COMBAT)) {
			requestModeStart(ScheduledMode.COMBAT);
		}
	}
	
	public void start(ScheduledThreadPool executor) {
		this.executor = executor;
		queueNextLoop(1000);
	}
	
	public void stop() {
		ScheduledFuture<?> prev = this.previousScheduled;
		if (prev != null)
			prev.cancel(false);
		this.executor = null;
	}
	
	void requestModeStart(ScheduledMode mode) {
		if (mode == this.mode)
			return;
		this.mode = mode;
		NpcMode activeMode = modes.get(mode);
		if (activeMode != null)
			activeMode.onModeStart();
		queueNextLoop(0);
	}
	
	void requestModeEnd(ScheduledMode mode) {
		if (mode != this.mode && mode != ScheduledMode.DEFAULT)
			return;
		NpcMode inactiveMode = modes.get(this.mode);
		if (inactiveMode != null)
			inactiveMode.onModeEnd();
		requestModeStart(ScheduledMode.DEFAULT);
	}
	
	NpcMode getMode(ScheduledMode mode) {
		return modes.get(mode);
	}
	
	void queueNextLoop(long delay) {
		ScheduledFuture<?> prev = this.previousScheduled;
		if (prev != null)
			prev.cancel(false);
		previousScheduled = executor.execute(delay, this::loop);
	}
	
	ScheduledMode getActiveMode() {
		return mode;
	}
	
	final Set<CreatureObject> getNearbyPlayers() {
		return Collections.unmodifiableSet(playersNearby);
	}
	
	private void loop() {
		try {
			NpcMode mode = this.modes.get(this.mode);
			if (mode == null)
				return;
			mode.act();
		} catch (Throwable t) {
			Log.w(t);
			mode = ScheduledMode.DEFAULT;
			queueNextLoop(1000);
		}
	}
	
	enum ScheduledMode {
		DEFAULT,
		COMBAT
	}
	
}
