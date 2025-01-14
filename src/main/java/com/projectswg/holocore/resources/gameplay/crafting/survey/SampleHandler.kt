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
package com.projectswg.holocore.resources.gameplay.crafting.survey

import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import kotlinx.coroutines.CoroutineScope
import kotlin.concurrent.Volatile

class SampleHandler(private val creature: CreatureObject, private val surveyTool: TangibleObject, private val surveyScope: CoroutineScope) {
	@Volatile
	private var sampleSession: SampleLoopSession? = null

	@Synchronized
	fun startSession() {
	}

	@Synchronized
	fun stopSession() {
		val session = this.sampleSession
		this.sampleSession = null
		session?.stopSession()
	}

	@Synchronized
	fun startSampleLoop(resource: GalacticResource) {
		val sampleLocation = creature.worldLocation

		val prevSession = this.sampleSession
		if (prevSession != null && prevSession.isMatching(creature, surveyTool, resource, sampleLocation)) {
			if (!prevSession.isSampling) prevSession.startSession(surveyScope)
		} else {
			val nextSession = SampleLoopSession(creature, surveyTool, resource, sampleLocation)
			prevSession?.stopSession()
			nextSession.startSession(surveyScope)
			this.sampleSession = nextSession
		}
	}

	@Synchronized
	fun onPlayerMoved() {
		val session = this.sampleSession
		session?.onPlayerMoved()
	}

	@Synchronized
	fun stopSampleLoop() {
		stopSession()
	}

	val isSampling: Boolean
		get() {
			val session = this.sampleSession
			return session != null && session.isSampling
		}
}
