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
package com.projectswg.holocore.services.support.npc.ai;

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class AIService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Collection<AIObject> aiObjects;
	private final AtomicBoolean started;
	
	public AIService() {
		this.executor = new ScheduledThreadPool(16, "ai-service-%d");
		this.aiObjects = ConcurrentHashMap.newKeySet();
		this.started = new AtomicBoolean(false);
	}
	
	@Override
	public boolean start() {
		executor.start();
		started.set(true);
		for (AIObject obj : aiObjects) {
			obj.start(executor);
		}
		return true;
	}
	
	@Override
	public boolean stop() {
		started.set(false);
		aiObjects.clear();
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		if (!(oci.getObject() instanceof AIObject))
			return;
		AIObject obj = (AIObject) oci.getObject();
		if (aiObjects.add(obj) && started.get())
			obj.start(executor);
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		if (!(doi.getObject() instanceof AIObject))
			return;
		AIObject obj = (AIObject) doi.getObject();
		if (aiObjects.remove(obj) && started.get())
			obj.stop();
	}
	
	@IntentHandler
	private void handleEnterCombatIntent(EnterCombatIntent eci) {
		if (!(eci.getSource() instanceof AIObject obj))
			return;
		if (eci.getTarget() instanceof CreatureObject creatureTarget) {
			StartNpcCombatIntent.broadcast(obj, List.of(creatureTarget));
		}
	}
	
}
