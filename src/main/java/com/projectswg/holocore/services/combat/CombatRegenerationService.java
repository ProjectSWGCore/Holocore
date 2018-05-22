package com.projectswg.holocore.services.combat;

import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CombatRegenerationService extends Service {
	
	private final Set<CreatureObject> regeneratingHealthCreatures;    // Only allowed outside of combat
	private final Set<CreatureObject> regeneratingActionCreatures;    // Always allowed
	
	public CombatRegenerationService() {
		this.regeneratingHealthCreatures = new CopyOnWriteArraySet<>();
		this.regeneratingActionCreatures = new CopyOnWriteArraySet<>();
		
		// TODO: executor.executeWithFixedRate(1000, 1000, this::periodicRegeneration);
	}
	
	private void periodicRegeneration() {
		regeneratingHealthCreatures.forEach(this::regenerationHealthTick);
		regeneratingActionCreatures.forEach(this::regenerationActionTick);
	}
	
	private void regenerationActionTick(CreatureObject creature) {
		if (creature.getAction() >= creature.getMaxAction()) {
			if (!creature.isInCombat())
				stopActionRegeneration(creature);
			return;
		}
		
		int modification = creature.getSkillModValue("action_regen");
		
		if (!creature.isInCombat())
			modification *= 4;
		
		creature.modifyAction(modification);
	}
	
	private void regenerationHealthTick(CreatureObject creature) {
		if (creature.getHealth() >= creature.getMaxHealth()) {
			if (!creature.isInCombat())
				stopHealthRegeneration(creature);
			return;
		}
		
		int modification = creature.getSkillModValue("health_regen");
		
		if (!creature.isInCombat())
			modification *= 4;
		
		creature.modifyHealth(modification);
	}
	
	private void startActionRegeneration(CreatureObject creature) {
		regeneratingActionCreatures.add(creature);
	}
	
	private void startHealthRegeneration(CreatureObject creature) {
		regeneratingHealthCreatures.add(creature);
	}
	
	private void stopActionRegeneration(CreatureObject creature) {
		regeneratingActionCreatures.remove(creature);
	}
	
	private void stopHealthRegeneration(CreatureObject creature) {
		regeneratingHealthCreatures.remove(creature);
	}
	
}
