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
package com.projectswg.holocore.services.gameplay.world.travel

import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.holocore.intents.gameplay.combat.CreatureIncapacitatedIntent
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent
import com.projectswg.holocore.intents.gameplay.world.*
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent
import com.projectswg.holocore.intents.support.global.network.CloseConnectionIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.ObjectCreatedIntent
import com.projectswg.holocore.intents.support.objects.ObjectTeleportIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.vehicles
import com.projectswg.holocore.resources.support.data.server_info.mongodb.PswgDatabase.config
import com.projectswg.holocore.resources.support.global.network.DisconnectReason
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.ObjectCreator
import com.projectswg.holocore.resources.support.objects.swg.SWGObject
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject
import com.projectswg.holocore.resources.support.objects.swg.intangible.IntangibleObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup
import com.projectswg.holocore.utilities.HolocoreCoroutine
import com.projectswg.holocore.utilities.cancelAndWait
import com.projectswg.holocore.utilities.launchAfter
import com.projectswg.holocore.utilities.launchWithFixedDelay
import kotlinx.coroutines.Job
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class PlayerMountService : Service() {
	private val coroutineScope = HolocoreCoroutine.childScope()
	private val calledMounts: MutableMap<CreatureObject, MutableSet<Mount>> = ConcurrentHashMap()
	private val vehicleDecayJobs: MutableMap<CreatureObject, Job> = ConcurrentHashMap()

	override fun stop(): Boolean {
		coroutineScope.cancelAndWait()
		globallyStorePets() // Don't want mounts out and about when server starts up again

		return true
	}

	fun pcdForVehicleDeed(deedTemplate: String): String {
		assert(deedTemplate.startsWith("object/tangible/deed/")) { "invalid vehicle deed" }
		return deedTemplate.replace("tangible/deed/vehicle_deed", "intangible/vehicle").replace("deed", "pcd")
	}

	@IntentHandler
	private fun handleCreatureIncapacitatedIntent(cii: CreatureIncapacitatedIntent) {
		val player = cii.incappee
		if (player.isPlayer) exitMount(player)
	}

	@IntentHandler
	private fun handleCreatureKilledIntent(cki: CreatureKilledIntent) {
		val player = cki.corpse
		if (player.isPlayer) exitMount(player)
	}

	@IntentHandler
	private fun handleExecuteCommandIntent(eci: ExecuteCommandIntent) {
		val cmdName = eci.command.name
		val target = eci.target as? CreatureObject ?: return

		if (cmdName == "mount") enterMount(eci.source, target)
		else if (cmdName == "dismount") exitMount(eci.source, target)
	}

	@IntentHandler
	private fun handlePlayerTransformedIntent(pti: PlayerTransformedIntent) {
		val player = pti.player
		val mounts = calledMounts[player] ?: return

		for (m in mounts) {
			if (!player.aware.contains(m.mount)) storeMount(player, m.mount, m.petControlDevice)
		}
	}

	@IntentHandler
	private fun handleMount(pmi: MountIntent) {
		enterMount(pmi.creature, pmi.pet)
	}

	@IntentHandler
	private fun handleDismount(pmi: DismountIntent) {
		exitMount(pmi.creature, pmi.pet)
	}

	@IntentHandler
	private fun handlePetDeviceCall(pci: PetDeviceCallIntent) {
		callMount(pci.creature, pci.controlDevice)
	}

	@IntentHandler
	private fun handlePetStore(psi: StoreMountIntent) {
		val creature = psi.creature
		val mount = psi.pet

		val mounts = calledMounts[creature]
		if (mounts != null) {
			for (p in mounts) {
				if (p.mount === mount) {
					storeMount(creature, mount, p.petControlDevice)
					return
				}
			}
		}
		broadcastPersonal(psi.creature.owner!!, "Could not find mount to store!")
	}

	@IntentHandler
	private fun handlePetDeviceStore(psi: PetDeviceStoreIntent) {
		val creature = psi.creature
		val pcd = psi.controlDevice

		val mounts = calledMounts[creature]
		if (mounts != null) {
			for (mount in mounts) {
				if (mount.petControlDevice === pcd) {
					storeMount(creature, mount.mount, pcd)
					return
				}
			}
		}
		pcd.count = IntangibleObject.COUNT_PCD_STORED
	}

	@IntentHandler
	private fun handleVehicleDeedGenerate(vdgi: VehicleDeedGenerateIntent) {
		if (!vdgi.deed.template.startsWith("object/tangible/deed/vehicle_deed/")) {
			broadcastPersonal(vdgi.creature.owner!!, "Invalid vehicle deed!")
			return
		}
		generateVehicle(vdgi.creature, vdgi.deed)
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		val creature = pei.player.creatureObject ?: return
		when (pei.event) {
			PlayerEvent.PE_LOGGED_OUT, PlayerEvent.PE_DISAPPEAR, PlayerEvent.PE_DESTROYED, PlayerEvent.PE_SERVER_KICKED -> storeMounts(creature)
			else                                                                                                        -> {}
		}
	}

	@IntentHandler
	private fun handleObjectTeleportIntent(oti: ObjectTeleportIntent) {
		val obj = oti.obj
		if (oti.oldParent === oti.newParent) return
		if (obj !is CreatureObject) return

		val oldParent = oti.oldParent as? CreatureObject ?: return

		if (obj.isStatesBitmask(CreatureState.RIDING_MOUNT) && isMountable(oldParent) && oldParent.isStatesBitmask(CreatureState.MOUNTED_CREATURE)) {
			// Need to do this manually because most of the standard checks don't work in this case
			emergencyDismount(obj, oldParent)
			StoreMountIntent(obj, oldParent).broadcast()
		}
	}

	private fun generateVehicle(creator: CreatureObject, deed: SWGObject) {
		val pcdTemplate = pcdForVehicleDeed(deed.template)
		val vehicleInfo = vehicles().getVehicleFromPcdIff(pcdTemplate)
		if (vehicleInfo == null) {
			StandardLog.onPlayerError(this, creator, "Unknown vehicle created from deed: %s", deed.template)
			return
		}
		val vehicleControlDevice = ObjectCreator.createObjectFromTemplate(pcdTemplate) as IntangibleObject

		DestroyObjectIntent(deed).broadcast()

		vehicleControlDevice.setServerAttribute(ServerAttribute.PCD_PET_TEMPLATE, vehicleInfo.objectTemplate)
		vehicleControlDevice.count = IntangibleObject.COUNT_PCD_STORED
		vehicleControlDevice.moveToContainer(creator.datapad)
		ObjectCreatedIntent(vehicleControlDevice).broadcast()

		broadcastPersonal(creator.owner!!, "@pet/pet_menu:device_added")

		callMount(creator, vehicleControlDevice) // Once generated, the vehicle is called
	}

	private fun callMount(player: CreatureObject, mountControlDevice: IntangibleObject) {
		if (player.parent != null || player.isInCombat) {
			return
		}
		if (player.datapad !== mountControlDevice.parent) {
			StandardLog.onPlayerError(this, player, "disconnecting - attempted to call another player's mount [%s]", mountControlDevice)
			CloseConnectionIntent(player.owner!!, DisconnectReason.SUSPECTED_HACK).broadcast()
			return
		}

		val template = checkNotNull(mountControlDevice.getServerTextAttribute(ServerAttribute.PCD_PET_TEMPLATE)) { "mount control device doesn't have mount template attribute" }
		val vehicleInfo = vehicles().getVehicleFromIff(template)
		if (vehicleInfo == null) {
			StandardLog.onPlayerError(this, player, "unknown vehicle template '%s', unable to proceed", template)
			return
		}
		val mount = ObjectCreator.createObjectFromTemplate(template) as CreatureObject
		mount.systemMove(null, player.location)
		mount.addOptionFlags(OptionFlag.MOUNT) // The mount won't appear properly if this isn't set
		mount.faction = player.faction
		mount.ownerId = player.objectId // Client crash if this isn't set before making anyone aware

		// TODO after combat there's a delay
		// TODO update faction status on mount if necessary
		val mountRecord = Mount(mountControlDevice, mount)
		val mounts = calledMounts.computeIfAbsent(player) { c: CreatureObject? -> CopyOnWriteArraySet() }
		if (mountControlDevice.count == IntangibleObject.COUNT_PCD_CALLED || !mounts.add(mountRecord)) {
			StandardLog.onPlayerTrace(this, player, "already called mount %s", mount)
			return
		}
		if (mounts.size > mountLimit) {
			mounts.remove(mountRecord)
			StandardLog.onPlayerTrace(this, player, "hit mount limit of %d", mountLimit)
			broadcastPersonal(player.owner!!, "@pet/pet_menu:at_max")
			return
		}
		mountControlDevice.count = IntangibleObject.COUNT_PCD_CALLED
		mount.setRunSpeed(vehicleInfo.speed)
		mount.setWalkSpeed(vehicleInfo.minSpeed)
		mount.setTurnScale(vehicleInfo.turnRateMax.toDouble())
		mount.setAccelScale(vehicleInfo.accelMax)
		mount.putCustomization("/private/index_speed_max", (vehicleInfo.speed * 10.0).toInt())
		mount.putCustomization("/private/index_turn_rate_min", vehicleInfo.turnRate)
		mount.putCustomization("/private/index_turn_rate_max", vehicleInfo.turnRateMax)
		mount.putCustomization("/private/index_accel_min", (vehicleInfo.accelMin * 10.0).toInt())
		mount.putCustomization("/private/index_accel_max", (vehicleInfo.accelMax * 10.0).toInt())
		mount.putCustomization("/private/index_decel", (vehicleInfo.decel * 10.0).toInt())
		mount.putCustomization("/private/index_damp_roll", (vehicleInfo.dampingRoll * 10.0).toInt())
		mount.putCustomization("/private/index_damp_pitch", (vehicleInfo.dampingPitch * 10.0).toInt())
		mount.putCustomization("/private/index_damp_height", (vehicleInfo.dampingHeight * 10.0).toInt())
		mount.putCustomization("/private/index_glide", (vehicleInfo.glide * 10.0).toInt())
		mount.putCustomization("/private/index_banking", vehicleInfo.bankingAngle.toInt())
		mount.putCustomization("/private/index_hover_height", (vehicleInfo.hoverHeight * 10.0).toInt())
		mount.putCustomization("/private/index_auto_level", (vehicleInfo.autoLevel * 100.0).toInt())

		ObjectCreatedIntent(mount).broadcast()
		StandardLog.onPlayerTrace(this, player, "called mount %s at %s %s", mount, mount.terrain, mount.location.position)
		cleanupCalledMounts()

		// Vehicle loses condition shortly after being called
		coroutineScope.launchAfter(2, TimeUnit.SECONDS) {
			decayVehicle(player, mount, vehicleInfo.decayRate)
		}

		// Vehicle slowly loses condition over time, as long as it's called out
		vehicleDecayJobs[mount] = coroutineScope.launchWithFixedDelay(10, TimeUnit.MINUTES) {
			decayVehicle(player, mount, vehicleInfo.decayRate / 2)
		}

		if (isJetpack(mount)) {
			automaticallyMountJetpack(player, mount)
		}
	}

	private fun automaticallyMountJetpack(player: CreatureObject, mount: CreatureObject) {
		coroutineScope.launchAfter(1, TimeUnit.SECONDS) {
			enterMount(player, mount)
		}
	}

	private fun isJetpack(mount: CreatureObject): Boolean {
		return "object/mobile/vehicle/shared_jetpack.iff" == mount.template
	}

	private fun decayVehicle(player: CreatureObject, mount: CreatureObject, conditionDamage: Int) {
		val sanitizedConditionDamage = conditionDamage.coerceAtMost(remainingHitPoints(mount))
		mount.conditionDamage += sanitizedConditionDamage

		StandardLog.onPlayerTrace(this, player, "mount %s condition decayed by %d points, condition is now %d/%d", mount, sanitizedConditionDamage, remainingHitPoints(mount), mount.maxHitPoints)
	}

	private fun remainingHitPoints(mount: CreatureObject): Int {
		return mount.maxHitPoints - mount.conditionDamage
	}

	private fun enterMount(player: CreatureObject, mount: CreatureObject) {
		if (!isMountable(mount) || mount.parent != null) {
			StandardLog.onPlayerTrace(this, player, "attempted to mount %s when it's not mountable", mount)
			return
		}

		if (player.parent != null || player.posture != Posture.UPRIGHT) return

		if (player.objectId == mount.ownerId) {
			player.moveToSlot(mount, "rider", mount.getArrangementId(player))
		} else if (mount.getSlottedObject("rider") != null) {
			val group = ObjectLookup.getObjectById(player.groupId) as GroupObject?
			if (group == null || !group.isInGroup(mount.ownerId)) {
				StandardLog.onPlayerTrace(this, player, "attempted to mount %s when not in the same group as the owner", mount)
				return
			}
			var added = false
			for (i in 1..7) {
				if (!mount.hasSlot("rider$i")) {
					StandardLog.onPlayerTrace(this, player, "attempted to mount %s when no slots remain", mount)
					return
				}
				if (mount.getSlottedObject("rider$i") == null) {
					player.moveToSlot(mount, "rider$i", mount.getArrangementId(player))
					added = true
					break
				}
			}
			if (!added) {
				StandardLog.onPlayerTrace(this, player, "attempted to mount %s when no slots remain", mount)
				return
			}
		} else {
			return
		}

		player.setStatesBitmask(CreatureState.RIDING_MOUNT)
		mount.setStatesBitmask(CreatureState.MOUNTED_CREATURE)
		mount.posture = Posture.DRIVING_VEHICLE
		player.inheritMovement(mount)
		StandardLog.onPlayerEvent(this, player, "mounted %s", mount)
	}

	private fun exitMount(player: CreatureObject) {
		if (!player.isStatesBitmask(CreatureState.RIDING_MOUNT)) return
		val parent = player.parent as? CreatureObject ?: return
		if (isMountable(parent) && parent.isStatesBitmask(CreatureState.MOUNTED_CREATURE)) exitMount(player, parent)
	}

	private fun exitMount(player: CreatureObject, mount: CreatureObject) {
		if (!isMountable(mount)) {
			StandardLog.onPlayerTrace(this, player, "attempted to dismount %s when it's not mountable", mount)
			return
		}

		if (player.parent === mount) dismount(player, mount)

		if (mount.getSlottedObject("rider") == null) {
			for (child in mount.slottedObjects) {
				assert(child is CreatureObject)
				dismount(child as CreatureObject, mount)
			}
			mount.clearStatesBitmask(CreatureState.MOUNTED_CREATURE)
			mount.posture = Posture.UPRIGHT
		}
	}

	private fun storeMount(player: CreatureObject, mount: CreatureObject, mountControlDevice: IntangibleObject) {
		if (player.isInCombat) return

		if (isMountable(mount) && mount.ownerId == player.objectId) {
			// Dismount anyone riding the mount about to be stored
			for (rider in mount.slottedObjects) {
				assert(rider is CreatureObject)
				exitMount(rider as CreatureObject, mount)
			}
			assert(mount.slottedObjects.isEmpty())


			// Remove the record of the mount
			val mounts: MutableCollection<Mount>? = calledMounts[player]
			mounts?.remove(Mount(mountControlDevice, mount))

			// Cancel the periodic decay job
			vehicleDecayJobs[mount]?.cancel()

			// Destroy the mount
			mountControlDevice.count = IntangibleObject.COUNT_PCD_STORED
			player.broadcast(DestroyObjectIntent(mount))
			StandardLog.onPlayerTrace(this, player, "stored mount %s at %s %s", mount, mount.terrain, mount.location.position)
		}
		cleanupCalledMounts()
	}

	private fun storeMounts(player: CreatureObject) {
		val mounts = calledMounts.remove(player) ?: return

		for (mount in mounts) {
			storeMount(player, mount.mount, mount.petControlDevice)
		}
	}

	/**
	 * Stores all called mounts by all players
	 */
	private fun globallyStorePets() {
		calledMounts.keys.forEach(Consumer { player: CreatureObject -> this.storeMounts(player) })
	}

	/**
	 * Removes mount records where there are no mounts called
	 */
	private fun cleanupCalledMounts() {
		calledMounts.entries.removeIf { e: Map.Entry<CreatureObject, Set<Mount>> -> e.value.isEmpty() }
	}

	private fun dismount(player: CreatureObject, mount: CreatureObject) {
		assert(player.parent === mount)
		player.clearStatesBitmask(CreatureState.RIDING_MOUNT)
		player.moveToContainer(null, mount.location)
		player.resetMovement()
		StandardLog.onPlayerEvent(this, player, "dismounted %s", mount)

		if (isJetpack(mount)) {
			automaticallyStoreJetpack(player, mount)
		}
	}

	private fun automaticallyStoreJetpack(player: CreatureObject, mount: CreatureObject) {
		coroutineScope.launchAfter(1, TimeUnit.SECONDS) {
			val mounts = calledMounts[player]
			if (mounts != null) {
				for (p in mounts) {
					if (p.mount === mount) {
						storeMount(player, mount, p.petControlDevice)
					}
				}
			}
		}
	}

	private fun emergencyDismount(player: CreatureObject, mount: CreatureObject) {
		player.clearStatesBitmask(CreatureState.RIDING_MOUNT)
		player.resetMovement()
		if (mount.getSlottedObject("rider") == null) {
			for (child in mount.slottedObjects) {
				assert(child is CreatureObject)
				dismount(child as CreatureObject, mount)
			}
		}
		mount.clearStatesBitmask(CreatureState.MOUNTED_CREATURE)
		mount.posture = Posture.UPRIGHT
		StandardLog.onPlayerEvent(this, player, "dismounted %s", mount)
	}

	private val mountLimit: Int
		get() = config.getInt(this, "mountLimit", 1)

	private class Mount(val petControlDevice: IntangibleObject, val mount: CreatureObject) {
		override fun hashCode(): Int {
			return petControlDevice.hashCode()
		}

		override fun equals(o: Any?): Boolean {
			return (o is Mount) && o.petControlDevice == petControlDevice
		}
	}

	companion object {
		private fun isMountable(mount: CreatureObject): Boolean {
			return mount.hasOptionFlags(OptionFlag.MOUNT)
		}
	}
}
