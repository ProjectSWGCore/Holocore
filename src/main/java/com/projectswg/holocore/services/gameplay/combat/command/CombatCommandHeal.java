/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.services.gameplay.combat.command;

import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.holocore.intents.gameplay.player.experience.ExperienceIntent;
import com.projectswg.holocore.resources.gameplay.combat.CombatStatus;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.projectswg.holocore.services.gameplay.combat.command.CombatCommandCommon.createCombatAction;

enum CombatCommandHeal implements CombatCommandHitType {
	INSTANCE;
	
	@Override
	public CombatStatus handle(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull Command command, @NotNull CombatCommand combatCommand, @NotNull String arguments) {
		int healAmount = combatCommand.getAddedDamage();
		int healingPotency = source.getSkillModValue("expertise_healing_all");
		int healedDamage = 0;

		if (healingPotency > 0) {
			healAmount *= healingPotency;
		}
		
		switch (combatCommand.getAttackType()) {
			case SINGLE_TARGET: {
				switch (command.getTargetType()) {
					case NONE: {
						healedDamage += doHeal(source, source, healAmount, combatCommand);
						break;
					}
					case REQUIRED: {
						if (target == null) {
							return CombatStatus.NO_TARGET;
						}
						
						// Same logic as OPTIONAL and ALL, so no break!
					}
					case OPTIONAL: {
						if (target != null) {
							if (!(target instanceof CreatureObject creatureTarget)) {
								return CombatStatus.INVALID_TARGET;
							}
							
							if (source.isAttackable(creatureTarget)) {
								healedDamage += doHeal(source, source, healAmount, combatCommand);
							} else {
								healedDamage += doHeal(source, creatureTarget, healAmount, combatCommand);
							}
						} else {
							healedDamage += doHeal(source, source, healAmount, combatCommand);
						}
						
						break;
					}
				}
				break;
			}
			
			case AREA: {
				// Targets are never supplied for AoE heals
				double range = combatCommand.getConeLength();
				Location sourceLocation = source.getWorldLocation();
				
				for (SWGObject nearbyObject : source.getAware()) {
					if (sourceLocation.isWithinDistance(nearbyObject.getLocation(), range)) {
						if (!(nearbyObject instanceof CreatureObject nearbyCreature)) {
							// We can't heal something that's not a creature
							continue;
						}
						
						if (source.isAttackable(nearbyCreature)) {
							// Don't heal (potential) enemies
							continue;
						}

						if (nearbyCreature.hasOptionFlags(OptionFlag.INVULNERABLE)) {
							// Don't heal creatures that can't take damage
							continue;
						}

						// Heal nearby friendly
						healedDamage += doHeal(source, nearbyCreature, healAmount, combatCommand);
					}
				}
				
				break;
			}
		}

		grantMedicalXp(source, healedDamage);
		return CombatStatus.SUCCESS;
	}

	private void grantMedicalXp(CreatureObject source, int healedDamage) {
		int medicalXp = Math.round((float) healedDamage * 0.25f);

		if (medicalXp > 0) {
			new ExperienceIntent(source, "medical", medicalXp).broadcast();
		}
	}

	private int doHeal(CreatureObject healer, CreatureObject healed, int healAmount, CombatCommand combatCommand) {
		if (combatCommand.getHealAttrib() != HealAttrib.HEALTH) {
			return 0;
		}

		if (healed.getHealth() == healed.getMaxHealth()) {
			return 0;
		}

		int originalHealth = healed.getHealth();
		healed.modifyHealth(healAmount);
		int difference = healed.getHealth() - originalHealth;
		
		WeaponObject weapon = healer.getEquippedWeapon();
		CombatAction combatAction = createCombatAction(healer, weapon, TrailLocation.RIGHT_HAND, combatCommand);
		combatAction.addDefender(new Defender(healed.getObjectId(), healed.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		healed.sendObservers(combatAction);

		String targetEffect = combatCommand.getTargetEffect();
		if (targetEffect.length() > 0) {
			String targetEffectHardpoint = combatCommand.getTargetEffectHardpoint();
			healed.sendObservers(new PlayClientEffectObjectMessage(targetEffect, targetEffectHardpoint, healed.getObjectId(), ""));
		}
		
		return difference;
	}
	
}
