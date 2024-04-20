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
package com.projectswg.holocore.services.gameplay.entertainment

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.encodables.tangible.Posture
import com.projectswg.common.network.packets.swg.zone.object_controller.Animation
import com.projectswg.holocore.intents.gameplay.entertainment.dance.DanceIntent
import com.projectswg.holocore.intents.gameplay.entertainment.dance.FlourishIntent
import com.projectswg.holocore.intents.gameplay.entertainment.dance.WatchIntent
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerTransformedIntent
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader.Companion.performances
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.function.Consumer

class EntertainmentService : Service() {
	private val performerMap = mutableMapOf<Long, Performance>()
	private val executorService = Executors.newSingleThreadScheduledExecutor()

	override fun terminate(): Boolean {
		executorService.shutdownNow()
		return super.terminate()
	}

	@IntentHandler
	private fun handleDanceIntent(di: DanceIntent) {
		val player = di.player
		val dancer = player.creatureObject
		val danceName = di.danceName
		if (di.isStartDance) {
			// This intent wants the creature to start dancing
			// If we're changing dance, allow them to do so
			val changeDance = di.isChangeDance
			if (!changeDance && dancer.isPerforming) {
				SystemMessageIntent(player, "@performance:already_performing_self").broadcast()
			} else if (performances().getPerformanceByName(danceName) != null) {
				// The dance name is valid.
				if (dancer.hasCommand("startDance+$danceName")) {
					if (changeDance) {    // If they're changing dance, we just need to change their animation.
						changeDance(dancer, danceName)
					} else {    // Otherwise, they should begin performing now
						startDancing(player, danceName)
					}
				} else {
					// This creature doesn't have the ability to perform this dance.
					SystemMessageIntent(player, "@performance:dance_lack_skill_self").broadcast()
				}
			} else {
				// This dance name is invalid
				SystemMessageIntent(player, "@performance:dance_unknown_self").broadcast()
			}
		} else {
			// This intent wants the creature to stop dancing
			stopDancing(player)
		}
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		val player = pei.player
		val creature = player.creatureObject ?: return
		when (pei.event) {
			PlayerEvent.PE_LOGGED_OUT -> handlePlayerLoggedOut(creature)
			PlayerEvent.PE_ZONE_IN_SERVER -> handlePlayerZoneIn(creature)
			PlayerEvent.PE_DISAPPEAR -> handlePlayerDisappear(player)
			else -> {}
		}
	}

	private fun handlePlayerDisappear(player: Player) {
		val creature = player.creatureObject
		// If a spectator disappears, they need to stop watching and be removed from the audience
		val performerId = creature.performanceListenTarget
		val performance = performerMap[performerId]
		val spectating = performance?.removeSpectator(creature) == true
		if (performerId != 0L && spectating) {
			stopWatching(player, false)
		}

		// If a performer disappears, the audience needs to be cleared
		// They're also removed from the map of active performers.
		if (creature.isPerforming) {
			performerMap[creature.objectId]?.clearSpectators()
			performerMap.remove(creature.objectId)
		}
	}

	private fun handlePlayerZoneIn(creature: CreatureObject) {
		if (creature.posture == Posture.SKILL_ANIMATING) {
			val danceId = creature.animation.replace("dance_", "").toInt()
			val performanceByDanceId = performances().getPerformanceByDanceId(danceId)
			if (performanceByDanceId != null) {
				scheduleExperienceTask(
					creature, performanceByDanceId.performanceName
				)
			}
		}
	}

	private fun handlePlayerLoggedOut(creature: CreatureObject) {
		if (creature.posture == Posture.SKILL_ANIMATING) {
			cancelExperienceTask(creature)
		}
	}

