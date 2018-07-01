package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.npc.spawn.Spawner;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class NpcCombatMode extends NpcMode {
	
	private static final double RETURN_THRESHOLD = 3;
	
	private final AtomicReference<Location> returnLocation;
	private final Collection<CreatureObject> targets;
	private final AtomicInteger attackCountdown;
	private final Spawner spawner;
	
	public NpcCombatMode(Spawner spawner) {
		this.returnLocation = new AtomicReference<>(null);
		this.targets = new CopyOnWriteArraySet<>();
		this.attackCountdown = new AtomicInteger(0);
		this.spawner = spawner;
	}
	
	@Override
	public void onPlayerMoveInAware(CreatureObject player, double distance) {
		if (distance < spawner.getAggressiveRadius()) {
			if (targets.add(player)) {
				requestAssistance();
				if (!isExecuting())
					requestModeStart();
			}
		} else {
			targets.remove(player);
		}
	}
	
	@Override
	public void onPlayerExitAware(CreatureObject player) {
		targets.remove(player);
	}
	
	@Override
	public void onModeStart() {
		returnLocation.set(null);
	}
	
	@Override
	public void act() {
		returnLocation.compareAndSet(null, getAI().getLocation());
		if (isRooted()) {
			queueNextLoop(1000);
			return;
		}
		
		syncTargets();
		if (isInActiveCombat()) {
			performCombatAction();
			queueNextLoop(1000);
		} else if (isReturning()) {
			performResetAction();
			queueNextLoop(1000);
		} else {
			requestModeEnd();
		}
	}
	
	private boolean isReturning() {
		Location ret = returnLocation.get();
		return getAI().isInCombat() || ret == null || getAI().getLocation().distanceTo(ret) >= RETURN_THRESHOLD;
	}
	
	private boolean isInActiveCombat() {
		if (getAI().isInCombat())
			return true;
		
		for (CreatureObject target : targets) {
			if (!target.hasBuff("incapWeaken") || spawner.isDeathblow())
				return true;
		}
		return false;
	}
	
	private void performCombatAction() {
		CreatureObject target = getPrimaryTarget();
		if (target == null)
			return;
		
		AIObject obj = getAI();
		WeaponObject weapon = obj.getEquippedWeapon();
		Location nextStep = AINavigationSupport.getNextStepTo(obj.getLocation(), target.getLocation(), 1, Math.max(1, weapon.getMaxRange()/2), getRunSpeed());
		runTo(nextStep);
		
		if (target.getPosture() != Posture.INCAPACITATED && target.getPosture() != Posture.DEAD && attackCountdown.decrementAndGet() <= 0) {
			attack(target, weapon);
			if (getAI().getPrimaryWeapons().contains(weapon))
				attackCountdown.set((int) spawner.getPrimaryWeaponSpeed());
			else
				attackCountdown.set((int) spawner.getSecondaryWeaponSpeed());
		}
	}
	
	private void performResetAction() {
		Location nextStep = AINavigationSupport.getNextStepTo(getAI().getLocation(), returnLocation.get(), getRunSpeed());
		runTo(nextStep);
	}
	
	private void attack(CreatureObject target, WeaponObject weapon) {
		AIObject obj = getAI();
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
		if (spawner.isDeathblow()) {
			return targets.stream()
					.filter(creo -> creo.getHealth() > 0) // Don't attack if they're already dead
					.min(Comparator.comparingInt(CreatureObject::getHealth)).orElse(null);
		}
		
		return targets.stream()
				.filter(creo -> creo.getHealth() > 0) // Don't attack if they're already dead
				.filter(creo -> !creo.hasBuff("incapWeaken")) // Don't attack if it'll be a DB
				.min(Comparator.comparingInt(CreatureObject::getHealth)).orElse(null);
	}
	
	private void syncTargets() {
		List<Long> defenders = getAI().getDefenders();
		boolean added = targets.addAll(getNearbyPlayers().stream()
				.filter(creo -> defenders.contains(creo.getObjectId()))
				.collect(Collectors.toList()));
		if (added)
			requestAssistance();
	}
	
	private void requestAssistance() {
		Location myLocation = getAI().getWorldLocation();
		double assistRange = spawner.getAssistRadius();
		getAI().getAware().stream()
				.filter(AIObject.class::isInstance) // get nearby AI
				.filter(ai -> ai.getWorldLocation().flatDistanceTo(myLocation) < assistRange) // that can assist
				.map(AIObject.class::cast)
				.map(this::requestPeerMode)
				.filter(Objects::nonNull) // not all nearby AI are able to attack
				.map(NpcCombatMode.class::cast)
				.forEach(ai -> ai.assist(this)); // halp
	}
	
	private void assist(NpcCombatMode peer) {
		if (targets.addAll(peer.targets) && !isExecuting())
			requestModeStart();
	}
	
}
