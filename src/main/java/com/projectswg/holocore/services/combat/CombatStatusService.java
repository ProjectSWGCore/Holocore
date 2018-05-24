package com.projectswg.holocore.services.combat;

import com.projectswg.holocore.intents.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.combat.ExitCombatIntent;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.services.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class CombatStatusService extends Service {
	
	private final Set<CreatureObject> inCombat;
	private final ScheduledThreadPool executor;
	
	public CombatStatusService() {
		this.inCombat = ConcurrentHashMap.newKeySet();
		this.executor = new ScheduledThreadPool(1, 3, "combat-status-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		executor.executeWithFixedRate(1000, 1000, this::periodicCombatStatusChecks);
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	private void periodicCombatStatusChecks() {
		for (CreatureObject creature : inCombat) {
			if (creature.getTimeSinceLastCombat() >= 10E3)
				ExitCombatIntent.broadcast(creature);
		}
	}
	
	@IntentHandler
	private void handleEnterCombatIntent(EnterCombatIntent eci) {
		CreatureObject source = eci.getSource();
		CreatureObject target = eci.getTarget();
		if (source.isInCombat())
			return;
		
		source.setInCombat(true);
		target.addDefender(source);
		source.addDefender(target);
		inCombat.add(source);
		inCombat.add(target);
	}
	
	@IntentHandler
	private void handleExitCombatIntent(ExitCombatIntent eci) {
		CreatureObject source = eci.getSource();
		List<CreatureObject> defenders = source.getDefenders().stream().map(ObjectLookup::getObjectById).map(CreatureObject.class::cast).collect(Collectors.toList());
		source.clearDefenders();
		for (CreatureObject defender : defenders) {
			defender.removeDefender(source);
			if (!defender.hasDefenders())
				ExitCombatIntent.broadcast(defender);
		}
		if (source.hasDefenders())
			return;
		
		source.setInCombat(false);
		inCombat.remove(source);
	}
}
