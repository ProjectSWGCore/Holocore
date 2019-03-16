package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.common.data.RGB;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.holocore.intents.gameplay.combat.CreatureRevivedIntent;
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.ExitCombatIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.SpecialLineLoader;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ThreadLocalRandom;

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
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		CreatureObject creature = pei.getPlayer().getCreatureObject();
		switch (pei.getEvent()) {
			case PE_ZONE_IN_SERVER:
				if (!creature.isInCombat()) {
					startHealthRegeneration(creature);
					startActionRegeneration(creature);
				}
				break;
			default:
				break;
		}
	}
	
	@IntentHandler
	private void handleExecuteCommandIntent(ExecuteCommandIntent eci) {
		if (!eci.getCommand().isCombatCommand() || !(eci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand command = (CombatCommand) eci.getCommand();
		CreatureObject source = eci.getSource();
		String lineName = command.getSpecialLine();
		SpecialLineLoader lineLoader = DataLoader.specialLines();
		SpecialLineLoader.SpecialLineInfo specialLine = lineLoader.getSpecialLine(lineName);
		double actionCost = command.getActionCost() * command.getAttackRolls();
		int currentAction = source.getAction();
		
		// TODO future: reduce actionCost with general ACR, weapon ACR and ability ACR
		
		if (actionCost <= 0 || actionCost > currentAction) {
			return;
		}
		
		if (specialLine != null && !lineName.isEmpty()) {
			// Roll for a freeshot
			rollFreeshot(source, actionCost, specialLine);
		} else {
			deductActionPoints(source, actionCost);
		}
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
	
	private void rollFreeshot(CreatureObject source, double actionCost, SpecialLineLoader.SpecialLineInfo specialLine) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		String freeshotModName = specialLine.getFreeshotModName();
		int skillModValue = source.getSkillModValue(freeshotModName);
		int generated = random.nextInt(0, 100);
		
		if (skillModValue > generated) {
			// They rolled a freeshot. This requires a skill mod value of at least 1.
			ShowFlyText showFlyText = new ShowFlyText(source.getObjectId(), new StringId("spam", "freeshot"), ShowFlyText.Scale.MEDIUM, new RGB(255, 255, 255), ShowFlyText.Flag.IS_FREESHOT);
			
			source.sendSelf(showFlyText);
		} else {
			// Normal behavior
			deductActionPoints(source, actionCost);
		}
	}
	
	private void deductActionPoints(CreatureObject source, double actionCost) {
		source.modifyAction((int) -actionCost);
		startActionRegeneration(source);
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
		
		int modification = creature.isPlayer() ? creature.getSkillModValue("action_regen") : creature.getMaxAction() / 10;
		
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
		
		int modification = creature.isPlayer() ? creature.getSkillModValue("health_regen") : creature.getMaxHealth() / 10;
		
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
