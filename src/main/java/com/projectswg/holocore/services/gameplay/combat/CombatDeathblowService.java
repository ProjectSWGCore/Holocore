package com.projectswg.holocore.services.gameplay.combat;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.intents.gameplay.combat.*;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.StandardLog;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

public class CombatDeathblowService extends Service {
	
	private static final byte INCAP_TIMER = 10;    // Amount of seconds to be incapacitated
	
	private final Map<CreatureObject, Future<?>> incapacitatedCreatures;
	private final ScheduledThreadPool executor;
	
	public CombatDeathblowService() {
		this.incapacitatedCreatures = new ConcurrentHashMap<>();
		this.executor = new ScheduledThreadPool(1, 3, "combat-deathblow-service");
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
				new BuffIntent("incapWeaken", killer, corpse, true).broadcast();
				killCreature(killer, corpse);
			}
		} else {
			// Can't happen with the current code, but in case it's ever refactored...
			Log.e("Incapacitation timer for player %s being deathblown unexpectedly didn't exist!", "");
		}
	}
	
	@IntentHandler
	private void handleIncapacitateCreatureIntent(IncapacitateCreatureIntent ici) {
		incapacitatePlayer(ici.getIncapper(), ici.getIncappee());
	}
	
	@IntentHandler
	private void handleKillCreatureIntent(KillCreatureIntent kci) {
		killCreature(kci.getKiller(), kci.getCorpse());
	}
	
	@IntentHandler
	private void handleRequestCreatureDeathIntent(RequestCreatureDeathIntent rcdi) {
		CreatureObject corpse = rcdi.getCorpse();
		CreatureObject killer = rcdi.getKiller();
		
		boolean deathblow = !corpse.isPlayer() || corpse.hasBuff("incapWeaken");
		if (!deathblow && killer instanceof AIObject)
			deathblow = ((AIObject) killer).getSpawner().isDeathblow();
		
		if (deathblow) {
			killCreature(rcdi.getKiller(), corpse);
		} else {
			incapacitatePlayer(rcdi.getKiller(), corpse);
		}
		corpse.setHealth(0);
		corpse.setTurnScale(0);
		corpse.setMovementScale(0);
		
		ExitCombatIntent.broadcast(corpse);
	}
	
	private void incapacitatePlayer(CreatureObject incapacitator, CreatureObject incapacitated) {
		incapacitated.setCounter(INCAP_TIMER);
		incapacitated.setPosture(Posture.INCAPACITATED);
		
		StandardLog.onPlayerEvent(this, incapacitated, "was incapacitated by %s", incapacitator);
		
		// Once the incapacitation counter expires, revive them.
		incapacitatedCreatures.put(incapacitated, executor.execute(INCAP_TIMER * 1000, () -> expireIncapacitation(incapacitated)));
		
		new BuffIntent("incapWeaken", incapacitator, incapacitated, false).broadcast();
		Player incapacitatorOwner = incapacitator.getOwner();
		if (incapacitatorOwner != null) { // This will be NPCs most of the time
			new SystemMessageIntent(incapacitatorOwner, new ProsePackage(new StringId("base_player", "prose_target_incap"), "TT", incapacitated.getObjectName())).broadcast();
		}
		Player incapacitatedOwner = incapacitated.getOwner();
		if (incapacitatedOwner != null) { // Logged out player
			new SystemMessageIntent(incapacitatedOwner, new ProsePackage(new StringId("base_player", "prose_victim_incap"), "TT", incapacitator.getObjectName())).broadcast();
		}
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
		CreatureRevivedIntent.broadcast(revivedCreature);
		
		StandardLog.onPlayerEvent(this, revivedCreature, "was revived");
	}
	
	private void killCreature(CreatureObject killer, CreatureObject corpse) {
		// We don't want to kill a creature that is already dead
		if (corpse.getPosture() == Posture.DEAD)
			return;
		
		corpse.setPosture(Posture.DEAD);
		if (corpse.isPlayer())
			StandardLog.onPlayerEvent(this, corpse, "was killed by %s", killer);
		new CreatureKilledIntent(killer, corpse).broadcast();
	}
	
}
