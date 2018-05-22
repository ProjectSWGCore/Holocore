package com.projectswg.holocore.services.combat;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.intents.BuffIntent;
import com.projectswg.holocore.intents.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.combat.CreatureIncapacitatedIntent;
import com.projectswg.holocore.intents.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.combat.DeathblowIntent;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class CombatDeathblowService extends Service {
	
	private static final byte INCAP_TIMER = 10;    // Amount of seconds to be incapacitated
	
	private final Map<CreatureObject, Future<?>> incapacitatedCreatures;
	
	public CombatDeathblowService() {
		this.incapacitatedCreatures = new ConcurrentHashMap<>();
	}
	
	@IntentHandler
	private void handleDeathblowIntent(DeathblowIntent di) {
		CreatureObject killer = di.getKiller();
		CreatureObject corpse = di.getCorpse();
		
		// Only deathblowing players is allowed!
		if (!corpse.isPlayer()) {
			return;
		}
		
		// They must be enemies
		if (!corpse.isEnemyOf(killer)) {
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
	
}
