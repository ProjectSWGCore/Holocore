package com.projectswg.holocore.services.combat;

import com.projectswg.holocore.intents.chat.ChatCommandIntent;
import com.projectswg.holocore.intents.combat.CreatureRevivedIntent;
import com.projectswg.holocore.intents.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.combat.ExitCombatIntent;
import com.projectswg.holocore.resources.commands.CombatCommand;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CombatRegenerationService extends Service {
	
	private final Set<CreatureObject> regeneratingHealthCreatures;    // Only allowed outside of combat
	private final Set<CreatureObject> regeneratingActionCreatures;    // Always allowed
	private final ScheduledThreadPool executor;
	
	public CombatRegenerationService() {
		this.regeneratingHealthCreatures = new CopyOnWriteArraySet<>();
		this.regeneratingActionCreatures = new CopyOnWriteArraySet<>();
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
		executor.awaitTermination(1000);
		return true;
	}
	
	@IntentHandler
	private void handleChatCommandIntent(ChatCommandIntent cci) {
		if (!cci.getCommand().isCombatCommand() || !(cci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand command = (CombatCommand) cci.getCommand();
		CreatureObject source = cci.getSource();
		
		double actionCost = command.getActionCost() * command.getAttackRolls();
		int currentAction = source.getAction();
		
		if (actionCost <= 0 || actionCost > currentAction) {
			return;
		}
		
		source.modifyAction((int) -actionCost);
		startActionRegeneration(source);
	}
	
	@IntentHandler
	private void handleEnterCombatIntent(EnterCombatIntent eci) {
		stopHealthRegeneration(eci.getSource());
	}
	
	@IntentHandler
	private void handleExitCombatIntent(ExitCombatIntent eci) {
		CreatureObject source = eci.getSource();
		switch (source.getPosture()) {
			case DEAD:
			case INCAPACITATED:
				stopHealthRegeneration(source);
				stopActionRegeneration(source);
				break;
			default:
				startHealthRegeneration(source);
				startActionRegeneration(source);
				break;
		}
	}
	
	@IntentHandler
	private void handleCreatureRevivedIntent(CreatureRevivedIntent cri) {
		startHealthRegeneration(cri.getCreature());
		startActionRegeneration(cri.getCreature());
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
