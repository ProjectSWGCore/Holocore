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
package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

class NpcCombatMode(obj: AIObject) : NpcMode(obj) {
	
	private val returnLocation = AtomicReference<NavigationPoint>(null)
	private val startCombatLocation = AtomicReference<Location>(null)
	private val targets = CopyOnWriteArraySet<CreatureObject>()
	private val iteration = AtomicLong(0)
	private val npcRunSpeed = PswgDatabase.config.getDouble(this, "npcRunSpeed", 9.0)
	private val npcMovement = NpcCombatMovement(obj, npcRunSpeed)
	
	private val primaryTarget: CreatureObject? // Don't attack if they're already dead
		get() = targets.stream()
				.filter { creo -> creo.isAttackable(ai) }
				.filter { creo -> (creo.posture != Posture.INCAPACITATED || spawner?.isDeathblow ?: false) && creo.posture != Posture.DEAD }
				.max(Comparator.comparingInt { ai.hateMap[it] ?: 0 }).orElse(null)
	
	override fun onPlayerMoveInAware(player: CreatureObject, distance: Double) {
		val spawner = this.spawner ?: return
		if (distance > spawner.aggressiveRadius) {
			// If out of aggressive range, and not actively in combat
			if (spawner.behavior == AIBehavior.PATROL && spawner.npcs.none { it.defenders.contains(player.objectId) })
				targets.remove(player)
			else if (!ai.defenders.contains(player.objectId))
				targets.remove(player)
		}
	}
	
	override fun onPlayerExitAware(player: CreatureObject) {
		targets.remove(player)
	}
	
	override fun onModeStart() {
		showExclamationMarkAboveNpc()
		StopNpcMovementIntent(ai).broadcast()
		returnLocation.set(NavigationPoint.at(ai.parent, ai.location, npcRunSpeed))
		startCombatLocation.set(ai.worldLocation)
	}
	
	private fun showExclamationMarkAboveNpc() {
		ai.sendObservers(ShowFlyText(ai.objectId, StringId("npc_reaction/flytext", "threaten"), ShowFlyText.Scale.SMALL, SWGColor.Reds.red))
	}

	override fun onModeEnd() {
		val obj = ai
		obj.lookAtTargetId = 0
	}
	
	override fun act() {
		if (ai.posture == Posture.DEAD) {
			return	// Don't waste CPU cycles if the NPC is dead
		}
		
		if (isRooted) {
			queueNextLoop(500)
			return
		}
		
		if (ai.worldLocation.flatDistanceTo(startCombatLocation.get()) > 100) {
			targets.clear() // We're too far away from home, no longer interested in combat
		}
		
		targets.removeIf { creo -> creo.posture == Posture.INCAPACITATED || creo.posture == Posture.DEAD }
		if (!targets.isEmpty()) {
			performCombatAction()
			queueNextLoop(500 + ThreadLocalRandom.current().nextLong(-200, 200))
		} else {
			ScheduleNpcModeIntent(ai, NpcNavigateMode(ai, returnLocation.get())).broadcast()
		}
	}
	
	fun addTargets(targets: Collection<CreatureObject>) {
		if (this.targets.addAll(targets))
			requestAssistance()
	}
	
	private fun performCombatAction() {
		val target = primaryTarget ?: return
		val weapon = ai.equippedWeapon
		val lineOfSight = ai.isLineOfSight(target)
		
		npcMovement.handleMovement(target, weapon, lineOfSight)
		
		if (lineOfSight && iteration.get() % 4 == 0L) {
			attack(target, weapon)
		}
		iteration.incrementAndGet()
	}
	
	private fun attack(target: CreatureObject, weapon: WeaponObject) {
		val obj = ai
		val distance = obj.worldLocation.distanceTo(target.worldLocation)
		if (distance > weapon.maxRange)
			return
		obj.lookAtTargetId = target.objectId
		// If we're close, angle towards target
		val myLocation = obj.location
		val targetLocation = target.location
		val headingTo = myLocation.getHeadingTo(targetLocation.position)
		MoveObjectIntent(obj, obj.parent, Location.builder(myLocation).setHeading(headingTo).build(), npcRunSpeed).broadcast()

		val x = obj.worldLocation.x.toFloat()
		val z = obj.worldLocation.z.toFloat()
		val terrainTemplate = ServerData.terrains.getTerrain(obj.terrain)
		val npcInWater = terrainTemplate != null && terrainTemplate.isWater(x, z)

		if (npcInWater) {
			return
		}

		if (target.posture == Posture.INCAPACITATED) {
			QueueCommandIntent(obj, target, "", ServerData.commands.getCommand("deathblow") ?: return, 0).broadcast()
			return
		}
		
		QueueCommandIntent(obj, target, "", ServerData.commands.getCommand(getWeaponCommand(weapon)) ?: return, 0).broadcast()
	}
	
	private fun requestAssistance() {
		val myLocation = ai.worldLocation
		val assistRange = spawner?.assistRadius?.toDouble() ?: return
		ai.aware.stream()
				.filter { AIObject::class.java.isInstance(it) } // get nearby AI
				.filter { ai -> ai.worldLocation.distanceTo(myLocation) < assistRange } // that can assist
				.map { AIObject::class.java.cast(it) }
				.filter { ai -> targets.stream().anyMatch { ai.isAttackable(it) } }
				.forEach { ai -> StartNpcCombatIntent(ai, targets).broadcast() }
	}
	
	private fun getWeaponCommand(weapon: WeaponObject): String {
		if (weapon.template == "object/weapon/creature/shared_creature_default_weapon.iff") {
			// Creature weapon, use the melee attack designed for creatures
			return "creatureMeleeAttack"
		}

		return weapon.type.defaultAttack
	}
	
}
