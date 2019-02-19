package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.Service;

public class CombatRegenerationService extends Service {
	
	private final ScheduledThreadPool executor;
	
	public CombatRegenerationService() {
		this.executor = new ScheduledThreadPool(1, 3, "combat-regeneration-service");
	}
	
	@Override
	public boolean start() {
		executor.start();
		executor.executeWithFixedRate(1000, 1000, this::periodicRegeneration);
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	private void periodicRegeneration() {
		PlayerLookup.getLoggedInCharacters().forEach(this::regenerate);
	}
	
	private void regenerate(CreatureObject creature) {
		regenerationHealthTick(creature);
		regenerationActionTick(creature);
	}
	
	private void regenerationActionTick(CreatureObject creature) {
		if (creature.getAction() >= creature.getMaxAction())
			return;
		
		int modification = creature.isPlayer() ? creature.getSkillModValue("action_regen") : creature.getMaxAction() / 10;
		
		if (!creature.isInCombat())
			modification *= 4;
		
		creature.modifyAction(modification);
	}
	
	private void regenerationHealthTick(CreatureObject creature) {
		if (creature.getHealth() >= creature.getMaxHealth() || creature.isInCombat())
			return;
		
		int modification = creature.isPlayer() ? creature.getSkillModValue("health_regen") : creature.getMaxHealth() / 10;
		
		if (!creature.isInCombat())
			modification *= 4;
		
		creature.modifyHealth(modification);
	}
	
}
