/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.npc.spawn

import com.projectswg.common.data.encodables.tangible.PvpFlag
import com.projectswg.common.data.encodables.tangible.PvpStatus
import com.projectswg.common.data.location.Location
import com.projectswg.common.data.objects.GameObjectType
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionIntent
import com.projectswg.holocore.intents.gameplay.gcw.UpdateFactionStatusIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.npcStats
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.npcWeaponRanges
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.terrains
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.npcEquipment
import com.projectswg.holocore.resources.support.data.server_info.loader.combat.FactionLoader.Faction
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStatLoader.DetailNpcStatInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStatLoader.NpcStatInfo
import com.projectswg.holocore.resources.support.data.server_info.loader.npc.NpcStaticSpawnLoader.SpawnerFlag
import com.projectswg.holocore.resources.support.npc.ai.NpcLoiterMode
import com.projectswg.holocore.resources.support.npc.ai.NpcPatrolMode
import com.projectswg.holocore.resources.support.npc.ai.NpcTurningMode
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.ObjectCreator.ObjectCreationException
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureDifficulty
import com.projectswg.holocore.resources.support.objects.swg.custom.AIBehavior
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponClass
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType
import me.joshlarson.jlcommon.control.IntentChain
import me.joshlarson.jlcommon.log.Log
import me.joshlarson.jlcommon.utilities.Arguments
import java.util.*
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.cos
import kotlin.math.sin

object NPCCreator {
	fun createAllNPCs(spawner: Spawner): Collection<AIObject> {
		Arguments.validate(spawner.minLevel <= spawner.maxLevel, "min level must be less than max level")
		val amount = spawner.amount
		val npcs: MutableCollection<AIObject> = ArrayList()

		for (i in 0 until amount) {
			npcs.add(createSingleNpc(spawner))
		}

		return npcs
	}

	fun createSingleNpc(spawner: Spawner): AIObject {
		val combatLevel = ThreadLocalRandom.current().nextInt(spawner.minLevel, spawner.maxLevel + 1)
		val obj = ObjectCreator.createObjectFromTemplate(spawner.getRandomIffTemplate(), AIObject::class.java)

		val equipmentId = spawner.equipmentId
		if (equipmentId != null) {
			addEquipmentItemsToNpc(obj, equipmentId)
		}

		val npcStats = npcStats()[combatLevel]!!
		val detailNpcStat = getDetailedNpcStats(npcStats, spawner.difficulty)
		obj.spawner = spawner
		obj.systemMove(spawner.egg.parent, behaviorLocation(spawner))
		obj.objectName = spawner.name
		obj.setLevel(combatLevel)
		obj.difficulty = spawner.difficulty
		obj.maxHealth = detailNpcStat.health
		obj.health = detailNpcStat.health
		obj.maxAction = detailNpcStat.action
		obj.action = detailNpcStat.action
		obj.moodAnimation = spawner.mood
		obj.creatureId = spawner.npcId
		obj.setWalkSpeed(spawner.movementSpeed)
		obj.height = getScale(spawner)

		val def = detailNpcStat.def
		val toHit = detailNpcStat.toHit
		for (weaponClass in WeaponClass.entries) {
			obj.adjustSkillmod(weaponClass.defenseSkillMod, def, 0)
			obj.adjustSkillmod(weaponClass.accuracySkillMod, toHit, 0)
		}

		val hue = spawner.hue
		if (hue != 0) {
			// No reason to add color customization if the value is default anyway
			obj.putCustomization("/private/index_color_1", hue)
		}

		// Assign weapons
		try {
			spawner.defaultWeapon.stream().map { w: String -> createWeapon(detailNpcStat, w) }.filter(Objects::nonNull).forEach { weapon: WeaponObject? -> obj.addDefaultWeapon(weapon) }
			spawner.thrownWeapon.stream().map { w: String -> createWeapon(detailNpcStat, w) }.filter(Objects::nonNull).forEach { weapon: WeaponObject? -> obj.addThrownWeapon(weapon) }
			val defaultWeapons = obj.defaultWeapons
			if (defaultWeapons.isNotEmpty()) obj.equippedWeapon = defaultWeapons[ThreadLocalRandom.current().nextInt(defaultWeapons.size)]
		} catch (t: Throwable) {
			Log.w(t)
		}

		when (spawner.behavior) {
			AIBehavior.LOITER -> obj.defaultMode = NpcLoiterMode(obj, spawner.loiterRadius.toDouble())
			AIBehavior.TURN   -> obj.defaultMode = NpcTurningMode(obj)
			AIBehavior.PATROL -> obj.defaultMode = NpcPatrolMode(obj, spawner.patrolRoute ?: listOf())
			else              -> {}
		}
		setFlags(obj, spawner)
		setNPCFaction(obj, spawner.faction, spawner.isSpecForce)

		spawner.addNPC(obj)
		ObjectCreatedIntent(obj).broadcast()
		return obj
	}

