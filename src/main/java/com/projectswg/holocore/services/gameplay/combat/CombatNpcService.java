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
package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.CorpseLootedIntent;
import com.projectswg.holocore.intents.gameplay.combat.LootLotteryStartedIntent;
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class CombatNpcService extends Service {
	
	private final Map<Long, ScheduledFuture<?>> deleteCorpseTasks;
	private final ScheduledThreadPool executor;
	
	public CombatNpcService() {
		this.deleteCorpseTasks = new HashMap<>();
		this.executor = new ScheduledThreadPool(1, "combat-npc-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent i) {
		CreatureObject corpse = i.getCorpse();
		if (corpse.isPlayer())
			return;
		
		if (i.getKiller().isPlayer())
			deleteCorpseTasks.put(corpse.getObjectId(), executor.execute(TimeUnit.SECONDS.toMillis(120), () -> deleteCorpse(corpse)));
		else
			executor.execute(60000, () -> deleteCorpse(corpse));
	}
	
	@IntentHandler
	private void handleLootLotteryStartedIntent(LootLotteryStartedIntent llsi) {
		CreatureObject corpse = llsi.getCorpse();
		assert !corpse.isPlayer() : "Cannot (shouldn't) loot a player";
		
		ScheduledFuture<?> task = deleteCorpseTasks.get(corpse.getObjectId());
		assert task != null;
		
		if (task.getDelay(TimeUnit.SECONDS) < 35 && task.cancel(false))
			executor.execute(35000, () -> deleteCorpse(corpse));
	}
	
	@IntentHandler
	private void handleCorpseLootedIntent(CorpseLootedIntent cli) {
		CreatureObject corpse = cli.getCorpse();
		assert !corpse.isPlayer() : "Cannot (shouldn't) loot a player";
		
		ScheduledFuture<?> task = deleteCorpseTasks.get(corpse.getObjectId());
		
		if (task == null) {
			Log.w("There should already be a deleteCorpse task for corpse %s!", corpse.toString());
			executor.execute(30000, () -> deleteCorpse(corpse));
			return;
		}
		
		// if existing deleteCorpse task has more than 5 seconds remaining, cancel it
		// if the cancel operation succeeds, schedule another deleteCorpse task for 5 seconds
		if (task.getDelay(TimeUnit.SECONDS) > 15 && task.cancel(false))
			executor.execute(30000, () -> deleteCorpse(corpse));
	}
	
	private void deleteCorpse(CreatureObject creatureCorpse) {
		new DestroyObjectIntent(creatureCorpse).broadcast();
		deleteCorpseTasks.remove(creatureCorpse.getObjectId());
		Log.d("Corpse of NPC %s was deleted from the world", creatureCorpse);
	}
	
}