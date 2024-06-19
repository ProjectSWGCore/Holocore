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

import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.global.commands.Locomotion;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureState;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
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
		TangibleObject source = eci.getSource();
		if (source instanceof AIObject)
			npcRegen.add((CreatureObject) source);
		TangibleObject target = eci.getTarget();
		if (target instanceof AIObject)
			npcRegen.add((CreatureObject) target);
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		if (doi.getObj() instanceof CreatureObject creature)
			npcRegen.remove(creature);
	}
	
	private void periodicRegeneration() {
		PlayerLookup.getLoggedInCharacters().forEach(this::regenerate);
		npcRegen.forEach(this::regenerate);
		npcRegen.removeIf(npc -> !npc.isInCombat() && npc.getHealth() == npc.getMaxHealth() && npc.getAction() == npc.getMaxAction());
	}
	
	private void regenerate(CreatureObject creature) {
		regenerationHealthTick(creature);
		regenerationActionTick(creature);
		regenerationMindTick(creature);
	}
	
	private void regenerationActionTick(CreatureObject creature) {
		if (creature.getAction() >= creature.getMaxAction())
			return;

		if (creature.isStatesBitmask(CreatureState.STUNNED)) {
			return;
		}
		
		int modification = creature.getMaxAction() / 40;
		
		if (!creature.isInCombat())
			modification *= 2;
		
		if (Locomotion.SITTING.isActive(creature)) {
			modification *= 2;
		}
		
		creature.modifyAction(modification);
	}
	
	private void regenerationMindTick(CreatureObject creature) {
		if (creature.getMind() >= creature.getMaxMind())
			return;
		
		int modification = creature.getMaxMind() / 40;
		
		if (!creature.isInCombat())
			modification *= 2;
		
		if (Locomotion.SITTING.isActive(creature)) {
			modification *= 2;
		}
		
		creature.modifyMind(modification);
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
		
		int modification = creature.getMaxHealth() / 40;
		
		if (Locomotion.SITTING.isActive(creature)) {
			modification *= 2;
		}
		
		creature.modifyHealth(modification);
	}
	
}