	private fun addEquipmentItemsToNpc(`object`: AIObject, equipmentId: Long) {
		val equipmentInfo = npcEquipment.getEquipmentInfo(equipmentId)

		if (equipmentInfo != null) {
			addEquipmentToSlot(equipmentInfo.leftHandTemplate, "hold_l", `object`)
			addEquipmentToSlot(equipmentInfo.rightHandTemplate, "hold_r", `object`)
		}
	}

	private fun addEquipmentToSlot(objectTemplate: String, slotName: String, `object`: AIObject) {
		if (objectTemplate.isNotBlank()) {
			val rightHandObject = ObjectCreator.createObjectFromTemplate(objectTemplate)
			rightHandObject.moveToSlot(`object`, slotName, 4)
		}
	}

	private fun setFlags(`object`: AIObject, spawner: Spawner) {
		when (spawner.spawnerFlag) {
			SpawnerFlag.AGGRESSIVE   -> {
				`object`.setPvpFlags(PvpFlag.CAN_ATTACK_YOU)
				`object`.addOptionFlags(OptionFlag.AGGRESSIVE)
				`object`.setPvpFlags(PvpFlag.YOU_CAN_ATTACK)
				`object`.addOptionFlags(OptionFlag.HAM_BAR)
			}

			SpawnerFlag.ATTACKABLE   -> {
				`object`.setPvpFlags(PvpFlag.YOU_CAN_ATTACK)
				`object`.addOptionFlags(OptionFlag.HAM_BAR)
			}

			SpawnerFlag.INVULNERABLE -> `object`.addOptionFlags(OptionFlag.INVULNERABLE)
			SpawnerFlag.QUEST        -> {
				`object`.addOptionFlags(OptionFlag.INVULNERABLE)
				`object`.addOptionFlags(OptionFlag.INTERESTING)
			}
		}
	}

	private fun setNPCFaction(`object`: TangibleObject, faction: Faction, specForce: Boolean) {
		if (specForce) {
			IntentChain.broadcastChain(
				UpdateFactionIntent(`object`, faction), UpdateFactionStatusIntent(`object`, PvpStatus.SPECIALFORCES)
			)
		} else {
			UpdateFactionIntent(`object`, faction).broadcast()
		}
	}

	private fun getScale(spawner: Spawner): Double {
		val scaleMin = spawner.scaleMin
		val scaleMax = spawner.scaleMax
		return if (scaleMin == scaleMax) {
			// Min and max are the same. Using either of them is fine.
			scaleMin
		} else {
			// There's a gap between min and max. Let's generate a random number between them (both inclusive)
			ThreadLocalRandom.current().nextDouble(scaleMin, scaleMax + 0.1) // +0.1 to make scaleMax inclusive
		}
	}

	private fun behaviorLocation(spawner: Spawner): Location {
		val builder = Location.builder(spawner.location)

		when (spawner.behavior) {
			AIBehavior.LOITER -> {
				run {
					// Random location within float radius of spawner
					val angle = ThreadLocalRandom.current().nextDouble(Math.PI * 2)
					val distance = ThreadLocalRandom.current().nextDouble(spawner.loiterRadius.toDouble())
					val offsetX = (cos(angle) * distance).toInt()
					val offsetZ = (sin(angle) * distance).toInt()
					builder.translatePosition(offsetX.toDouble(), 0.0, offsetZ.toDouble())
				}
				run {
					// Random heading when spawned
					val randomHeading = randomBetween(0, 360) // Can't use negative numbers as minimum
					builder.setHeading(randomHeading.toDouble())
				}
			}

			AIBehavior.TURN   -> {
				val randomHeading = randomBetween(0, 360)
				builder.setHeading(randomHeading.toDouble())
			}

			else              -> {}
		}
		if (spawner.buildingId.isEmpty() || spawner.buildingId.endsWith("_world")) builder.setY(terrains().getHeight(builder))

		return builder.build()
	}

