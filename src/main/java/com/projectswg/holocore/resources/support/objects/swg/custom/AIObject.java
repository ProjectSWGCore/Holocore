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
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.NpcStaticSpawnLoader.SpawnerFlag;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;

public class AIObject extends CreatureObject {
	
	private final Set<CreatureObject> playersNearby;
	private final List<WeaponObject> primaryWeapons;
	private final List<WeaponObject> secondaryWeapons;
	private final SWGObject hiddenInventory;
	
	private NpcMode defaultMode;
	private NpcMode activeMode;
	private Spawner spawner;
	private ScheduledThreadPool executor;
	private ScheduledFuture<?> previousScheduled;
	private String creatureId;
	
	public AIObject(long objectId) {
		super(objectId);
		this.playersNearby = new CopyOnWriteArraySet<>();
		this.primaryWeapons = new ArrayList<>();
		this.secondaryWeapons = new ArrayList<>();
		this.hiddenInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_character_inventory.iff");
		
		this.spawner = null;
		this.executor = null;
		this.previousScheduled = null;
		this.defaultMode = null;
		this.activeMode = null;
		this.creatureId = null;
	}
	
	@Override
	public void onObjectMoveInAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO)
			return;
		if (((CreatureObject) aware).isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			aware = aware.getSlottedObject("rider");
			if (aware == null)
				return;
		}
		CreatureObject player = (CreatureObject) aware;
		if (!player.isLoggedInPlayer())
			return;
		double distance = getLocation().flatDistanceTo(aware.getLocation());
		NpcMode activeMode = this.activeMode;
		if (distance <= 100 && player.getPosture() != Posture.INCAPACITATED && player.getPosture() != Posture.DEAD) {
			if (playersNearby.add(player)) {
				if (activeMode != null)
					activeMode.onPlayerEnterAware(player, distance);
			} else {
				if (activeMode != null) {
					activeMode.onPlayerMoveInAware(player, distance);
					if (getSpawner().getSpawnerFlag() == SpawnerFlag.AGGRESSIVE && distance < getSpawner().getAggressiveRadius() && isEnemyOf(player) && isLineOfSight(player)) {
						StartNpcCombatIntent.broadcast(this, List.of(player));
					}
					
				}
			}
		} else {
			if (playersNearby.remove(player)) {
				if (activeMode != null)
					activeMode.onPlayerExitAware(player);
			}
		}
	}
	
	@Override
	public boolean isWithinAwarenessRange(SWGObject target) {
		return target instanceof CreatureObject && getObserverCreatures().contains(target);
	}
	
	@Override
	public boolean isEnemyOf(TangibleObject obj) {
		Posture myPosture = getPosture();
		if (myPosture == Posture.INCAPACITATED || myPosture == Posture.DEAD || !(obj instanceof CreatureObject))
			return false;
		Posture theirPosture = ((CreatureObject) obj).getPosture();
		return (theirPosture != Posture.INCAPACITATED || hasPvpFlag(PvpFlag.AGGRESSIVE)) && theirPosture != Posture.DEAD && super.isEnemyOf(obj);
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
	
	public NpcMode getDefaultMode() {
		return defaultMode;
	}
	
	public NpcMode getActiveMode() {
		return activeMode;
	}
	
	public void setCreatureId(String creatureId) {
		this.creatureId = creatureId;
	}
	
	public void start(ScheduledThreadPool executor) {
		this.executor = executor;
		ScheduleNpcModeIntent.broadcast(this, null);
	}
	
	public void stop() {
		ScheduledFuture<?> prev = this.previousScheduled;
		if (prev != null)
			prev.cancel(false);
		this.executor = null;
	}
	
	public void setDefaultMode(@NotNull NpcMode mode) {
		this.defaultMode = mode;
	}
	
	public void setActiveMode(@Nullable NpcMode mode) {
		this.activeMode = mode;
		queueNextLoop(0);
	}
	
	void queueNextLoop(long delay) {
		ScheduledFuture<?> prev = this.previousScheduled;
		if (prev != null)
			prev.cancel(false);
		previousScheduled = executor.execute(delay, this::loop);
	}
	
	final Set<CreatureObject> getNearbyPlayers() {
		return Collections.unmodifiableSet(playersNearby);
	}
	
	private void loop() {
		try {
			NpcMode mode = activeMode;
			if (mode != null)
				mode.act();
		} catch (Throwable t) {
			Log.w(t);
			queueNextLoop(1000);
		}
	}
	
}
