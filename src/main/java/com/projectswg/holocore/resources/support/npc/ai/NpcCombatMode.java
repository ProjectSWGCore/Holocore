package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject.ScheduledMode;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class NpcCombatMode extends NpcMode {
	
	private static final double RETURN_THRESHOLD = 3;
	
	private final AtomicReference<NavigationPoint> returnLocation;
	private final Collection<CreatureObject> targets;
	private final List<NavigationPoint> movementPoints;
	
	private double lastAttack;
	private int movementIndex;
	
	public NpcCombatMode(AIObject obj) {
		super(obj, ScheduledMode.COMBAT);
		this.returnLocation = new AtomicReference<>(null);
		this.targets = new CopyOnWriteArraySet<>();
		this.movementPoints = new ArrayList<>();
		
		this.lastAttack = 0;
		this.movementIndex = 0;
	}
	
	@Override
	public void onPlayerMoveInAware(CreatureObject player, double distance) {
		if (distance < getSpawner().getAggressiveRadius() && player.isEnemyOf(getAI())) {
			if (getAI().isLineOfSight(player) && targets.add(player)) {
				requestAssistance();
				if (!isExecuting())
					requestModeStart();
			}
		} else {
			// If out of aggressive range, and not actively fighting
			if (!getAI().getDefenders().contains(player.getObjectId()))
				targets.remove(player);
		}
	}
	
	@Override
	public void onPlayerExitAware(CreatureObject player) {
		targets.remove(player);
	}
	
	@Override
	public void onModeStart() {
		returnLocation.set(new NavigationPoint(getAI().getParent(), getAI().getLocation(), getRunSpeed()));
		movementPoints.clear();
	}
	
	@Override
	public void act() {
		if (isRooted()) {
			queueNextLoop(1000);
			return;
		}
		
		syncTargets();
		if (!targets.isEmpty()) {
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
		NavigationPoint ret = returnLocation.get();
		return ret.distanceTo(getAI()) >= RETURN_THRESHOLD;
	}
	
	private void performCombatAction() {
		CreatureObject target = getPrimaryTarget();
		if (target == null)
			return;
		
		AIObject obj = getAI();
		WeaponObject weapon = obj.getEquippedWeapon();
		double targetRange = Math.max(1, weapon.getMaxRange()/2);
		boolean lineOfSight = obj.isLineOfSight(target);
		if (obj.getWorldLocation().distanceTo(target.getWorldLocation()) >= targetRange || !lineOfSight) { 
			// Recalculate path, since we won't get within range using the existing path
			if (movementPoints.isEmpty() || movementPoints.get(movementPoints.size()-1).distanceTo(target) >= targetRange) {
				movementPoints.clear();
				movementPoints.addAll(NavigationPoint.from(getAI().getParent(), getAI().getLocation(), target.getParent(), target.getLocation(), getRunSpeed()));
				movementIndex = 0;
			}
			assert movementIndex < movementPoints.size();
			movementPoints.get(movementIndex++).move(getAI());
		}
		
		if (lineOfSight) {
			double attackSpeed = getAI().getPrimaryWeapons().contains(weapon) ? getSpawner().getPrimaryWeaponSpeed() : getSpawner().getSecondaryWeaponSpeed();
			if (System.nanoTime() - lastAttack >= attackSpeed * 1E9) {
				lastAttack = System.nanoTime();
				attack(target, weapon);
			}
		}
	}
	
	private void performResetAction() {
		if (movementPoints.isEmpty() || movementPoints.get(movementPoints.size()-1).distanceTo(returnLocation.get()) >= RETURN_THRESHOLD) {
			movementPoints.clear();
			movementPoints.addAll(NavigationPoint.from(getAI().getParent(), getAI().getLocation(), returnLocation.get().getParent(), returnLocation.get().getLocation(), getRunSpeed()));
			movementIndex = 0;
		}
		
		assert movementIndex < movementPoints.size();
		movementPoints.get(movementIndex++).move(getAI());
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
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("creatureMeleeHit"), 0);
				break;
		}
	}
	
	@Nullable
	private CreatureObject getPrimaryTarget() {
		return targets.stream()
				.filter(creo -> creo.getHealth() > 0) // Don't attack if they're already dead
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
		double assistRange = getSpawner().getAssistRadius();
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
