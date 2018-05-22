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
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatAction.Defender;
import com.projectswg.common.network.packets.swg.zone.object_controller.combat.CombatSpam;
import com.projectswg.holocore.intents.chat.ChatCommandIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.commands.CombatCommand;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.resources.objects.weapon.WeaponObject;
import com.projectswg.holocore.services.objects.ObjectCreator;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

public class CombatCommandService extends Service {
	
	public CombatCommandService() {
		
	}
	
	@IntentHandler
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
				float range = combatCommand.getConeLength();
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
		String anim = combatCommand.getRandomAnimation(weapon.getType());	// Uses defaultAnim if it needs to
		
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
		
		healed.sendObserversAndSelf(action, flyText, effect, combatSpam);
	}
	
}