	@IntentHandler
	private fun handleFlourishIntent(fi: FlourishIntent) {
		val performer = fi.performer
		val performerObject = performer.creatureObject
		if (performerObject.performanceCounter != 0) return
		performerObject.performanceCounter = 1

		// Send the flourish animation to the owner of the creature and owners of creatures observing
		performerObject.sendObservers(Animation(performerObject.objectId, fi.flourishName))
		SystemMessageIntent(performer, "@performance:flourish_perform").broadcast()
	}

	@IntentHandler
	private fun handleWatchIntent(wi: WatchIntent) {
		val target = wi.target
		if (target is CreatureObject) {
			val actor = wi.actor
			if (target.isPlayer) {
				if (target.isPerforming) {
					val performance = performerMap[target.objectId] ?: return
					if (wi.isStartWatch) {
						if (performance.addSpectator(actor.creatureObject)) {
							startWatching(actor, target)
						}
					} else {
						if (performance.removeSpectator(actor.creatureObject)) {
							stopWatching(actor, true)
						}
					}
				} else {
					// While this is a valid target for watching, the target is currently not performing.
					SystemMessageIntent(
						actor, ProsePackage(StringId("performance", "dance_watch_not_dancing"), "TT", target.objectName)
					).broadcast()
				}
			} else {
				// You can't watch NPCs, regardless of whether they're dancing or not
				SystemMessageIntent(actor, "@performance:dance_watch_npc").broadcast()
			}
		}
	}

	@IntentHandler
	private fun handlePlayerTransformedIntent(pti: PlayerTransformedIntent) {
		val movedPlayer = pti.player
		val performanceListenTarget = movedPlayer.performanceListenTarget
		if (performanceListenTarget != 0L) {
			// They're watching a performer!
			val performance = performerMap[performanceListenTarget]
			if (performance == null) {
				Log.e("Couldn't perform range check on %s, because there was no performer with object ID %d", movedPlayer, performanceListenTarget)
				return
			}
			val performer = performance.performer
			val performerLocation = performer.worldLocation
			val movedPlayerLocation = pti.player.worldLocation // Ziggy: The newLocation in PlayerTransformedIntent isn't the world location, which is what we need here
			if (!movedPlayerLocation.isWithinDistance(performerLocation, WATCH_RADIUS.toDouble())) {
				// They moved out of the defined range! Make them stop watching
				if (performance.removeSpectator(movedPlayer)) {
					val player = movedPlayer.owner
					if (player != null) {
						stopWatching(player, true)
					}
				} else {
					Log.w(
						"%s ran out of range of %s, but couldn't stop watching because they weren't watching in the first place",
						movedPlayer,
						performer
					)
				}
			}
		}
	}

	private fun scheduleExperienceTask(performer: CreatureObject, performanceName: String) {
		Log.d("Scheduled %s to receive XP every %d seconds", performer, XP_CYCLE_RATE)
		synchronized(performerMap) {
			val performerId = performer.objectId
			val future = executorService.scheduleAtFixedRate(
				EntertainerExperience(performer),
				XP_CYCLE_RATE.toLong(),
				XP_CYCLE_RATE.toLong(),
				TimeUnit.SECONDS
			)

			// If they went LD but came back before disappearing
			val performance = performerMap[performerId]
			if (performance != null) {
				performance.future = future
			} else {
				performerMap.put(performer.objectId, Performance(performer, future, performanceName))
			}
		}
	}

	private fun cancelExperienceTask(performer: CreatureObject) {
		Log.d("%s no longer receives XP every %d seconds", performer, XP_CYCLE_RATE)
		synchronized(performerMap) {
			val performance = performerMap[performer.objectId]
			if (performance == null) {
				Log.e("Couldn't cancel experience task for %s because they weren't found in performerMap", performer)
				return
			}
			performance.future.cancel(false)
		}
	}