	private fun getDetailedNpcStats(npcStats: NpcStatInfo, difficulty: CreatureDifficulty): DetailNpcStatInfo {
		return when (difficulty) {
			CreatureDifficulty.NORMAL -> npcStats.normalDetailStat
			CreatureDifficulty.ELITE  -> npcStats.eliteDetailStat
			CreatureDifficulty.BOSS   -> npcStats.bossDetailStat
		}
	}

	/**
	 * Generates a random number between from (inclusive) and to (inclusive)
	 * @param from a positive minimum value
	 * @param to maximum value, which is larger than the minimum value
	 * @return a random number between the two, both inclusive
	 */
	private fun randomBetween(from: Int, to: Int): Int {
		return ThreadLocalRandom.current().nextInt((to - from) + 1) + from
	}

	private fun createWeapon(detailNpcStat: DetailNpcStatInfo, template: String): WeaponObject? {
		try {
			val weapon = ObjectCreator.createObjectFromTemplate(template) as WeaponObject
			val weaponType = getWeaponType(weapon.gameObjectType)

			weapon.minDamage = (detailNpcStat.damagePerSecond * 2 * 0.90).toInt()
			weapon.maxDamage = detailNpcStat.damagePerSecond * 2
			val range = npcWeaponRanges().getWeaponRange(template)
			if (range == -1) Log.w("Failed to load weapon range for: %s", template)
			weapon.minRange = range.toFloat()
			weapon.maxRange = range.toFloat()
			weapon.type = weaponType
			// TODO set damage type, since all NPC weapons shouldn't deal kinetic damage
			return weapon
		} catch (e: ObjectCreationException) {
			Log.w("Weapon template does not exist: %s", template)
			return null
		}
	}

	/**
	 * Somewhat accurate way of determining a WeaponType based on a GameObjectType.
	 * Problem is that GOT_WEAPON_RANGED_RIFLE can be both a rifle and a heavy weapon, but we assume it's a rifle since NPCs don't use heavy weapons.
	 *
	 * @param weaponObjectType to determine a WeaponType based on
	 * @return `WeaponType` that was determined from the given `weaponObjectType` param
	 */
	private fun getWeaponType(weaponObjectType: GameObjectType): WeaponType {
		return when (weaponObjectType) {
			GameObjectType.GOT_WEAPON_HEAVY_MINE     -> WeaponType.HEAVY
			GameObjectType.GOT_WEAPON_HEAVY_MISC     -> WeaponType.HEAVY
			GameObjectType.GOT_WEAPON_HEAVY_SPECIAL  -> WeaponType.HEAVY
			GameObjectType.GOT_WEAPON_MELEE_1H       -> WeaponType.ONE_HANDED_MELEE
			GameObjectType.GOT_WEAPON_MELEE_2H       -> WeaponType.TWO_HANDED_MELEE
			GameObjectType.GOT_WEAPON_MELEE_MISC     -> WeaponType.ONE_HANDED_MELEE
			GameObjectType.GOT_WEAPON_MELEE_POLEARM  -> WeaponType.POLEARM_MELEE
			GameObjectType.GOT_WEAPON_RANGED_CARBINE -> WeaponType.CARBINE
			GameObjectType.GOT_WEAPON_RANGED_PISTOL  -> WeaponType.PISTOL
			GameObjectType.GOT_WEAPON_RANGED_RIFLE   -> WeaponType.RIFLE
			GameObjectType.GOT_WEAPON_RANGED_THROWN  -> WeaponType.THROWN
			else                                     -> WeaponType.UNARMED
		}
	}
}
