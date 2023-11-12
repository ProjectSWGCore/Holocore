/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.world.weather

import com.projectswg.common.data.WeatherType
import com.projectswg.common.data.location.Terrain
import com.projectswg.common.network.packets.swg.zone.ServerTimeMessage
import com.projectswg.common.network.packets.swg.zone.ServerWeatherMessage
import com.projectswg.common.utilities.ThreadUtilities
import com.projectswg.holocore.ProjectSWG
import com.projectswg.holocore.intents.support.global.zone.NotifyPlayersPacketIntent
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent
import com.projectswg.holocore.resources.support.global.player.PlayerEvent
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class EnvironmentService : Service() {
	private val cycleDuration = Duration.of(10, ChronoUnit.MINUTES)
	private val terrains = Terrain.values()
	private val weatherForTerrain = mutableMapOf<Terrain, WeatherType>()
	private val executor = Executors.newScheduledThreadPool(2, ThreadUtilities.newThreadFactory("environment-service"))

	override fun initialize(): Boolean {
		for (terrain in terrains) {
			weatherForTerrain[terrain] = randomWeather()
			executor.scheduleAtFixedRate({ maybeUpdateWeather(terrain) }, 0, cycleDuration.toSeconds(), TimeUnit.SECONDS)
		}
		return super.initialize()
	}

	override fun start(): Boolean {
		executor.scheduleAtFixedRate({ updateTime() }, 30, 30, TimeUnit.SECONDS)
		return super.start()
	}

	override fun terminate(): Boolean {
		var success = true
		try {
			executor.shutdownNow()
			success = executor.awaitTermination(3000, TimeUnit.MILLISECONDS)
		} catch (ignored: InterruptedException) {
		}
		return super.terminate() && success
	}

	@IntentHandler
	private fun handlePlayerEventIntent(pei: PlayerEventIntent) {
		if (pei.event == PlayerEvent.PE_ZONE_IN_CLIENT) handleZoneIn(pei)
	}

	private fun handleZoneIn(pei: PlayerEventIntent) {
		val p = pei.player
		val t = p.creatureObject.terrain
		p.sendPacket(constructWeatherPacket(t))
	}

	private fun updateTime() {
		NotifyPlayersPacketIntent(ServerTimeMessage(ProjectSWG.galacticTime)).broadcast()
	}

	private fun maybeUpdateWeather(terrain: Terrain) {
		if (Random.nextBoolean()) { // 50/50 chance of weather change
			updateWeather(terrain, randomWeather())
		}
	}

	private fun updateWeather(terrain: Terrain, type: WeatherType) {
		val weatherTypeAlreadyActive = weatherForTerrain.containsKey(terrain) && type == weatherForTerrain[terrain]
		if (weatherTypeAlreadyActive) return
		weatherForTerrain[terrain] = type
		val serverWeatherMessage = constructWeatherPacket(terrain)
		NotifyPlayersPacketIntent(serverWeatherMessage, terrain).broadcast()
	}

	private fun constructWeatherPacket(terrain: Terrain): ServerWeatherMessage {
		val swm = ServerWeatherMessage()
		val type = weatherForTerrain[terrain]
		swm.type = type
		swm.cloudVectorX = Random.nextFloat() + 1
		swm.cloudVectorZ = Random.nextFloat() + 1
		swm.cloudVectorY = 0f // Always 0, clouds don't move up/down
		return swm
	}

	private fun randomWeather(): WeatherType {
		var weather = WeatherType.CLEAR
		val roll = Random.nextFloat()

		for (candidate in WeatherType.values()) {
			if (roll <= candidate.chance) {
				weather = candidate
			}
		}

		return weather
	}
}
