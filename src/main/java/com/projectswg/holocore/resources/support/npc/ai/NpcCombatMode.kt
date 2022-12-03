package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent
import com.projectswg.holocore.resources.support.color.SWGColor
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos
import kotlin.math.sin

class NpcCombatMode(obj: AIObject) : NpcMode(obj) {
	
	private val returnLocation = AtomicReference<NavigationPoint>(null)
	private val startCombatLocation = AtomicReference<Location>(null)
	private val targets = CopyOnWriteArraySet<CreatureObject>()
	private val iteration = AtomicLong(0)
	private val npcRunSpeed = PswgDatabase.config.getDouble(this, "npcRunSpeed", 9.0)
	
	private val primaryTarget: CreatureObject? // Don't attack if they're already dead
		get() = targets.stream()
				.filter { creo -> creo.isAttackable(ai) }
				.filter { creo -> (creo.posture != Posture.INCAPACITATED || spawner.isDeathblow) && creo.posture != Posture.DEAD }
				.max(Comparator.comparingInt { ai.hateMap[it] ?: 0 }).orElse(null)
	
	override fun onPlayerMoveInAware(player: CreatureObject, distance: Double) {
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
		StopNpcMovementIntent.broadcast(ai)
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
			ScheduleNpcModeIntent.broadcast(ai, NpcNavigateMode(ai, returnLocation.get()))
		}
	}
	
	fun addTargets(targets: Collection<CreatureObject>) {
		if (this.targets.addAll(targets))
			requestAssistance()
	}
	
	private fun performCombatAction() {
		val target = primaryTarget ?: return
		
		val obj = ai
		val weapon = obj.equippedWeapon
		val targetDistance = target.worldLocation.distanceTo(obj.worldLocation)
		val attackRange = weapon.maxRange.toDouble()
		val actionRange = attackRange / 2
		val lineOfSight = obj.isLineOfSight(target)

		if (ai.walkSpeed > 0 && (targetDistance > actionRange || !lineOfSight)) {
			val targetLocation = target.location
			val targetHeading = target.location.yaw + ThreadLocalRandom.current().nextDouble(-75.0, 75.0)
			val targetRange = actionRange / 2
			val moveX = targetLocation.x + sin(Math.toRadians(targetHeading)) * targetRange
			val moveZ = targetLocation.z + cos(Math.toRadians(targetHeading)) * targetRange
			val moveHeading = targetLocation.getHeadingTo(Point3D(moveX, targetLocation.y, moveZ)) + 180
			StartNpcMovementIntent.broadcast(obj, target.effectiveParent, Location.builder(targetLocation).setX(moveX).setZ(moveZ).setHeading(moveHeading).build(), npcRunSpeed)
		}
		
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
		MoveObjectIntent.broadcast(obj, obj.parent, Location.builder(myLocation).setHeading(headingTo).build(), npcRunSpeed)
		
		if (target.posture == Posture.INCAPACITATED) {
			QueueCommandIntent.broadcast(obj, target, "", ServerData.commands.getCommand("deathblow"), 0)
			return
		}
		
		QueueCommandIntent.broadcast(obj, target, "", ServerData.commands.getCommand(getWeaponCommand(weapon)), 0)
	}
	
	private fun requestAssistance() {
		val myLocation = ai.worldLocation
		val assistRange = spawner.assistRadius.toDouble()
		ai.aware.stream()
				.filter { AIObject::class.java.isInstance(it) } // get nearby AI
				.filter { ai -> ai.worldLocation.distanceTo(myLocation) < assistRange } // that can assist
				.map { AIObject::class.java.cast(it) }
				.filter { ai -> targets.stream().anyMatch { ai.isAttackable(it) } }
				.forEach { ai -> StartNpcCombatIntent.broadcast(ai, targets) }
	}
	
	private fun getWeaponCommand(weapon: WeaponObject): String {
		if (weapon.template == "object/weapon/creature/shared_creature_default_weapon.iff") {
			// Creature weapon, use the melee attack designed for creatures
			return "creatureMeleeAttack"
		}

		return weapon.type.defaultAttack
	}
	
}
