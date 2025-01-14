/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.objects.swg.custom

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.resources.support.color.SWGColor.Reds.red
import com.projectswg.holocore.resources.support.npc.ai.NpcCombatMode
import com.projectswg.holocore.resources.support.npc.ai.NpcIdleMode
import com.projectswg.holocore.resources.support.npc.spawn.Spawner
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.utilities.cancelAndWait
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import java.time.Instant
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet

class AIObject(objectId: Long) : CreatureObject(objectId) {
	private val playersNearby: MutableSet<CreatureObject> = CopyOnWriteArraySet()
	private val _defaultWeapons: MutableList<WeaponObject> = ArrayList()
	private val _thrownWeapons: MutableList<WeaponObject> = ArrayList()
	private val hiddenInventory = ObjectCreator.createObjectFromTemplate("object/tangible/inventory/shared_character_inventory.iff")
	private val incapSafetyTimer: IncapSafetyTimer = IncapSafetyTimer(45000)
	private var coroutineScope: CoroutineScope? = null
	private var previousScheduled: Job? = null
	private var questionMarkBlockedUntil: Instant = Instant.now()
	
	val defaultWeapons: List<WeaponObject>
		get() {
			return _defaultWeapons
		}
	val thrownWeapons: List<WeaponObject>
		get() {
			return _thrownWeapons
		}
	
	var defaultMode: NpcMode = NpcIdleMode(this)
	var activeMode: NpcMode? = null
		set(value) {
			field = value
			startMode(value ?: defaultMode)
		}
	
	var spawner: Spawner? = null
	var creatureId: String? = null
	var isHarvested: Boolean = false
	
	override fun onObjectEnteredAware(aware: SWGObject) {
		val player = getCreatureOrRider(aware) ?: return
		if (player.hasOptionFlags(OptionFlag.INVULNERABLE)) return
		
		playersNearby.add(player)
		activeMode?.onPlayerEnterAware(player, flatDistanceTo(player))
		
		checkAwareAttack(player)
	}
	
	override fun onObjectExitedAware(aware: SWGObject) {
		val player = getCreatureOrRider(aware) ?: return
		playersNearby.remove(player)
		activeMode?.onPlayerExitAware(player)
	}
	
	override fun onObjectMoveInAware(aware: SWGObject) {
		val player = getCreatureOrRider(aware) ?: return
		if (player.hasOptionFlags(OptionFlag.INVULNERABLE)) return
		playersNearby.add(player)
		
		val npcIsInvulnerable = hasOptionFlags(OptionFlag.INVULNERABLE)
		
		if (!npcIsInvulnerable) {
			val npcIsInCombat = isInCombat
			if (!npcIsInCombat) {
				val npcIsAggressiveTowardsPlayer = isAttackable(player)
				
				if (npcIsAggressiveTowardsPlayer) {
					val playerIsInLineOfSight = isLineOfSight(player)
					
					if (playerIsInLineOfSight) {
						val playerWorldLocation = player.worldLocation
						val aiWorldLocation = this.worldLocation
						val questionMarkRange = 64.0
						val distanceBetweenNpcAndPlayer = aiWorldLocation.distanceTo(playerWorldLocation)
						val playerIsInRange = distanceBetweenNpcAndPlayer <= questionMarkRange
						
						if (playerIsInRange) {
							val now = Instant.now()
							val questionMarkTimerExpired = now.isAfter(questionMarkBlockedUntil)
							if (questionMarkTimerExpired) {
								showQuestionMarkAboveNpc()
								questionMarkBlockedUntil = Instant.now().plusSeconds(60)
							}
						}
					}
				}
			}
		}
		
		checkAwareAttack(player)
	}
	
	private fun getCreatureOrRider(aware: SWGObject): CreatureObject? {
		if (aware.baselineType != BaselineType.CREO) return null
		
		if ((aware as CreatureObject).isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			val rider = aware.getSlottedObject("rider")
			if (rider !is CreatureObject) return null
			return rider
		} else {
			return aware
		}
	}
	
	private fun showQuestionMarkAboveNpc() {
		this.sendObservers(ShowFlyText(this.objectId, StringId("npc_reaction/flytext", "alert"), ShowFlyText.Scale.SMALL, red))
	}
	
	private fun checkAwareAttack(player: CreatureObject) {
		val spawner = this.spawner ?: return
		val incapSafetyTimerExpired = incapSafetyTimer.isExpired(System.currentTimeMillis(), player.lastIncapTime)
		
		if (isAttackable(player) && incapSafetyTimerExpired) {
			val distance = location.flatDistanceTo(player.location)
			val maxAggroDistance = if (player.isLoggedInPlayer) spawner.aggressiveRadius.toDouble()
			else if (!player.isPlayer) 30.0
			else -1.0 // Ensures the following if-statement will fail and remove the player from the list
			
			if (distance <= maxAggroDistance && isLineOfSight(player)) {
				if (spawner.behavior == AIBehavior.PATROL) {
					for (npc in spawner.npcs) npc.startCombat(listOf(player))
				} else {
					startCombat(listOf(player))
				}
			}
		}
	}
	
	override fun isWithinAwarenessRange(target: SWGObject): Boolean {
		assert(target is CreatureObject)
		return isAttackable(target as CreatureObject) && flatDistanceTo(target) <= 50
	}
	
	fun addDefaultWeapon(weapon: WeaponObject) {
		_defaultWeapons.add(weapon)
		weapon.systemMove(hiddenInventory)
		
		if ("object/weapon/creature/shared_creature_default_weapon.iff" == weapon.template) {
			addCommand("creatureMeleeAttack")
		} else {
			addCommand(weapon.type.defaultAttack)
		}
	}
	
	fun addThrownWeapon(weapon: WeaponObject) {
		_thrownWeapons.add(weapon)
		weapon.systemMove(hiddenInventory)
	}
	
	override fun setEquippedWeapon(weapon: WeaponObject) {
		val equipped = equippedWeapon
		equipped?.systemMove(hiddenInventory)
		weapon.moveToContainer(this)
		super.setEquippedWeapon(weapon)
	}
	
	fun startCombat(targets: Collection<CreatureObject>) {
		val mode = activeMode
		if (mode !is NpcCombatMode) {
			val newMode = NpcCombatMode(this, coroutineScope ?: return) // No coroutine? No AI
			newMode.addTargets(targets)
			activeMode = newMode
		} else {
			mode.addTargets(targets)
		}
	}
	
	fun start(coroutineScope: CoroutineScope) {
		this.coroutineScope = CoroutineScope(coroutineScope.coroutineContext + SupervisorJob())
		startMode(activeMode ?: defaultMode)
	}
	
	fun stop() {
		this.activeMode = null
		this.coroutineScope?.cancelAndWait()
		this.coroutineScope = null
	}
	
	val nearbyPlayers: Set<CreatureObject>
		get() = Collections.unmodifiableSet(playersNearby)
	
	private fun startMode(mode: NpcMode) {
		previousScheduled?.cancel()
		previousScheduled = coroutineScope?.let { mode.act(it) }
	}
	
}
