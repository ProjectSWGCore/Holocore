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

import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent;
import com.projectswg.holocore.resources.support.color.SWGColor;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledFuture;

public class AIObject extends CreatureObject {
	
	private final Set<CreatureObject> playersNearby;
	private final List<WeaponObject> defaultWeapons;
	private final List<WeaponObject> thrownWeapon;
	private final SWGObject hiddenInventory;
	private final IncapSafetyTimer incapSafetyTimer;
	
	private NpcMode defaultMode;
	private NpcMode activeMode;
	private Spawner spawner;
	private ScheduledThreadPool executor;
	private ScheduledFuture<?> previousScheduled;
	private String creatureId;
	private Instant questionMarkBlockedUntil;
	
	public AIObject(long objectId) {
		super(objectId);
		this.playersNearby = new CopyOnWriteArraySet<>();
		this.defaultWeapons = new ArrayList<>();
		this.thrownWeapon = new ArrayList<>();
		this.hiddenInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_character_inventory.iff");
		
		this.spawner = null;
		this.executor = null;
		this.previousScheduled = null;
		this.defaultMode = null;
		this.activeMode = null;
		this.creatureId = null;
		incapSafetyTimer = new IncapSafetyTimer(45_000);
		questionMarkBlockedUntil = Instant.now();
	}
	
	@Override
	public void onObjectEnteredAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO || hasOptionFlags(OptionFlag.INVULNERABLE))
			return;
		
		if (((CreatureObject) aware).isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			aware = aware.getSlottedObject("rider");
			if (!(aware instanceof CreatureObject))
				return;
		}
		CreatureObject player = (CreatureObject) aware;
		if (player.hasOptionFlags(OptionFlag.INVULNERABLE))
			return;
		
		playersNearby.add(player);
		if (activeMode != null)
			activeMode.onPlayerEnterAware(player, flatDistanceTo(player));
		
		checkAwareAttack(player);
	}
	
	@Override
	public void onObjectExitedAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO)
			return;
		
		if (((CreatureObject) aware).isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			aware = aware.getSlottedObject("rider");
			if (!(aware instanceof CreatureObject))
				return;
		}
		playersNearby.remove(aware);
		if (activeMode != null)
			activeMode.onPlayerExitAware((CreatureObject) aware);
	}
	
	@Override
	public void onObjectMoveInAware(SWGObject aware) {
		if (aware.getBaselineType() != BaselineType.CREO)
			return;
		
		if (((CreatureObject) aware).isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			aware = aware.getSlottedObject("rider");
			if (!(aware instanceof CreatureObject))
				return;
		}
		CreatureObject player = (CreatureObject) aware;
		if (player.hasOptionFlags(OptionFlag.INVULNERABLE))
			return;
		playersNearby.add(player);
		
		boolean npcIsInvulnerable = hasOptionFlags(OptionFlag.INVULNERABLE);
		
		if (!npcIsInvulnerable) {
			boolean npcIsInCombat = isInCombat();
			if (!npcIsInCombat) {
				boolean npcIsAggressiveTowardsPlayer = isAttackable(player);
				
				if (npcIsAggressiveTowardsPlayer) {
					boolean playerIsInLineOfSight = isLineOfSight(player);
					
					if (playerIsInLineOfSight) {
						Location playerWorldLocation = player.getWorldLocation();
						Location aiWorldLocation = this.getWorldLocation();
						double questionMarkRange = 64;
						double distanceBetweenNpcAndPlayer = aiWorldLocation.distanceTo(playerWorldLocation);
						boolean playerIsInRange = distanceBetweenNpcAndPlayer <= questionMarkRange;
						
						if (playerIsInRange) {
							Instant now = Instant.now();
							boolean questionMarkTimerExpired = now.isAfter(questionMarkBlockedUntil);
							if (questionMarkTimerExpired) {
								showQuestionMarkAboveNpc();
								questionMarkBlockedUntil = Instant.now().plusSeconds(60);
							}
						}
					}
				}
			}
		}
		
		checkAwareAttack(player);
	}
	
	private void showQuestionMarkAboveNpc() {
		this.sendObservers(new ShowFlyText(this.getObjectId(), new StringId("npc_reaction/flytext", "alert"), ShowFlyText.Scale.SMALL, SWGColor.Reds.INSTANCE.getRed()));
	}
	
	private void checkAwareAttack(CreatureObject player) {
		boolean incapSafetyTimerExpired = incapSafetyTimer.isExpired(System.currentTimeMillis(), player.getLastIncapTime());
		
		if (isAttackable(player) && incapSafetyTimerExpired) {
			double distance = getLocation().flatDistanceTo(player.getLocation());
			double maxAggroDistance;
			if (player.isLoggedInPlayer())
				maxAggroDistance = getSpawner().getAggressiveRadius();
			else if (!player.isPlayer())
				maxAggroDistance = 30;
			else
				maxAggroDistance = -1; // Ensures the following if-statement will fail and remove the player from the list
			
			if (distance <= maxAggroDistance && isLineOfSight(player)) {
				if (spawner.getBehavior() == AIBehavior.PATROL) {
					for (AIObject npc : spawner.getNpcs())
						StartNpcCombatIntent.broadcast(npc, List.of(player));
				} else {
					StartNpcCombatIntent.broadcast(this, List.of(player));
				}
			}
		}
	}
	
	@Override
	public boolean isWithinAwarenessRange(SWGObject target) {
		assert target instanceof CreatureObject;
		return isAttackable((CreatureObject) target) && flatDistanceTo(target) <= 50;
	}
	
	public void setSpawner(Spawner spawner) {
		this.spawner = spawner;
	}
	
	public void addDefaultWeapon(WeaponObject weapon) {
		this.defaultWeapons.add(weapon);
		weapon.systemMove(hiddenInventory);
		
		if ("object/weapon/creature/shared_creature_default_weapon.iff".equals(weapon.getTemplate())) {
			addCommand("creatureMeleeAttack");
		} else {
			addCommand(weapon.getType().getDefaultAttack());
		}
	}
	
	public void addThrownWeapon(WeaponObject weapon) {
		this.thrownWeapon.add(weapon);
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
	
	public List<WeaponObject> getDefaultWeapons() {
		return Collections.unmodifiableList(defaultWeapons);
	}
	
	public List<WeaponObject> getThrownWeapon() {
		return Collections.unmodifiableList(thrownWeapon);
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
