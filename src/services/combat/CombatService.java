/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package services.combat;

import java.awt.Color;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.object_controller.ShowFlyText;
import network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import network.packets.swg.zone.object_controller.combat.CombatAction;
import intents.chat.ChatCommandIntent;
import java.util.Iterator;
import resources.Posture;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.combat.AttackInfoLight;
import resources.combat.AttackType;
import resources.combat.CombatStatus;
import resources.combat.HitLocation;
import resources.combat.TrailLocation;
import resources.commands.CombatCommand;
import resources.common.CRC;
import resources.common.RGB;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.server_info.Log;
import utilities.Scripts;
import utilities.ThreadUtilities;

public class CombatService extends Service {
	
	private ScheduledExecutorService executor;
	
	private final Map<Long, CombatCreature> inCombat;
	private final Set<CreatureObject> regeneratingHealthCreatures;	// Only allowed outside of combat
	private final Set<CreatureObject> regeneratingActionCreatures;	// Always allowed
	
	public CombatService() {
		inCombat = new HashMap<>();
		regeneratingHealthCreatures = new HashSet<>();
		regeneratingActionCreatures = new HashSet<>();
	}
	
	@Override
	public boolean initialize() {
		registerForIntent(ChatCommandIntent.TYPE);
		executor = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("combat-service"));
		return super.initialize();
	}
	
	@Override
	public boolean start() {
		executor.scheduleAtFixedRate(() -> periodicChecks(), 0, 5, TimeUnit.SECONDS);
		executor.scheduleAtFixedRate(() -> periodicRegeneration(), 1, 1, TimeUnit.SECONDS);
		return super.start();
	}
	
	@Override
	public boolean terminate() {
		if (executor != null) {
			executor.shutdownNow();
			try {
				executor.awaitTermination(3, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				
			}
		}
		return super.terminate();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if (i instanceof ChatCommandIntent)
			processChatCommand((ChatCommandIntent) i);
	}
	
	private void periodicChecks() {
		Set<CombatCreature> removeSet = new HashSet<>();
		synchronized (inCombat) {
			for (CombatCreature combat : inCombat.values()) {
				if (combat.getTimeSinceCombat() >= 10E3) { // 10 sec
					removeSet.add(combat);
				}
			}
		}
		for (CombatCreature combat : removeSet)
			removeFromCombat(combat);
	}
	
	private void removeFromCombat(CombatCreature combat) {
		synchronized (inCombat) {
			inCombat.remove(combat.getCreature().getObjectId());
		}
		exitCombat(combat.getCreature());
	}
	
	private void periodicRegeneration() {
		synchronized (regeneratingActionCreatures) {
			Iterator<CreatureObject> iterator = regeneratingActionCreatures.iterator();

			while (iterator.hasNext()) {
				regenerationActionTick(iterator.next(), iterator);
			}
		}

		synchronized (regeneratingHealthCreatures) {
			Iterator<CreatureObject> iterator = regeneratingHealthCreatures.iterator();

			while (iterator.hasNext()) {
				regenerationHealthTick(iterator.next(), iterator);
			}
		}
	}
	
	private void regenerationActionTick(CreatureObject creatureObject, Iterator<CreatureObject> iterator) {
		if(creatureObject.getAction() < creatureObject.getMaxAction()) {
			int modification = 13;
			int level = creatureObject.getLevel();
			
			if(level > 1) {
				modification += 4 * level;
			}
			
			
			if(creatureObject.modifyAction(modification) == 0) {
				// Their action didn't change, meaning they're maxed out
				iterator.remove();
			}
		} else {
			// Maxed out - remove 'em
			iterator.remove();
		}
	}
	
	private void regenerationHealthTick(CreatureObject creatureObject, Iterator<CreatureObject> iterator) {
		if(creatureObject.getHealth() < creatureObject.getMaxHealth()) {
			int modification = 40;
			int level = creatureObject.getLevel();
			
			if(level > 1) {
				modification += 4 * level;
			}
			
			if(creatureObject.modifyHealth(modification) == 0) {
				// Their health didn't change, meaning they're maxed out
				iterator.remove();
			}
		} else {
			// Maxed out - remove 'em
			iterator.remove();
		}
	}
	
	private void processChatCommand(ChatCommandIntent cci) {
		if (!cci.getCommand().isCombatCommand() || !(cci.getCommand() instanceof CombatCommand))
			return;
		CombatCommand c = (CombatCommand) cci.getCommand();
		CombatStatus status = canPerform(cci.getSource(), cci.getTarget(), c);
		if (!handleStatus(cci.getSource(), status))
			return;
		Object res = Scripts.invoke("commands/combat/"+c.getName(), "doCombat", cci.getSource(), cci.getTarget(), c);
		if (res == null) {
			handleStatus(cci.getSource(), CombatStatus.UNKNOWN);
			return;
		}
		updateCombatList(cci.getSource());
		if (cci.getTarget() instanceof CreatureObject)
			updateCombatList((CreatureObject) cci.getTarget());
		if (res instanceof Number)
			doCombat(cci.getSource(), cci.getTarget(), new AttackInfoLight(((Number) res).intValue()), c);
		else if (res instanceof AttackInfoLight)
			doCombat(cci.getSource(), cci.getTarget(), (AttackInfoLight) res, c);
		else {
			Log.w(this, "Unknown return from combat script: " + res);
			return;
		}
	}
	
	private void updateCombatList(CreatureObject creature) {
		CombatCreature combat;
		synchronized (inCombat) {
			combat = inCombat.get(creature.getObjectId());
		}
		if (combat == null) {
			combat = new CombatCreature(creature);
			synchronized (inCombat) {
				inCombat.put(creature.getObjectId(), combat);
			}
		}
		combat.updateLastCombat();
	}
	
	private void doCombat(CreatureObject source, SWGObject target, AttackInfoLight info, CombatCommand command) {
		CombatAction action = new CombatAction(source.getObjectId());
		String anim = command.getRandomAnimation(source.getEquippedWeapon().getType());
		action.setActionCrc(CRC.getCrc(anim));
		action.setAttacker(source);
		action.setClientEffectId((byte) 0);
		action.setCommandCrc(command.getCrc());
		action.setTrail(TrailLocation.WEAPON);
		action.setUseLocation(false);
		if (target instanceof CreatureObject) {
			if (command.getAttackType() == AttackType.SINGLE_TARGET)
				doCombatSingle(source, (CreatureObject) target, info, command);
			action.addDefender((CreatureObject) target, true, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) info.getDamage());
		}
		source.sendObserversAndSelf(action);
	}
	
	private void doCombatSingle(CreatureObject source, CreatureObject target, AttackInfoLight info, CombatCommand command) {
		if (!source.isInCombat())
			enterCombat(source);
		if (!target.isInCombat())
			enterCombat(target);
		target.addDefender(source);
		source.addDefender(target);
		// Note: This will not kill anyone
		if (target.getHealth() <= info.getDamage())
			doCreatureDeath(target, source);
		else
			target.modifyHealth(-info.getDamage());
	}
	
	private void enterCombat(CreatureObject creature) {
		creature.setInCombat(true);
		
		// If this creature is currently regenerating health, they should stop doing so now
		synchronized(regeneratingHealthCreatures) {
			regeneratingHealthCreatures.remove(creature);
		}
	}
	
	private void exitCombat(CreatureObject creature) {
		creature.setInCombat(false);
		creature.clearDefenders();
		
		// Once out of combat, we can regenerate health.
		synchronized(regeneratingHealthCreatures) {
			regeneratingHealthCreatures.add(creature);
		}
	}
	
	private void doCreatureDeath(CreatureObject killedCreature, CreatureObject killer) {
		killedCreature.setHealth(0);
		exitCombat(killedCreature);
		killer.removeDefender(killedCreature);
		
		// Let's check if the killer needs to remain in-combat...
		if(!killer.hasDefenders()) {
			// They have no active targets they're in combat with, make 'em exit combat
			exitCombat(killer);
		}
		
		// We need to handle this differently, depending on whether killedCreature is a player or not
		if(killedCreature.isPlayer()) {
			// TODO account for AI deathblowing players..?
			// If it's a player, they need to be incapacitated
			incapacitatePlayer(killedCreature, killer);
		} else {
			// This is just a plain ol' NPC. Die!
			killCreature(killedCreature, killer);
		}
	}
	
	private void incapacitatePlayer(CreatureObject incapacitatedPlayer, CreatureObject incapacitator) {
		incapacitatedPlayer.setPosture(Posture.INCAPACITATED);
		incapacitatedPlayer.setCounter(15);	// Do we need to count down this?
	}
	
	private void killCreature(CreatureObject killedCreature, CreatureObject killer) {
		killedCreature.setPosture(Posture.DEAD);
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
			default:
				showFlyText(source, "@combat_effects:cant_attack_fly", Scale.MEDIUM, Color.WHITE, ShowFlyText.Flag.PRIVATE);
				Log.e(this, "Character unable to attack. Player: %s  Reason: %s", source.getName(), status);
				return false;
		}
	}
	
	private CombatStatus canPerform(CreatureObject source, SWGObject target, CombatCommand c) {
		if (source.getEquippedWeapon() == null)
			return CombatStatus.NO_WEAPON;
		if (!(target instanceof TangibleObject))
			return CombatStatus.INVALID_TARGET;
		TangibleObject tangibleTarget = (TangibleObject) target;
		if(tangibleTarget.getPvpFaction() != PvpFaction.NEUTRAL) {
			if(!tangibleTarget.isEnemy(source)) {
				return CombatStatus.INVALID_TARGET;
			}
		} else if ((tangibleTarget.getPvpFlags() & PvpFlag.ATTACKABLE.getBitmask()) == 0)
			return CombatStatus.INVALID_TARGET;
		
		CombatStatus status;
		switch (c.getAttackType()) {
			case AREA:
			case TARGET_AREA:
				status = canPerformArea(source, c);
				break;
			case SINGLE_TARGET:
				status = canPerformSingle(source, target, c);
				break;
			default:
				status = CombatStatus.UNKNOWN;
				break;
		}
		if (status != CombatStatus.SUCCESS)
			return status;
		status = Scripts.invoke("commands/combat/"+c.getName(), "canPerform", source, target, c);
		if (status == null) {
			return CombatStatus.UNKNOWN;
		}
		return status;
	}
	
	private CombatStatus canPerformSingle(CreatureObject source, SWGObject target, CombatCommand c) {
		if (target == null || !(target instanceof CreatureObject))
			return CombatStatus.NO_TARGET;
		WeaponObject weapon = source.getEquippedWeapon();
		double dist = source.getLocation().distanceTo(target.getLocation());
		if (dist > weapon.getMaxRange() || (dist > c.getMaxRange() && c.getMaxRange() > 0))
			return CombatStatus.TOO_FAR;
		return CombatStatus.SUCCESS;
	}
	
	private CombatStatus canPerformArea(CreatureObject source, CombatCommand c) {
		return CombatStatus.SUCCESS;
	}
	
	private void showFlyText(TangibleObject obj, String text, Scale scale, Color c, ShowFlyText.Flag ... flags) {
		obj.sendSelf(new ShowFlyText(obj.getObjectId(), text, scale, new RGB(c), flags));
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