	private fun startDancing(player: Player, danceName: String) {
		val dancer = player.creatureObject
		val performanceByName = performances().getPerformanceByName(danceName)
		if (performanceByName == null) {
			StandardLog.onPlayerEvent(this, dancer, "tried to start unknown dance %s", danceName)
			return
		}
		val danceVisualId = performanceByName.danceVisualId
		dancer.animation = "dance_$danceVisualId"
		dancer.performanceId = 0 // 0 - anything else will make it look like we're playing music
		dancer.performanceCounter = 0
		dancer.isPerforming = true
		dancer.posture = Posture.SKILL_ANIMATING
		scheduleExperienceTask(dancer, danceName)
		SystemMessageIntent(player, "@performance:dance_start_self").broadcast()
	}

	private fun stopDancing(player: Player) {
		val dancer = player.creatureObject
		if (dancer.isPerforming) {
			dancer.isPerforming = false
			dancer.posture = Posture.UPRIGHT
			dancer.performanceCounter = 0
			dancer.animation = ""

			cancelExperienceTask(dancer)
			val performance = performerMap.remove(dancer.objectId)
			performance?.clearSpectators()
			SystemMessageIntent(player, "@performance:dance_stop_self").broadcast()
		} else {
			SystemMessageIntent(player, "@performance:dance_not_performing").broadcast()
		}
	}

	private fun changeDance(dancer: CreatureObject, newPerformanceName: String) {
		val performance = performerMap[dancer.objectId]
		if (performance != null) {
			performance.performanceName = newPerformanceName
			val performanceByName = performances().getPerformanceByName(newPerformanceName)
			if (performanceByName != null) {
				dancer.animation = "dance_" + performanceByName.performanceName
			}
		}
	}

	private fun startWatching(player: Player, creature: CreatureObject) {
		val actor = player.creatureObject
		actor.moodAnimation = "entertained"
		SystemMessageIntent(player, ProsePackage(StringId("performance", "dance_watch_self"), "TT", creature.objectName)).broadcast()
		actor.performanceListenTarget = creature.objectId
	}

	private fun stopWatching(player: Player, displaySystemMessage: Boolean) {
		val actor = player.creatureObject
		actor.moodAnimation = "neutral"
		if (displaySystemMessage) SystemMessageIntent(player, "@performance:dance_watch_stop_self").broadcast()
		actor.performanceListenTarget = 0
	}

	private inner class Performance(val performer: CreatureObject, var future: Future<*>, var performanceName: String) {
		private val audience = mutableSetOf<CreatureObject>()

		fun addSpectator(spectator: CreatureObject): Boolean {
			return audience.add(spectator)
		}

		fun removeSpectator(spectator: CreatureObject): Boolean {
			return audience.remove(spectator)
		}

		fun clearSpectators() {
			audience.mapNotNull { it.owner }.forEach(Consumer { player -> stopWatching(player, true) })
			audience.clear()
		}
	}

	private inner class EntertainerExperience(private val performer: CreatureObject) : Runnable {
		override fun run() {
			val performance = performerMap[performer.objectId]
			if (performance == null) {
				StandardLog.onPlayerError(this, performer, "is not in performerMap")
				return
			}
			val performanceName = performance.performanceName
			val performanceData = performances().getPerformanceByName(performanceName)
			if (performanceData == null) {
				StandardLog.onPlayerError(this, performer, "was performing unknown performance: '%s'", performanceName)
				return
			}
			val flourishXpMod = performanceData.flourishXpMod
			val performanceCounter = performer.performanceCounter
			val xpGained = performanceCounter * flourishXpMod
			if (xpGained > 0) {
				if (isDancing) {
					ExperienceIntent(performer, performer, "dance", xpGained, true).broadcast()
				}
				performer.performanceCounter = performanceCounter - 1
			}
		}

		private val isDancing: Boolean
			get() = performer.performanceId == 0
	}

	companion object {
		// TODO: when performing, make NPCs in a radius of x look towards the player (?) and clap. When they stop, turn back (?) and stop clapping
		private const val XP_CYCLE_RATE: Byte = 10
		private const val WATCH_RADIUS: Byte = 20
	}
}
