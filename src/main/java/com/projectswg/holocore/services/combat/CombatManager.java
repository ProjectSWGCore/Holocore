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
package com.projectswg.holocore.services.combat;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.RGB;
import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.BuffIntent;
import com.projectswg.holocore.intents.chat.ChatCommandIntent;
import com.projectswg.holocore.intents.combat.DeathblowIntent;
import com.projectswg.holocore.intents.combat.IncapacitateCreatureIntent;
import com.projectswg.holocore.intents.combat.KillCreatureIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.commands.CombatCommand;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;
import com.projectswg.holocore.services.combat.loot.LootManager;
import com.projectswg.holocore.services.objects.ObjectCreator;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;
import me.joshlarson.jlcommon.log.Log;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@ManagerStructure(children = {
		CombatRegenerationService.class,
		CombatDeathblowService.class,
		CombatStatusService.class,
		CombatCommandService.class,
		CombatExperienceService.class,
		CombatCloningService.class,
		CombatNpcService.class,
		CombatDuelService.class,
		LootManager.class
})
public class CombatManager extends Manager {
	
	private static final RGB COLOR_WHITE = new RGB(255, 255, 255);
	private static final RGB COLOR_CYAN = new RGB(0, 255, 255);
	
	private final Map<Long, CombatCreature> inCombat;
	private final ScheduledThreadPool executor;
	private final Random random;
	
	public CombatManager() {
		this.inCombat = new ConcurrentHashMap<>();
		this.executor = new ScheduledThreadPool(1, "combat-service");
		this.random = new Random();
		
		registerForIntent(DeathblowIntent.class, this::handleDeathblowIntent);
		registerForIntent(ChatCommandIntent.class, this::handleChatCommandIntent);
		registerForIntent(IncapacitateCreatureIntent.class, ici -> incapacitatePlayer(ici.getIncapper(), ici.getIncappee()));
		registerForIntent(KillCreatureIntent.class, kci -> killCreature(kci.getKiller(), kci.getCorpse()));
	}
	
	@Override
	public boolean start() {
		executor.start();
		executor.executeWithFixedRate(0, 5000, this::periodicChecks);
		executor.executeWithFixedRate(1000, 1000, this::periodicRegeneration);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return super.terminate();
	}
	
	private void periodicChecks() {
		inCombat.values().stream().filter(c -> c.getTimeSinceCombat() >= 10E3).forEach(c -> exitCombat(c.getCreature()));
	}
	
	private void updateCombatList(CreatureObject creature) {
		CombatCreature combat = inCombat.computeIfAbsent(creature.getObjectId(), s -> new CombatCreature(creature));
		combat.updateLastCombat();
	}
	
