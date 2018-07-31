package com.projectswg.holocore.resources.support.npc.ai;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Point3D;
import com.projectswg.holocore.intents.support.global.command.QueueCommandIntent;
import com.projectswg.holocore.intents.support.npc.ai.ScheduleNpcModeIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcCombatIntent;
import com.projectswg.holocore.intents.support.npc.ai.StartNpcMovementIntent;
import com.projectswg.holocore.intents.support.npc.ai.StopNpcMovementIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.NpcMode;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class NpcCombatMode extends NpcMode {
	
	private final AtomicReference<NavigationPoint> returnLocation;
	private final AtomicReference<Location> previousTargetLocation;
	private final Collection<CreatureObject> targets;
	private final AtomicLong iteration;
	private final double runSpeed;
	
	public NpcCombatMode(AIObject obj) {
		super(obj);
		this.returnLocation = new AtomicReference<>(null);
		this.previousTargetLocation = new AtomicReference<>(null);
		this.targets = new CopyOnWriteArraySet<>();
		this.iteration = new AtomicLong(0);
		this.runSpeed = DataManager.getConfig(ConfigFile.PRIMARY).getDouble("NPC-RUN-SPEED", 9);
	}
	
	@Override
	public void onPlayerMoveInAware(CreatureObject player, double distance) {
		if (distance > getSpawner().getAggressiveRadius() && !getAI().getDefenders().contains(player.getObjectId())) {
			// If out of aggressive range, and not actively fighting
			targets.remove(player);
		}
	}
	
	@Override
	public void onPlayerExitAware(CreatureObject player) {
		targets.remove(player);
	}
	
	@Override
	public void onModeStart() {
		returnLocation.set(NavigationPoint.at(getAI().getParent(), getAI().getLocation(), runSpeed));
	}
	
	@Override
	public void act() {
		if (isRooted()) {
			queueNextLoop(500);
			return;
		}
		
		if (!targets.isEmpty()) {
			performCombatAction();
			queueNextLoop(500);
		} else {
			ScheduleNpcModeIntent.broadcast(getAI(), new NpcNavigateMode(getAI(), returnLocation.get()));
		}
	}
	
	public void addTargets(Collection<CreatureObject> targets) {
		if (this.targets.addAll(targets))
			requestAssistance();
	}
	
	private void performCombatAction() {
		CreatureObject target = getPrimaryTarget();
		if (target == null)
			return;
		iteration.incrementAndGet();
		
		AIObject obj = getAI();
		WeaponObject weapon = obj.getEquippedWeapon();
		double targetRange = Math.max(1, weapon.getMaxRange()/3);
		boolean lineOfSight = obj.isLineOfSight(target);
		Location targetLocation = target.getWorldLocation();
		if (obj.getWorldLocation().distanceTo(targetLocation) >= targetRange || !lineOfSight) {
			Location prev = previousTargetLocation.getAndSet(targetLocation);
			if (prev == null) {
				StartNpcMovementIntent.broadcast(obj, target.getParent(), target.getLocation(), runSpeed);
			} else if (prev.distanceTo(targetLocation) >= 1 || !lineOfSight) {
				Point3D delta = targetLocation.getPosition();
				delta.translate(-prev.getX(), -prev.getY(), -prev.getZ());
				Location destination = target.getLocation();
				if (delta.flatDistanceTo(0, 0) < 50) {
					destination = Location.builder(destination).translatePosition(delta.getX()*2, delta.getY()*2, delta.getZ()*2).build();
				}
				StartNpcMovementIntent.broadcast(obj, target.getParent(), destination, runSpeed);
			}
		} else {
			StopNpcMovementIntent.broadcast(obj);
		}
		
		if (lineOfSight && iteration.get() % 4 == 0) {
			attack(target, weapon);
		}
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
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("creatureMeleeAttack"), 0);
				break;
			case ONE_HANDED_SABER:
			case TWO_HANDED_SABER:
			case POLEARM_SABER:
				QueueCommandIntent.broadcast(obj, target, "", DataLoader.commands().getCommand("saberHit"), 0);
				break;
		}
	}
	
	@Nullable
	private CreatureObject getPrimaryTarget() {
		return targets.stream()
				.filter(creo -> creo.isEnemyOf(getAI()))
				.filter(creo -> creo.getHealth() > 0) // Don't attack if they're already dead
				.min(Comparator.comparingInt(CreatureObject::getHealth)).orElse(null);
	}
	
	private void requestAssistance() {
		Location myLocation = getAI().getWorldLocation();
		double assistRange = getSpawner().getAssistRadius();
		getAI().getAware().stream()
				.filter(AIObject.class::isInstance) // get nearby AI
				.filter(ai -> ai.getWorldLocation().distanceTo(myLocation) < assistRange) // that can assist
				.map(AIObject.class::cast)
				.filter(ai -> targets.stream().anyMatch(ai::isEnemyOf))
				.forEach(ai -> StartNpcCombatIntent.broadcast(ai, targets));
	}
	
}
