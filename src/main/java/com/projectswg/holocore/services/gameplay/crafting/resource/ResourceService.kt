/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
 * *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 * *
 * This file is part of Holocore.                                                  *
 * *
 * --------------------------------------------------------------------------------*
 * *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 * *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 * *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http:></http:>//www.gnu.org/licenses/>.               *
 */
package com.projectswg.holocore.services.gameplay.crafting.resource

import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawner
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.addRawResource
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer.clear
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData.rawResources
import me.joshlarson.jlcommon.concurrency.BasicScheduledThread
import me.joshlarson.jlcommon.control.Service
import java.util.concurrent.TimeUnit

/**
 * In charge of spawning, despawning, and overall management of resources
 */
class ResourceService : Service() {
	private val spawner = GalacticResourceSpawner()
	private val spawnerUpdater = BasicScheduledThread("resource-spawn-updater") { spawner.updateAllResources() }

	override fun initialize(): Boolean {
		for (rawResource in rawResources.getResources()) {
			addRawResource(rawResource)
		}
		spawner.initialize()
		spawnerUpdater.startWithFixedRate(0, TimeUnit.HOURS.toMillis(3))
		return super.initialize()
	}

	override fun terminate(): Boolean {
		clear()
		spawnerUpdater.stop()
		spawner.terminate()
		return super.terminate()
	}
}
