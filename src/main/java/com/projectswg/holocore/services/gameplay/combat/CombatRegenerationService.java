package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.services.support.global.zone.CharacterLookupService.PlayerLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class CombatRegenerationService extends Service {
	
	private final ScheduledThreadPool executor;
	private final Set<CreatureObject> npcRegen;
	
	public CombatRegenerationService() {
		this.executor = new ScheduledThreadPool(1, 3, "combat-regeneration-service");
		this.npcRegen = new CopyOnWriteArraySet<>();
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
	
	@IntentHandler
	private void handleEnterCombatIntent(EnterCombatIntent eci) {
		if (!eci.getSource().isPlayer())
			npcRegen.add(eci.getSource());
		if (!eci.getTarget().isPlayer())
			npcRegen.add(eci.getTarget());
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		if (doi.getObject() instanceof CreatureObject)
			npcRegen.remove(doi.getObject());
	}
	
	private void periodicRegeneration() {
		PlayerLookup.getLoggedInCharacters().forEach(this::regenerate);
		npcRegen.forEach(this::regenerate);
		
		for (CreatureObject npc : npcRegen) {
			if (!npc.isInCombat() && npc.getHealth() == npc.getMaxHealth() && npc.getAction() == npc.getMaxAction())
				npcRegen.remove(npc);
		}
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
		switch (creature.getPosture()) {
			case DEAD:
			case INCAPACITATED:
				return;
			default:
				break;
		}
		
		int modification = creature.isPlayer() ? creature.getSkillModValue("health_regen") : creature.getMaxHealth() / 10;
		
		if (!creature.isInCombat())
			modification *= 4;
		
		creature.modifyHealth(modification);
	}
	
}
