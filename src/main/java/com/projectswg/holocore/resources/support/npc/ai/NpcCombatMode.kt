package com.projectswg.holocore.resources.support.npc.ai

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.location.Point3D
import com.projectswg.common.data.objects.GameObjectType
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import java.util.*
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

class NpcCombatMode(obj: AIObject) : NpcMode(obj) {
	
	private val returnLocation = AtomicReference<NavigationPoint>(null)
	private val targets = CopyOnWriteArraySet<CreatureObject>()
	private val iteration = AtomicLong(0)
	private val npcRunSpeed = PswgDatabase.config.getDouble(this, "npcRunSpeed", 9.0)
	
	private val primaryTarget: CreatureObject? // Don't attack if they're already dead
		get() = targets.stream()
				.filter { creo -> creo.isAttackable(ai) }
				.filter { creo -> (creo.posture != Posture.INCAPACITATED || spawner.isDeathblow) && creo.posture != Posture.DEAD }
				.min(Comparator.comparingInt { it.health }).orElse(null)
	
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
		StopNpcMovementIntent.broadcast(ai)
		returnLocation.set(NavigationPoint.at(ai.parent, ai.location, npcRunSpeed))
	}
	
	override fun onModeEnd() {
		val obj = ai
		obj.intendedTargetId = 0
		obj.lookAtTargetId = 0
	}
	
	override fun act() {
		if (isRooted) {
			queueNextLoop(500)
			return
		}
		
		targets.removeIf { creo -> creo.posture == Posture.INCAPACITATED || creo.posture == Posture.DEAD }
		if (!targets.isEmpty()) {
			performCombatAction()
			queueNextLoop(500 + ThreadLocalRandom.current().nextLong(-50, 50))
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
		
		if (targetDistance > actionRange || !lineOfSight) {
			if (targetDistance > actionRange * 2) {
				val targetLocation = target.location
				val location = obj.location
				val moveHeading = location.getHeadingTo(targetLocation)
				val moveDistance = max(0.0, targetDistance - actionRange / 2) / 3
				val moveX = targetLocation.x + cos(moveHeading) * moveDistance
				val moveZ = targetLocation.z + sin(moveHeading) * moveDistance
				StartNpcMovementIntent.broadcast(obj, target.effectiveParent, Location.builder(targetLocation).setX(moveX).setZ(moveZ).setHeading(moveHeading).build(), npcRunSpeed)
			} else {
				val targetLocation = target.location
				val targetHeading = target.location.yaw + ThreadLocalRandom.current().nextDouble(-20.0, 20.0)
				val targetRange = actionRange / 2
				val moveX = targetLocation.x + cos(targetHeading) * targetRange
				val moveZ = targetLocation.z + sin(targetHeading) * targetRange
				val moveHeading = targetLocation.getHeadingTo(Point3D(moveX, targetLocation.y, moveZ)) + 180
				StartNpcMovementIntent.broadcast(obj, target.effectiveParent, Location.builder(targetLocation).setX(moveX).setZ(moveZ).setHeading(moveHeading).build(), npcRunSpeed)
			}
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
		obj.intendedTargetId = target.objectId
		obj.lookAtTargetId = target.objectId
		// If we're close, angle towards target
		val myLocation = obj.worldLocation
		val targetLocation = target.worldLocation
		MoveObjectIntent.broadcast(obj, obj.parent, Location.builder(myLocation).setHeading(myLocation.getHeadingTo(targetLocation)).build(), npcRunSpeed)
		
		if (target.posture == Posture.INCAPACITATED) {
			QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("deathblow"), 0)
			return
		}
		
		QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand(getWeaponCommand(weapon)), 0)
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
		val ai = weapon.parent as AIObject?
		
		return if (ai != null && GameObjectType.GOT_CREATURE == ai.gameObjectType) {
			// Creatures use different default attack abilities than humanoids do
			"creatureMeleeAttack"    // TODO this is a bit simple as there are ranged attacks available for some creatures as well.
		} else {
			when (weapon.type) {
				WeaponType.PISTOL -> "rangedShotPistol"
				WeaponType.RIFLE -> "rangedShotRifle"
				WeaponType.LIGHT_RIFLE -> "rangedShotLightRifle"
				WeaponType.CARBINE, WeaponType.HEAVY, WeaponType.HEAVY_WEAPON, WeaponType.DIRECTIONAL_TARGET_WEAPON -> "rangedShot"
				WeaponType.ONE_HANDED_MELEE, WeaponType.TWO_HANDED_MELEE, WeaponType.UNARMED, WeaponType.POLEARM_MELEE, WeaponType.THROWN -> "meleeHit"
				WeaponType.ONE_HANDED_SABER, WeaponType.TWO_HANDED_SABER, WeaponType.POLEARM_SABER -> "saberHit"
				else -> "meleeHit"
			}
		}
	}
	
}
