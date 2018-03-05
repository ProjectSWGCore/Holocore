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

import com.projectswg.common.concurrency.PswgScheduledThreadPool;
import com.projectswg.common.control.Manager;
import com.projectswg.common.data.CRC;
import com.projectswg.common.data.RGB;
import com.projectswg.common.data.combat.*;
import com.projectswg.common.data.encodables.oob.OutOfBandPackage;
import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.BuffIntent;
import com.projectswg.holocore.intents.chat.ChatCommandIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.combat.*;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.commands.CombatCommand;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.tangible.TangibleObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;
import com.projectswg.holocore.services.loot.LootManager;
import com.projectswg.holocore.services.objects.ObjectCreator;

import java.awt.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class CombatManager extends Manager {
	
	private static final byte INCAP_TIMER = 10;    // Amount of seconds to be incapacitated
	
	private final Map<Long, CombatCreature> inCombat;
	private final Set<CreatureObject> regeneratingHealthCreatures;    // Only allowed outside of combat
	private final Set<CreatureObject> regeneratingActionCreatures;    // Always allowed
	private final Map<CreatureObject, Future<?>> incapacitatedCreatures;
	private final PswgScheduledThreadPool executor;
	private final Random random;
	
	public CombatManager() {
		this.inCombat = new ConcurrentHashMap<>();
		this.regeneratingHealthCreatures = new CopyOnWriteArraySet<>();
		this.regeneratingActionCreatures = new CopyOnWriteArraySet<>();
		this.incapacitatedCreatures = new ConcurrentHashMap<>();
		this.executor = new PswgScheduledThreadPool(1, "combat-service");
		this.random = new Random();
		
		addChildService(new CorpseService());
		addChildService(new CombatXpService());
		addChildService(new DuelPlayerService());
		addChildService(new LootManager());
		
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
	
	private void handleChatCommandIntent(ChatCommandIntent cci) {
		if (!cci.getCommand().isCombatCommand() || !(cci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand c = (CombatCommand) cci.getCommand();
		CreatureObject source = cci.getSource();
		SWGObject target = cci.getTarget();
		
		// Regardless of HitType, the command might have action cost
		addActionCost(source, c);
		
		// TODO implement support for remaining HitTypes
		switch (c.getHitType()) {
			case ATTACK:
				handleAttack(source, target, null, c);
				break;
			case BUFF:
				handleBuff(source, target, c);
				break;
			case HEAL:
				handleHeal(source, target, c);
				break;
			case DELAY_ATTACK:
				handleDelayAttack(source, target, c, cci.getArguments());
				break;
			default:
				handleStatus(source, CombatStatus.UNKNOWN);
				break;
		}
	}
	
	private void updateCombatList(CreatureObject creature) {
		CombatCreature combat = inCombat.computeIfAbsent(creature.getObjectId(), s -> new CombatCreature(creature));
		combat.updateLastCombat();
	}
	
	private void handleAttack(CreatureObject source, SWGObject target, SWGObject delayEgg, CombatCommand command) {
		if (!handleStatus(source, canPerform(source, target, command)))
			return;
		
		WeaponObject weapon = source.getEquippedWeapon();
		
		for (int i = 0; i < command.getAttackRolls(); i++) {
			AttackInfo info = new AttackInfo();
			
			switch (command.getAttackType()) {
				case SINGLE_TARGET:
					doCombatSingle(source, target, info, weapon, command);
					break;
				case AREA:
					doCombatArea(source, source, info, weapon, command, false);
					break;
				case TARGET_AREA:
					if (target != null) {
						// Same as AREA, but the target is the destination for the AoE and  can take damage
						doCombatArea(source, delayEgg != null ? delayEgg : target, info, weapon, command, true);
					} else {
						// TODO AoE based on Location instead of delay egg
					}
					
					break;
				default:
					break;
			}
		}
	}
	
	private void handleBuff(CreatureObject source, SWGObject target, CombatCommand combatCommand) {
		// TODO group buffs
		addBuff(source, source, combatCommand.getBuffNameSelf());
		
		if (!(target instanceof CreatureObject)) {
			return;	// Only CreatureObjects have buffs
		}
		
		String buffNameTarget = combatCommand.getBuffNameTarget();
		
		addBuff(source, (CreatureObject) target, buffNameTarget);
		
		CreatureObject creatureTarget = (CreatureObject) target;
		CombatAction action = new CombatAction(source.getObjectId());
		WeaponObject weapon = source.getEquippedWeapon();
		String anim = combatCommand.getRandomAnimation(weapon.getType());	// Uses defaultAnim if it needs to
		action.setPosture(source.getPosture());
		action.setAttackerId(source.getObjectId());
		action.setActionCrc(CRC.getCrc(anim));
		action.setWeaponId(weapon.getObjectId());
		action.setCommandCrc(combatCommand.getCrc());
		action.setTrail(TrailLocation.RIGHT_HAND);
		action.setUseLocation(false);
		
		action.addDefender(new Defender(source.getObjectId(), source.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		if (!buffNameTarget.isEmpty()) {
			action.addDefender(new Defender(creatureTarget.getObjectId(), creatureTarget.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		}
		
		CombatSpam combatSpam = new CombatSpam(source.getObjectId());
		
		combatSpam.setAttacker(source.getObjectId());
		combatSpam.setAttackerPosition(source.getLocation().getPosition());
		combatSpam.setWeapon(weapon.getObjectId());
		combatSpam.setDefender(target.getObjectId());
		combatSpam.setDefenderPosition(target.getLocation().getPosition());
		combatSpam.setInfo(new AttackInfo());
		combatSpam.setAttackName(new StringId("cmd_n", combatCommand.getName()));
		combatSpam.setSpamType(CombatSpamFilterType.ALL);
		// TODO doesn't look like a buff in the combat log
		
		source.sendObserversAndSelf(action, combatSpam);
	}
	
	private void handleHeal(CreatureObject source, SWGObject target, CombatCommand combatCommand) {
		int healAmount = combatCommand.getAddedDamage();
		int healingPotency = source.getSkillModValue("expertise_healing_all");
		
		if (healingPotency > 0) {
			healAmount *= healingPotency;
		}
		
		switch (combatCommand.getAttackType()) {
			case SINGLE_TARGET: {
				switch (combatCommand.getTargetType()) {
					case NONE: {	// No target used, always heals self
						doHeal(source, source, healAmount, combatCommand);
						break;
					}
					case REQUIRED: {    // Target is always used
						if (target == null) {
							return;
						}
						
						// Same logic as OPTIONAL and ALL, so no break!
					}
					case OPTIONAL:	// Appears to be the same as ALL
					case ALL: {	// Target is used IF supplied
						if (target != null) {
							if (!(target instanceof CreatureObject)) {
								return;
							}
							
							CreatureObject creatureTarget = (CreatureObject) target;
							
							if (source.isEnemy(creatureTarget)) {
								doHeal(source, source, healAmount, combatCommand);
							} else {
								doHeal(source, creatureTarget, healAmount, combatCommand);
							}
						} else {
							doHeal(source, source, healAmount, combatCommand);
						}
						
						break;
					}
				}
				break;
			}
			
			case AREA: {
				// Targets are never supplied for AoE heals
				float range = combatCommand.getConeLength();
				Location sourceLocation = source.getWorldLocation();
				
				// Heal ourselves
				doHeal(source, source, healAmount, combatCommand);
				
				for (SWGObject nearbyObject : source.getObjectsAware()) {
					if (sourceLocation.isWithinDistance(nearbyObject.getLocation(), range)) {
						if (!(nearbyObject instanceof CreatureObject)) {
							// We can't heal something that's not a creature
							continue;
						}
						
						CreatureObject nearbyCreature = (CreatureObject) nearbyObject;
						
						// Heal nearby friendly
						doHeal(source, nearbyCreature, healAmount, combatCommand);
					}
				}
				
				break;
			}
		}
	}
	
	private void handleDelayAttack(CreatureObject source, SWGObject target, CombatCommand combatCommand, String arguments[]) {
		Location eggLocation;
		SWGObject eggParent;
		
		switch (combatCommand.getEggPosition()) {
			case LOCATION:
				if (arguments[0].equals("a") || arguments[0].equals("c")) {    // is "c" in free-targeting mode
					eggLocation = source.getLocation();
				} else {
					eggLocation = new Location(Float.parseFloat(arguments[0]), Float.parseFloat(arguments[1]), Float.parseFloat(arguments[2]), source
							.getTerrain());
				}
				
				eggParent = source.getParent();
				break;
			default:
				Log.w("Unrecognised delay egg position %s from command %s - defaulting to SELF", combatCommand.getEggPosition(), combatCommand
						.getName());
			case SELF:
				eggLocation = source.getLocation();
				eggParent = source.getParent();
				break;
			case TARGET:
				eggLocation = target.getLocation();
				eggParent = target.getParent();
				break;
		}
		
		// Spawn delay egg object
		String eggTemplate = combatCommand.getDelayAttackEggTemplate();
		SWGObject delayEgg = eggTemplate.endsWith("generic_egg_small.iff") ? null : ObjectCreator.createObjectFromTemplate(eggTemplate);
		
		if (delayEgg != null) {
			delayEgg.setLocation(eggLocation);
			delayEgg.moveToContainer(eggParent);
			ObjectCreatedIntent.broadcast(delayEgg);
		}
		
		long interval = (long) (combatCommand.getInitialDelayAttackInterval() * 1000);
		executor.execute(interval, () -> delayEggLoop(delayEgg, source, target, combatCommand, 1));
	}
	
	private void delayEggLoop(final SWGObject delayEgg, final CreatureObject source, final SWGObject target, final CombatCommand combatCommand, final int currentLoop) {
		String delayAttackParticle = combatCommand.getDelayAttackParticle();
		
		// Show particle effect to everyone observing the delay egg, if one is defined
		if (delayEgg != null && !delayAttackParticle.isEmpty())
			delayEgg.sendObservers(new PlayClientEffectObjectMessage(delayAttackParticle, "", delayEgg.getObjectId(), ""));
		
		// Handle the attack of this loop
		handleAttack(source, target, delayEgg, combatCommand);
		
		if (currentLoop < combatCommand.getDelayAttackLoops()) {
			// Recursively schedule another loop if that wouldn't exceed the amount of loops we need to perform
			long interval = (long) (combatCommand.getDelayAttackInterval() * 1000);
			executor.execute(interval, () -> delayEggLoop(delayEgg, source, target, combatCommand, currentLoop + 1));
		} else if (delayEgg != null) {
			// The delayed attack has ended - destroy the egg
			new DestroyObjectIntent(delayEgg).broadcast();
		}
	}
	
	private void doHeal(CreatureObject healer, CreatureObject healed, int healAmount, CombatCommand combatCommand) {
		String attribName;
		
		switch (combatCommand.getHealAttrib()) {
			case HEALTH: {
				healed.modifyHealth(healAmount);
				attribName = "HEALTH";
				break;
			}
			
			case ACTION: {
				healed.modifyAction(healAmount);
				attribName = "ACTION";
				break;
			}
			
			default:
				return;
		}
		
		CombatAction action = new CombatAction(healer.getObjectId());
		WeaponObject weapon = healer.getEquippedWeapon();
		String anim = combatCommand.getRandomAnimation(weapon.getType());	// Uses defaultAnim if it needs to
		
		action.setPosture(healed.getPosture());
		action.setAttackerId(healer.getObjectId());
		action.setActionCrc(CRC.getCrc(anim));
		action.setWeaponId(weapon.getObjectId());
		action.setCommandCrc(combatCommand.getCrc());
		action.setTrail(TrailLocation.RIGHT_HAND);
		action.setUseLocation(false);
		
		action.addDefender(new Defender(healed.getObjectId(), healed.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		OutOfBandPackage oobp = new OutOfBandPackage(new ProsePackage("StringId", new StringId("healing", "heal_fly"), "DI", healAmount, "TO", attribName));
		ShowFlyText flyText = new ShowFlyText(healed.getObjectId(), oobp, Scale.MEDIUM, new RGB(46, 139, 87), ShowFlyText.Flag.IS_HEAL);
		PlayClientEffectObjectMessage effect = new PlayClientEffectObjectMessage("appearance/pt_heal.prt", "root", healed.getObjectId(), "");
		CombatSpam combatSpam = new CombatSpam(healer.getObjectId());
		
		combatSpam.setAttacker(healer.getObjectId());
		combatSpam.setAttackerPosition(healer.getLocation().getPosition());
		combatSpam.setWeapon(weapon.getObjectId());
		combatSpam.setWeaponName(weapon.getStringId());
		combatSpam.setDefender(healed.getObjectId());
		combatSpam.setDefenderPosition(healed.getLocation().getPosition());
		combatSpam.setInfo(new AttackInfo());
		combatSpam.setAttackName(new StringId("cmd_n", combatCommand.getName()));
		combatSpam.setSpamType(CombatSpamFilterType.ALL);
		// TODO doesn't look like a heal in the combat log
		
		healed.sendObserversAndSelf(action, flyText, effect, combatSpam);
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
				target.sendObserversAndSelf(combatSpam);
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
			
			target.sendObserversAndSelf(combatSpam);
			
			int finalDamage = info.getFinalDamage();
			
			action.addDefender(new Defender(target.getObjectId(), target
					.getPosture(), true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) finalDamage));
			
			target.handleDamage(source, finalDamage);
			
			if (target.getHealth() <= finalDamage)
				doCreatureDeath(target, source);
			else
				target.modifyHealth(-finalDamage);
		}
		
		source.sendObserversAndSelf(action);
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
	
	private void doCreatureDeath(CreatureObject corpse, CreatureObject killer) {
		corpse.setHealth(0);
		killer.removeDefender(corpse);
		
		if (!killer.hasDefenders()) {
			exitCombat(killer);
		}
		
		// The creature should not be able to move or turn.
		corpse.setTurnScale(0);
		corpse.setMovementScale(0);
		
		if (corpse.isPlayer()) {
			if (corpse.hasBuff("incapWeaken")) {
				killCreature(killer, corpse);
			} else {
				incapacitatePlayer(killer, corpse);
			}
		} else {
			killCreature(killer, corpse);
		}
		
		exitCombat(corpse);
	}
	
	private void incapacitatePlayer(CreatureObject incapacitator, CreatureObject incapacitated) {
		incapacitated.setPosture(Posture.INCAPACITATED);
		incapacitated.setCounter(INCAP_TIMER);
		
		Log.i("%s was incapacitated", incapacitated);
		
		// Once the incapacitation counter expires, revive them.
		incapacitatedCreatures.put(incapacitated, executor.execute(INCAP_TIMER * 1000, () -> expireIncapacitation(incapacitated)));
		
		new BuffIntent("incapWeaken", incapacitator, incapacitated, false).broadcast();
		new SystemMessageIntent(incapacitator.getOwner(), new ProsePackage(new StringId("base_player", "prose_target_incap"), "TT", incapacitated
				.getObjectName())).broadcast();
		new SystemMessageIntent(incapacitated.getOwner(), new ProsePackage(new StringId("base_player", "prose_victim_incap"), "TT", incapacitator
				.getObjectName())).broadcast();
		new CreatureIncapacitatedIntent(incapacitator, incapacitated).broadcast();
	}
	
	private void expireIncapacitation(CreatureObject incapacitatedPlayer) {
		incapacitatedCreatures.remove(incapacitatedPlayer);
		reviveCreature(incapacitatedPlayer);
	}
	
	private void reviveCreature(CreatureObject revivedCreature) {
		if (revivedCreature.isPlayer())
			revivedCreature.setCounter(0);
		
		revivedCreature.setPosture(Posture.UPRIGHT);
		
		// The creature is now able to turn around and move
		revivedCreature.setTurnScale(1);
		revivedCreature.setMovementScale(1);
		
		// Give 'em a percentage of their health and schedule them for HAM regeneration.
		revivedCreature.setHealth((int) (revivedCreature.getBaseHealth() * 0.1));    // Restores 10% health of their base health
		startHealthRegeneration(revivedCreature);
		startActionRegeneration(revivedCreature);
		
		Log.i("%s was revived", revivedCreature);
	}
	
	private void killCreature(CreatureObject killer, CreatureObject corpse) {
		// We don't want to kill a creature that is already dead
		if (corpse.getPosture() == Posture.DEAD)
			return;
		
		corpse.setPosture(Posture.DEAD);
		new CreatureKilledIntent(killer, corpse).broadcast();
	}
	
	private void handleDeathblowIntent(DeathblowIntent di) {
		CreatureObject killer = di.getKiller();
		CreatureObject corpse = di.getCorpse();
		
		// Only deathblowing players is allowed!
		if (!corpse.isPlayer()) {
			return;
		}
		
		// They must be enemies
		if (!corpse.isEnemy(killer)) {
			return;
		}
		
		// The target of the deathblow must be incapacitated!
		if (corpse.getPosture() != Posture.INCAPACITATED) {
			return;
		}
		
		// If they're deathblown while incapacitated, their incapacitation expiration timer should cancel
		Future<?> incapacitationTimer = incapacitatedCreatures.remove(corpse);
		
		if (incapacitationTimer != null) {
			if (incapacitationTimer.cancel(false)) {    // If the task is running, let them get back up
				killCreature(killer, corpse);
			}
		} else {
			// Can't happen with the current code, but in case it's ever refactored...
			Log.e("Incapacitation timer for player %s being deathblown unexpectedly didn't exist!", "");
		}
	}
	
	private boolean handleStatus(CreatureObject source, CombatStatus status) {
		switch (status) {
			case SUCCESS:
				return true;
			case NO_TARGET:
				showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, Color.WHITE, ShowFlyText.Flag.PRIVATE);
				return false;
			case TOO_FAR:
				showFlyText(source, "@combat_effects:range_too_far", Scale.MEDIUM, Color.CYAN, ShowFlyText.Flag.PRIVATE);
				return false;
			case INVALID_TARGET:
				showFlyText(source, "@combat_effects:target_invalid_fly", Scale.MEDIUM, Color.CYAN, ShowFlyText.Flag.PRIVATE);
				return false;
			default:
				showFlyText(source, "@combat_effects:action_failed", Scale.MEDIUM, Color.WHITE, ShowFlyText.Flag.PRIVATE);
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
		
		if (!source.isEnemy((TangibleObject) target)) {
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
		double actionCost = command.getActionCost();
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
	
	private void showFlyText(TangibleObject obj, String text, Scale scale, Color c, ShowFlyText.Flag... flags) {
		obj.sendSelf(new ShowFlyText(obj.getObjectId(), text, scale, new RGB(c), flags));
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