	private void doCombatSingle(CreatureObject source, SWGObject target, AttackInfo info, WeaponObject weapon, CombatCommand command) {
		// TODO single target only defence rolls against target
		// TODO single target only offence rolls for source
		
		// TODO below logic should be in CommandService when target checks are implemented in there
		Set<CreatureObject> targets = new HashSet<>();
		
		if (target instanceof CreatureObject)
			targets.add((CreatureObject) target);
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private void doCombatArea(CreatureObject source, SWGObject origin, AttackInfo info, WeaponObject weapon, CombatCommand command, boolean includeOrigin) {
		float aoeRange = command.getConeLength();
		SWGObject originParent = origin.getParent();
		Collection<SWGObject> objectsToCheck = originParent == null ? origin.getObjectsAware() : originParent.getContainedObjects();
		
		// TODO block
		// TODO evasion if no block
		
		// TODO line of sight checks between the explosive and each target
		Set<CreatureObject> targets = objectsToCheck.stream().filter(CreatureObject.class::isInstance).map(CreatureObject.class::cast)
				.filter(source::isAttackable).filter(target -> canPerform(source, target, command) == CombatStatus.SUCCESS)
				.filter(creature -> origin.getLocation().distanceTo(creature.getLocation()) <= aoeRange).collect(Collectors.toSet());
		
		// This way, mines or grenades won't try to harm themselves
		if (includeOrigin && origin instanceof CreatureObject)
			targets.add((CreatureObject) origin);
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private void doCombat(CreatureObject source, Set<CreatureObject> targets, WeaponObject weapon, AttackInfo info, CombatCommand command) {
		updateCombatList(source);
		
		CombatAction action = new CombatAction(source.getObjectId());
		String anim = command.getRandomAnimation(weapon.getType());
		action.setActionCrc(CRC.getCrc(anim));
		action.setAttackerId(source.getObjectId());
		action.setPosture(source.getPosture());
		action.setWeaponId(weapon.getObjectId());
		action.setClientEffectId((byte) 0);
		action.setCommandCrc(command.getCrc());
		action.setTrail(TrailLocation.WEAPON);
		action.setUseLocation(false);
		
		for (CreatureObject target : targets) {
			updateCombatList(target);
			
			if (!source.isInCombat())
				enterCombat(source);
			if (!target.isInCombat())
				enterCombat(target);
			target.addDefender(source);
			source.addDefender(target);
			
			CombatSpam combatSpam = new CombatSpam(source.getObjectId());
			combatSpam.setAttacker(source.getObjectId());
			combatSpam.setAttackerPosition(source.getLocation().getPosition());
			combatSpam.setWeapon(weapon.getObjectId());
			combatSpam.setWeaponName(weapon.getStringId());
			combatSpam.setDefender(target.getObjectId());
			combatSpam.setDefenderPosition(target.getLocation().getPosition());
			combatSpam.setInfo(info);
			combatSpam.setAttackName(new StringId("cmd_n", command.getName()));
			combatSpam.setSpamType(CombatSpamFilterType.ALL);
			
			if (!info.isSuccess()) {    // Single target negate, like dodge or parry!
				target.sendObservers(combatSpam);
				return;
			}
			
			addBuff(source, target, command.getBuffNameTarget());    // Add target buff
			
			int rawDamage = calculateWeaponDamage(source, weapon, command) + command.getAddedDamage();
			
			info.setRawDamage(rawDamage);
			info.setFinalDamage(rawDamage);    // Final damage will be modified by armour and defensive rolls later
			info.setDamageType(weapon.getDamageType());
			
			// TODO block roll for defenders
			// TODO Critical hit roll for attacker
			// TODO armour
			
			target.sendObservers(combatSpam);
			
			int finalDamage = info.getFinalDamage();
			
			action.addDefender(new Defender(target.getObjectId(), target
					.getPosture(), true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) finalDamage));
			
			target.handleDamage(source, finalDamage);
			
			if (target.getHealth() <= finalDamage)
				doCreatureDeath(target, source);
			else
				target.modifyHealth(-finalDamage);
		}
		
		source.sendObservers(action);
	}
	
	private void enterCombat(CreatureObject creature) {
		if (creature.isInCombat())
			return;
		creature.setInCombat(true);
		
		// If this creature is currently regenerating health, they should stop doing so now
		stopHealthRegeneration(creature);
	}
	
	private void exitCombat(CreatureObject creature) {
		if (inCombat.remove(creature.getObjectId()) == null || !creature.isInCombat())
			return;
		creature.setInCombat(false);
		creature.clearDefenders();
		
		// Once out of combat, we can regenerate health - unless we're dead or incapacitated!
		switch (creature.getPosture()) {
			case DEAD:
			case INCAPACITATED:
				stopHealthRegeneration(creature);
				stopActionRegeneration(creature);
				break;
			default:
				startHealthRegeneration(creature);
				startActionRegeneration(creature);
				break;
		}
	}
	
	private boolean handleStatus(CreatureObject source, CombatStatus status) {
		switch (status) {
			case SUCCESS:
				return true;
			case NO_TARGET:
				showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, COLOR_WHITE, ShowFlyText.Flag.PRIVATE);
				return false;
			case TOO_FAR:
				showFlyText(source, "@combat_effects:range_too_far", Scale.MEDIUM, COLOR_CYAN, ShowFlyText.Flag.PRIVATE);
				return false;
			case INVALID_TARGET:
				showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, COLOR_CYAN, ShowFlyText.Flag.PRIVATE);
				return false;
			default:
				showFlyText(source, "@combat_effects:action_failed", Scale.MEDIUM, COLOR_WHITE, ShowFlyText.Flag.PRIVATE);
				return false;
		}
	}
	
	private CombatStatus canPerform(CreatureObject source, SWGObject target, CombatCommand c) {
		if (source.getEquippedWeapon() == null)
			return CombatStatus.NO_WEAPON;
		
		if (target == null || source.equals(target))
			return CombatStatus.SUCCESS;
		
		if (!(target instanceof TangibleObject))
			return CombatStatus.INVALID_TARGET;
		
		if (!source.isEnemyOf((TangibleObject) target)) {
			return CombatStatus.INVALID_TARGET;
		}
		
		if (target instanceof CreatureObject) {
			switch (((CreatureObject) target).getPosture()) {
				case DEAD:
				case INCAPACITATED:
					return CombatStatus.INVALID_TARGET;
				default:
					break;
			}
		}
		
		switch (c.getAttackType()) {
			case AREA:
			case TARGET_AREA:
				return canPerformArea(source, c);
			case SINGLE_TARGET:
				return canPerformSingle(source, target, c);
			default:
				return CombatStatus.UNKNOWN;
		}
	}
	
	private CombatStatus canPerformSingle(CreatureObject source, SWGObject target, CombatCommand c) {
		if (target == null || !(target instanceof TangibleObject))
			return CombatStatus.NO_TARGET;
		
		WeaponObject weapon = source.getEquippedWeapon();
		double dist = Math.floor(source.getWorldLocation().distanceTo(target.getWorldLocation()));
		double commandRange = c.getMaxRange();
		double range = commandRange > 0 ? commandRange : weapon.getMaxRange();
		
		if (dist > range)
			return CombatStatus.TOO_FAR;
		
		return CombatStatus.SUCCESS;
	}
	
	private CombatStatus canPerformArea(CreatureObject source, CombatCommand c) {
		return CombatStatus.SUCCESS;
	}
	
	private void addActionCost(CreatureObject source, CombatCommand command) {
		double actionCost = command.getActionCost() * command.getAttackRolls();
		int currentAction = source.getAction();
		
		if (actionCost <= 0 || actionCost > currentAction) {
			return;
		}
		
		source.modifyAction((int) -actionCost);
		startActionRegeneration(source);
	}
	
	private int calculateWeaponDamage(CreatureObject source, WeaponObject weapon, CombatCommand command) {
		int minDamage = weapon.getMinDamage();
		int weaponDamage = random.nextInt((weapon.getMaxDamage() - minDamage) + 1) + minDamage;
		
		return (int) (weaponDamage * command.getPercentAddFromWeapon());
	}
	
	private void addBuff(CreatureObject caster, CreatureObject receiver, String buffName) {
		if (buffName.isEmpty()) {
			return;
		}
		
		new BuffIntent(buffName, caster, receiver, false).broadcast();
	}
	
	private void showFlyText(TangibleObject obj, String text, Scale scale, RGB c, ShowFlyText.Flag... flags) {
		obj.sendSelf(new ShowFlyText(obj.getObjectId(), text, scale, c, flags));
	}
	
	private static class CombatCreature {
		
		private final CreatureObject creature;
		private long lastCombat;
		
		public CombatCreature(CreatureObject creature) {
			this.creature = creature;
		}
		
		public void updateLastCombat() {
			lastCombat = System.nanoTime();
		}
		
		public double getTimeSinceCombat() {
			return (System.nanoTime() - lastCombat) / 1E6;
		}
		
		public CreatureObject getCreature() {
			return creature;
		}
		
	}
	
}
