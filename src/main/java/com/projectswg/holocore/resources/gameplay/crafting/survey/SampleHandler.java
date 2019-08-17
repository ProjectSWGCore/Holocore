/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.gameplay.crafting.survey;

import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import org.jetbrains.annotations.Nullable;

public class SampleHandler {
	
	private final CreatureObject creature;
	private final TangibleObject surveyTool;
	private final ScheduledThreadPool executor;
	
	private volatile @Nullable SampleLoopSession sampleSession;
	
	public SampleHandler(CreatureObject creature, TangibleObject surveyTool, ScheduledThreadPool executor) {
		this.creature = creature;
		this.surveyTool = surveyTool;
		this.executor = executor;
		
		this.sampleSession = null;
	}
	
	public synchronized void startSession() {
		
	}
	
	public synchronized void stopSession() {
		SampleLoopSession session = this.sampleSession;
		this.sampleSession = null;
		if (session != null)
			session.stopSession();
	}
	
	public synchronized void startSampleLoop(GalacticResource resource) {
		Location sampleLocation = creature.getWorldLocation();
		
		SampleLoopSession prevSession = this.sampleSession;
		if (prevSession != null && prevSession.isMatching(creature, surveyTool, resource, sampleLocation)) {
			if (!prevSession.isSampling())
				prevSession.startSession(executor);
		} else {
			SampleLoopSession nextSession = new SampleLoopSession(creature, surveyTool, resource, sampleLocation);
			if (prevSession != null)
				prevSession.stopSession();
			nextSession.startSession(executor);
			this.sampleSession = nextSession;
		}
	}
	
	public synchronized void onPlayerMoved() {
		SampleLoopSession session = this.sampleSession;
		if (session != null)
			session.onPlayerMoved();
	}
	
	public synchronized void stopSampleLoop() {
		stopSession();
	}
	
	public boolean isSampling() {
		SampleLoopSession session = this.sampleSession;
		return session != null && session.isSampling();
	}
	
}
