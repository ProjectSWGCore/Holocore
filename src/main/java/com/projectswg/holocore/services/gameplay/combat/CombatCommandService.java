package com.projectswg.holocore.services.gameplay.combat;

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
import com.projectswg.holocore.intents.gameplay.combat.EnterCombatIntent;
import com.projectswg.holocore.intents.gameplay.combat.RequestCreatureDeathIntent;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.support.global.command.ExecuteCommandIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.commands.CombatCommand;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

public class CombatCommandService extends Service {
	
	private static final RGB COLOR_WHITE = new RGB(255, 255, 255);
	private static final RGB COLOR_CYAN = new RGB(0, 255, 255);
	
	private final ScheduledThreadPool executor;
	private final Random random;
	
	public CombatCommandService() {
		this.executor = new ScheduledThreadPool(2, "combat-command-service-%d");
		this.random = new Random();
	}
	
	@Override
	public boolean start() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		executor.awaitTermination(1000);
		return true;
	}
	
	@IntentHandler
	private void handleChatCommandIntent(ExecuteCommandIntent eci) {
		if (!eci.getCommand().isCombatCommand() || !(eci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand c = (CombatCommand) eci.getCommand();
		CreatureObject source = eci.getSource();
		SWGObject target = eci.getTarget();
		
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
				handleDelayAttack(source, target, c, eci.getArguments());
				break;
			default:
				handleStatus(source, CombatStatus.UNKNOWN);
				break;
		}
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
			return;    // Only CreatureObjects have buffs
		}
		
		String buffNameTarget = combatCommand.getBuffNameTarget();
		
		addBuff(source, (CreatureObject) target, buffNameTarget);
		
		CreatureObject creatureTarget = (CreatureObject) target;
		CombatAction action = new CombatAction(source.getObjectId());
		WeaponObject weapon = source.getEquippedWeapon();
		String anim = combatCommand.getRandomAnimation(weapon.getType());    // Uses defaultAnim if it needs to
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
		
		source.sendObservers(action, combatSpam);
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
					case NONE: {    // No target used, always heals self
						doHeal(source, source, healAmount, combatCommand);
						break;
					}
					case REQUIRED: {    // Target is always used
						if (target == null) {
							return;
						}
						
						// Same logic as OPTIONAL and ALL, so no break!
					}
					case OPTIONAL:    // Appears to be the same as ALL
					case ALL: {    // Target is used IF supplied
						if (target != null) {
							if (!(target instanceof CreatureObject)) {
								return;
							}
							
							CreatureObject creatureTarget = (CreatureObject) target;
							
							if (source.isEnemyOf(creatureTarget)) {
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
				double range = combatCommand.getConeLength();
				Location sourceLocation = source.getWorldLocation();
				
				for (SWGObject nearbyObject : source.getAware()) {
					if (sourceLocation.isWithinDistance(nearbyObject.getLocation(), range)) {
						if (!(nearbyObject instanceof CreatureObject)) {
							// We can't heal something that's not a creature
							continue;
						}
						
						CreatureObject nearbyCreature = (CreatureObject) nearbyObject;
						
						if (source.isAttackable(nearbyCreature)) {
							continue;
						}
						
						// Heal nearby friendly
						doHeal(source, nearbyCreature, healAmount, combatCommand);
					}
				}
				
				break;
			}
		}
	}
	
	private void handleDelayAttack(CreatureObject source, SWGObject target, CombatCommand combatCommand, String arguments) {
		String [] argSplit = arguments.split(" ");
		Location eggLocation;
		SWGObject eggParent;
		
		switch (combatCommand.getEggPosition()) {
			case LOCATION:
				if (argSplit[0].equals("a") || argSplit[0].equals("c")) {    // is "c" in free-targeting mode
					eggLocation = source.getLocation();
				} else {
					eggLocation = new Location(Float.parseFloat(argSplit[0]), Float.parseFloat(argSplit[1]), Float.parseFloat(argSplit[2]), source.getTerrain());
				}
				
				eggParent = source.getParent();
				break;
			default:
				Log.w("Unrecognised delay egg position %s from command %s - defaulting to SELF", combatCommand.getEggPosition(), combatCommand.getName());
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
			delayEgg.moveToContainer(eggParent, eggLocation);
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
		int difference;
		
		switch (combatCommand.getHealAttrib()) {
			case HEALTH: {
				int currentHealth = healed.getHealth();
				healed.modifyHealth(healAmount);
				difference = healed.getHealth() - currentHealth;
				attribName = "HEALTH";
				break;
			}
			
			case ACTION: {
				int currentAction = healed.getAction();
				healed.modifyAction(healAmount);
				difference = healed.getAction() - currentAction;
				attribName = "ACTION";
				break;
			}
			
			default:
				return;
		}
		
		CombatAction action = new CombatAction(healer.getObjectId());
		WeaponObject weapon = healer.getEquippedWeapon();
		String anim = combatCommand.getRandomAnimation(weapon.getType());    // Uses defaultAnim if it needs to
		
		action.setPosture(healed.getPosture());
		action.setAttackerId(healer.getObjectId());
		action.setActionCrc(CRC.getCrc(anim));
		action.setWeaponId(weapon.getObjectId());
		action.setCommandCrc(combatCommand.getCrc());
		action.setTrail(TrailLocation.RIGHT_HAND);
		action.setUseLocation(false);
		
		action.addDefender(new Defender(healed.getObjectId(), healed.getPosture(), false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0));
		
		OutOfBandPackage oobp = new OutOfBandPackage(new ProsePackage("StringId", new StringId("healing", "heal_fly"), "DI", difference, "TO", attribName));
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
		
		healed.sendObservers(action, flyText, effect, combatSpam);
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
		double aoeRange = command.getConeLength();
		SWGObject originParent = origin.getParent();
		Collection<SWGObject> objectsToCheck = originParent == null ? origin.getObjectsAware() : originParent.getContainedObjects();
		
		// TODO block
		// TODO evasion if no block
		
		// TODO line of sight checks between the explosive and each target
		Set<CreatureObject> targets = objectsToCheck.stream().filter(CreatureObject.class::isInstance).map(CreatureObject.class::cast).filter(source::isAttackable)
				.filter(target -> canPerform(source, target, command) == CombatStatus.SUCCESS).filter(creature -> origin.getLocation().distanceTo(creature.getLocation()) <= aoeRange)
				.collect(Collectors.toSet());
		
		// This way, mines or grenades won't try to harm themselves
		if (includeOrigin && origin instanceof CreatureObject)
			targets.add((CreatureObject) origin);
		
		doCombat(source, targets, weapon, info, command);
	}
	
	private void doCombat(CreatureObject source, Set<CreatureObject> targets, WeaponObject weapon, AttackInfo info, CombatCommand command) {
		source.updateLastCombatTime();
		
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
			target.updateLastCombatTime();
			
			EnterCombatIntent.broadcast(source, target);
			EnterCombatIntent.broadcast(target, source);
			
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
			
			action.addDefender(new Defender(target.getObjectId(), target.getPosture(), true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) finalDamage));
			
			target.handleDamage(source, finalDamage);
			
			if (target.getHealth() <= finalDamage)
				RequestCreatureDeathIntent.broadcast(target, source);
			else
				target.modifyHealth(-finalDamage);
		}
		
		source.sendObservers(action);
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
		
		if (!source.isLineOfSight(target))
			return CombatStatus.TOO_FAR;
		
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
		if (!(target instanceof TangibleObject))
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
	
	private void showFlyText(TangibleObject obj, String text, Scale scale, RGB c, ShowFlyText.Flag... flags) {
		obj.sendSelf(new ShowFlyText(obj.getObjectId(), text, scale, c, flags));
	}
	
}
