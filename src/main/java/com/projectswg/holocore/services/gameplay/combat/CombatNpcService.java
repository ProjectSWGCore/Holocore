package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.holocore.intents.gameplay.combat.loot.CorpseLootedIntent;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
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
		
		deleteCorpseTasks.put(corpse.getObjectId(), executor.execute(TimeUnit.SECONDS.toMillis(120), () -> deleteCorpse(corpse)));
	}
	
	@IntentHandler
	private void handleCorpseLootedIntent(CorpseLootedIntent i) {
		CreatureObject corpse = i.getCorpse();
		assert !corpse.isPlayer() : "Cannot (shouldn't) loot a player";
		
		ScheduledFuture<?> task = deleteCorpseTasks.get(corpse.getObjectId());
		
		if (task == null) {
			Log.w("There should already be a deleteCorpse task for corpse %s!", corpse.toString());
			executor.execute(5000, () -> deleteCorpse(corpse));
			return;
		}
		
		// if existing deleteCorpse task has more than 5 seconds remaining, cancel it
		// if the cancel operation succeeds, schedule another deleteCorpse task for 5 seconds
		if (task.getDelay(TimeUnit.SECONDS) > 5 && task.cancel(false))
			executor.execute(5000, () -> deleteCorpse(corpse));
	}
	
	private void deleteCorpse(CreatureObject creatureCorpse) {
		DestroyObjectIntent.broadcast(creatureCorpse);
		deleteCorpseTasks.remove(creatureCorpse.getObjectId());
		Log.d("Corpse of NPC %s was deleted from the world", creatureCorpse);
	}
	
}
