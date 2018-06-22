package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.intents.support.objects.swg.MoveObjectIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class AICombatSupport {
	
	private static final double RETURN_THRESHOLD = 3;
	private static final double SPEED_MOD = 1; // Since we execute twice a second
	
	private final AIObject obj;
	private final AtomicReference<Location> returnLocation;
	private final AtomicReference<CreatureObject> previousTarget;
	
	public AICombatSupport(AIObject obj) {
		this.obj = obj;
		this.returnLocation = new AtomicReference<>(null);
		this.previousTarget = new AtomicReference<>(null);
	}
	
	public boolean isExecuting() {
		Location ret = returnLocation.get();
		return obj.isInCombat() || ret == null || obj.getLocation().distanceTo(ret) >= RETURN_THRESHOLD;
	}
	
	public void reset() {
		returnLocation.set(null);
		previousTarget.set(null);
	}
	
	public void act() {
		returnLocation.compareAndSet(null, obj.getLocation());
		Log.d("Has Vendetta: %b", hasVendetta());
		if (obj.isInCombat() || hasVendetta()) {
			performCombatAction();
		} else {
			performResetAction();
		}
	}
	
	private boolean hasVendetta() {
		CreatureObject previousTarget = this.previousTarget.get();
		return previousTarget != null && previousTarget.hasBuff("incapWeaken") && obj.isDeathblow();
	}
	
	private void performCombatAction() {
		CreatureObject target = getPrimaryTarget();
		if (target == null)
			return;
		previousTarget.set(target);
		
		WeaponObject weapon = obj.getEquippedWeapon();
		double speed = obj.getRunSpeed() * SPEED_MOD;
		Location nextStep = AINavigationSupport.getNextStepTo(obj.getLocation(), target.getLocation(), 1, Math.max(1, weapon.getMaxRange()-1), speed);
		MoveObjectIntent.broadcast(obj, obj.getParent(), nextStep, speed, obj.getNextUpdateCount());
		
		if (target.getPosture() != Posture.INCAPACITATED && target.getPosture() != Posture.DEAD)
			attack(target, weapon);
	}
	
	private void performResetAction() {
		double speed = obj.getRunSpeed() * SPEED_MOD;
		Location nextStep = AINavigationSupport.getNextStepTo(obj.getLocation(), returnLocation.get(), speed);
		MoveObjectIntent.broadcast(obj, obj.getParent(), nextStep, speed, obj.getNextUpdateCount());
	}
	
	private void attack(CreatureObject target, WeaponObject weapon) {
		double distance = obj.getWorldLocation().distanceTo(target.getWorldLocation());
		if (distance > weapon.getMaxRange())
			return;
		obj.setIntendedTargetId(target.getObjectId());
		obj.setLookAtTargetId(target.getObjectId());
		if (target.getPosture() == Posture.INCAPACITATED) {
			QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("deathblow"), 0);
			return;
		}
		switch (weapon.getType()) {
			case PISTOL:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("rangedShotPistol"), 0);
				break;
			case RIFLE:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("rangedShotRifle"), 0);
				break;
			case LIGHT_RIFLE:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("rangedShotLightRifle"), 0);
				break;
			case CARBINE:
			case HEAVY:
			case HEAVY_WEAPON:
			case DIRECTIONAL_TARGET_WEAPON:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("rangedShot"), 0);
				break;
			case ONE_HANDED_MELEE:
			case TWO_HANDED_MELEE:
			case UNARMED:
			case POLEARM_MELEE:
			case THROWN:
			case ONE_HANDED_SABER:
			case TWO_HANDED_SABER:
			case POLEARM_SABER:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("meleeHit"), 0);
				break;
		}
	}
	
	@Nullable
	private CreatureObject getPrimaryTarget() {
		List<Long> defenders = obj.getDefenders();
		Location currentLocation = obj.getLocation();
		return obj.getObservers().stream()
				.map(Player::getCreatureObject)
				.filter(creo -> defenders.contains(creo.getObjectId()))
				.min(Comparator.comparingDouble(a -> currentLocation.distanceTo(a.getLocation())))
				.orElse(null);
	}
	
}
