/***********************************************************************************
 * Copyright (c) 2025 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline.BaselineType;
import com.projectswg.common.network.packets.swg.zone.deltas.DeltasMessage;
import com.projectswg.common.network.packets.swg.zone.spatial.AttributeList;
import com.projectswg.holocore.resources.gameplay.crafting.trade.TradeSession;
import com.projectswg.holocore.resources.gameplay.player.group.GroupInviterData;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.data.location.InstanceLocation;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerState;
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.attributes.AttributesMutable;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.OptionFlag;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class CreatureObject extends TangibleObject {
	
	private final CreatureObjectAwareness		awareness	= new CreatureObjectAwareness(this);
	private final CreatureObjectShared			creo3		= new CreatureObjectShared(this);
	private final CreatureObjectClientServerNP	creo4 		= new CreatureObjectClientServerNP(this);
	private final CreatureObjectSharedNP		creo6 		= new CreatureObjectSharedNP(this);
	private final Map<CreatureObject, Integer> hateMap = new HashMap<>();
	private final List<CreatureObject>			sentDuels		= new ArrayList<>();
	private final Set<Container>				containersOpen	= ConcurrentHashMap.newKeySet();
	private final List<DeltasMessage>			pendingDeltas	= new ArrayList<>();
	private final AtomicReference<Player>		owner			= new AtomicReference<>(null);
	
	private Race	race					= Race.HUMAN_MALE;
	private long	lastIncapTime			= 0;
	private TradeSession tradeSession		= null;
	
	private SWGSet<String> skills					= new SWGSet<>(1, 3, StringType.ASCII);
	private final AttributesMutable baseAttributes;
	
	public CreatureObject(long objectId) {
		super(objectId, BaselineType.CREO);
		this.baseAttributes = new AttributesMutable(this, 1, 2);
		initBaseAttributes();
		getAwareness().setAware(AwarenessType.SELF, List.of(this));
	}
	
	public void flushAwareness() {
		Player owner = getOwnerShallow();
		if (getTerrain() == Terrain.GONE || owner == null || owner.getPlayerState() == PlayerState.DISCONNECTED) {
			awareness.flushNoPlayer();
		} else {
			awareness.flush(owner);
			sendAndFlushAllDeltas();
		}
	}
	
	public void resetObjectsAware() {
		awareness.resetObjectsAware();
	}
	
	@Override
	public void addObject(SWGObject obj) {
		super.addObject(obj);
		if (obj.getSlotArrangement() != -1 && !(obj instanceof PlayerObject) && !super.hasOptionFlags(OptionFlag.MOUNT)) {
			addEquipment(obj);
		}
	}
	
	@Override
	public void removeObject(SWGObject obj) {
		super.removeObject(obj);
		removeEquipment(obj);
	}
	
	public void setOwner(@Nullable Player owner) {
		Player previous = this.owner.getAndSet(owner);
		if (previous != null)
			previous.setCreatureObject(null);
		if (owner != null)
			owner.setCreatureObject(this);
	}

	@Override
	@Nullable
	public Player getOwner() {
		return owner.get();
	}

	@Override
	@Nullable
	public Player getOwnerShallow() {
		return owner.get();
	}

	@NotNull
	public SWGObject getInventory() {
		SWGObject inventory = getSlottedObject("inventory");
		assert inventory != null;
		return inventory;
	}
	
	@NotNull
	public SWGObject getDatapad() {
		SWGObject datapad = getSlottedObject("datapad");
		assert datapad != null;
		return datapad;
	}

	@Override
	@Nullable
	public SWGObject getEffectiveParent() {
		return isStatesBitmask(CreatureState.RIDING_MOUNT) ? null : getParent();
	}

	@Override
	protected void handleSlotReplacement(SWGObject oldParent, SWGObject obj, List<String> slots) {
		SWGObject inventory = getSlottedObject("inventory");
		for (String slot : slots) {
			SWGObject slotObj = getSlottedObject(slot);
			if (slotObj != null && slotObj != inventory) {
				slotObj.moveToContainer(inventory);
			}
		}
	}
	
	@Override
	protected void onAddedChild(SWGObject child) {
		if (!isPlayer())
			return;
		super.onAddedChild(child);
		Set<SWGObject> children = new HashSet<>(getAwareness().getAware(AwarenessType.SELF));
		getAllChildren(children, child);
		getAwareness().setAware(AwarenessType.SELF, children);
	}
	
	@Override
	protected void onRemovedChild(SWGObject child) {
		if (!isPlayer())
			return;
		super.onRemovedChild(child);
		Set<SWGObject> children = new HashSet<>(getAwareness().getAware(AwarenessType.SELF));
		{
			Set<SWGObject> removed = new HashSet<>();
			getAllChildren(removed, child);
			children.removeAll(removed);
			assert !removed.contains(this);
		}
		getAwareness().setAware(AwarenessType.SELF, children);
	}
	
	public long getLastIncapTime() {
		return lastIncapTime;
	}
	
	public void setLastIncapTime(long lastIncapTime) {
		this.lastIncapTime = lastIncapTime;
	}
	
	private void getAllChildren(Collection<SWGObject> children, SWGObject child) {
		children.add(child);
		for (SWGObject obj : child.getSlottedObjects())
			getAllChildren(children, obj);
		for (SWGObject obj : child.getContainedObjects())
			getAllChildren(children, obj);
	}
	
	public boolean isWithinAwarenessRange(SWGObject target) {
		assert isPlayer();

		Player owner = getOwnerShallow();
		if (owner == null || owner.getPlayerState() == PlayerState.DISCONNECTED || !target.isVisible(this))
			return false;

		SWGObject myParent = getSuperParent();
		SWGObject targetParent = target.getSuperParent();
		if (myParent != null && myParent == targetParent)
			return true;
		if (targetParent == this)
			return true;

		if (isDifferentInstance(target))
			return false;
		
		return switch (target.getBaselineType()) {
			case WAYP -> false;
			case SCLT, BUIO -> true;
			case CREO -> flatDistanceTo(target) <= 200;
			default -> flatDistanceTo(target) <= 400;
		};
	}

	private boolean isDifferentInstance(SWGObject target) {
		InstanceLocation myInstanceLocation = getInstanceLocation();
		InstanceLocation targetInstanceLocation = target.getInstanceLocation();
		boolean differentInstanceType = myInstanceLocation.getInstanceType() != targetInstanceLocation.getInstanceType();
		boolean differentInstanceNumber = myInstanceLocation.getInstanceNumber() != targetInstanceLocation.getInstanceNumber();
		
		return differentInstanceType || differentInstanceNumber;
	}

	@Override
	public boolean isVisible(CreatureObject target) {
		return !isLoggedOutPlayer() && super.isVisible(target);
	}
	
	@Override
	public boolean isInCombat() {
		// CreatureObjects use CreatureState
		
		return isStatesBitmask(CreatureState.COMBAT);
	}
	
	@Override
	public void setInCombat(boolean inCombat) {
		// CreatureObjects use CreatureState
		if (inCombat) {
			setStatesBitmask(CreatureState.COMBAT);
		} else {
			clearStatesBitmask(CreatureState.COMBAT);
		}
	}
	
	private void addEquipment(SWGObject obj) {
		creo6.addEquipment(obj, this);
	}

	private void removeEquipment(SWGObject obj) {
		creo6.removeEquipment(obj, this);
	}
	
	public boolean isContainerOpen(SWGObject obj, String slot) {
		return containersOpen.contains(new Container(obj, slot));
	}

	public boolean openContainer(SWGObject obj, String slot) {
		return containersOpen.add(new Container(obj, slot));
	}

	public void closeAllContainers() {
		boolean empty = containersOpen.isEmpty();
		containersOpen.clear();
	}

	public void closeContainer(SWGObject obj, String slot) {
		containersOpen.remove(new Container(obj, slot));
	}

	public void setTeleportDestination(SWGObject parent, Location location) {
		awareness.setTeleportDestination(parent, location);
	}

	public void addDelta(DeltasMessage delta) {
		synchronized (pendingDeltas) {
			pendingDeltas.add(delta);
		}
	}

	public void clearDeltas() {
		if (pendingDeltas.isEmpty())
			return;
		synchronized (pendingDeltas) {
			pendingDeltas.clear();
		}
	}

	public void sendAndFlushAllDeltas() {
		if (pendingDeltas.isEmpty())
			return;
		synchronized (pendingDeltas) {
			Player owner = getOwner();
			if (owner != null) {
				for (DeltasMessage delta : pendingDeltas) {
					if (awareness.isAware(delta.getObjectId()))
						owner.sendPacket(delta);
				}
			}
			pendingDeltas.clear();
		}
	}

	public boolean addSkill(String skill) {
		boolean added = skills.add(skill);
		if (added)
			skills.sendDeltaMessage(this);
		return added;
	}

	public boolean removeSkill(String skill) {
		boolean removed = skills.remove(skill);
		if (removed)
			skills.sendDeltaMessage(this);
		return removed;
	}

	public boolean hasSkill(String skillName) {
		return skills.contains(skillName);
	}
	
	public Set<String> getSkills() {
		return Collections.unmodifiableSet(skills);
	}

	public Posture getPosture() {
		return creo3.getPosture();
	}
	
	public Race getRace() {
		return race;
	}

	public double getHeight() {
		return creo3.getHeight();
	}

	public int getGuildId() {
		return creo6.getGuildId();
	}
	
	public short getLevel() {
		return creo6.getLevel();
	}
	
	public int getLevelHealthGranted() {
		return creo6.getLevelHealthGranted();
	}

	public CreatureDifficulty getDifficulty() {
		return creo6.getDifficulty();
	}
	
	public PlayerObject getPlayerObject() {
		return (PlayerObject) getSlottedObject("ghost");
	}
	
	public boolean isPlayer() {
		return isSlotPopulated("ghost");
	}
	
	public SWGObject getMissionBag() {
		return getSlottedObject("mission_bag");
	}
	
	public boolean isLoggedInPlayer() {
		return getOwnerShallow() != null && isPlayer();
	}
	
	public boolean isLoggedOutPlayer() {
		return getOwner() == null && isPlayer();
	}	
	
	public TradeSession getTradeSession() {
		return tradeSession;
	}

	public void setTradeSession(TradeSession tradeSession) {
		this.tradeSession = tradeSession;
	}

	public void setPosture(Posture posture) {
		creo3.setPosture(posture);
	}
	
	public void setRace(Race race) {
		this.race = race;
	}
	
	public void inheritMovement(CreatureObject vehicle) {
		setWalkSpeed(vehicle.getRunSpeed() / 2);
		setRunSpeed(vehicle.getRunSpeed());
		setAccelScale(vehicle.getAccelScale());
		setTurnScale(vehicle.getTurnScale());
		setMovementScale(MovementModifierIdentifier.BASE, vehicle.getMovementScale(), true);
	}

	public void resetMovement() {
		creo4.resetMovement();
	}

	public double getAccelPercent() {
		return creo4.getAccelPercent();
	}

	public void setAccelPercent(double accelPercent) {
		creo4.setAccelPercent((float) accelPercent);
	}

	/**
	 * Removes amount from cash first, then bank after. Returns true if the
	 * operation was successful
	 * @param amount the amount to remove
	 * @return TRUE if successfully withdrawn, FALSE otherwise
	 */
	public boolean removeFromCashAndBank(long amount) {
		int bankBalance = getBankBalance();
		int cashBalance = getCashBalance();
		long amountBalance = bankBalance + cashBalance;
		if (amountBalance < amount)
			return false;
		if (cashBalance < amount) {
			setBankBalance(bankBalance - (amount - cashBalance));
			setCashBalance(0);
		} else {
			setCashBalance(cashBalance - amount);
		}
		return true;
	}

	public float getAccelScale() {
		return creo4.getAccelScale();
	}

	public void setAccelScale(double accelScale) {
		creo4.setAccelScale((float) accelScale);
	}

	/**
	 * Removes amount from bank first, then cash after. Returns true if the
	 * operation was successful
	 * @param amount the amount to remove
	 * @return TRUE if successfully withdrawn, FALSE otherwise
	 */
	public boolean removeFromBankAndCash(long amount) {
		int bankBalance = getBankBalance();
		int cashBalance = getCashBalance();
		long amountBalance = bankBalance + cashBalance;
		if (amountBalance < amount)
			return false;
		if (bankBalance < amount) {
			setCashBalance(cashBalance - (amount - bankBalance));
			setBankBalance(0);
		} else {
			setBankBalance(bankBalance - amount);
		}
		return true;
	}
	
	@Override
	public void adjustSkillmod(@NotNull String skillModName, int base, int modifier) {
		creo4.adjustSkillmod(skillModName, base, modifier);
	}

	@Override
	public int getSkillModValue(@NotNull String skillModName) {
		return creo4.getSkillModValue(skillModName);
	}
	
	public Map<String, Integer> getSkillMods() {
		return creo4.getSkillMods();
	}
	
	public float getMovementPercent() {
		return creo4.getMovementPercent();
	}

	/**
	 * Overrides the movementScale of the creature. Useful for temporary effects, such as snares, roots and speed boost buffs.
	 * @param movementPercent 1 for full speed, 0.5 for half etc
	 */
	public void setMovementPercent(double movementPercent) {
		creo4.setMovementPercent((float) movementPercent);
	}

	public float getMovementScale() {
		return creo4.getMovementScale();
	}

	/**
	 * Adds amount to cash balance.
	 * @param amount the amount to add
	 */
	public void addToCash(long amount) {
		setCashBalance(getCashBalance() + amount);
	}

	/**
	 * Adds amount to bank balance.
	 * @param amount the amount to add
	 */
	public void addToBank(long amount) {
		setBankBalance(getBankBalance() + amount);
	}
	
	/**
	 * Increases the base movement speed of the creature. Will be overridden by movementPercent.
	 * This should NOT be used for snares and roots! Use {@link CreatureObject#setMovementPercent(double)} for this.
	 * @param movementModifierIdentifier an identifier that lets us know where the movement modifier comes from
	 * @param movementScale 1 for normal speed, 2 for double, 3 for triple etc
	 * @param fromMount if the source is a mount
	 */
	public void setMovementScale(MovementModifierIdentifier movementModifierIdentifier, float movementScale, boolean fromMount) {
		creo4.setMovementScale(movementModifierIdentifier, movementScale, fromMount);
	}
	
	public void removeMovementScale(MovementModifierIdentifier movementModifierIdentifier) {
		creo4.removeMovementScale(movementModifierIdentifier);
	}

	public long getPerformanceListenTarget() {
		return creo4.getPerformanceListenTarget();
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		creo4.setPerformanceListenTarget(performanceListenTarget);
	}
	
	public float getRunSpeed() {
		return creo4.getRunSpeed();
	}
	
	public void setRunSpeed(double runSpeed) {
		creo4.setRunSpeed((float) runSpeed);
	}
	
	public float getSlopeModAngle() {
		return creo4.getSlopeModAngle();
	}
	
	public void setSlopeModAngle(double slopeModAngle) {
		creo4.setSlopeModAngle((float) slopeModAngle);
	}

	public float getSlopeModPercent() {
		return creo4.getSlopeModPercent();
	}
	
	public void setSlopeModPercent(double slopeModPercent) {
		creo4.setSlopeModPercent((float) slopeModPercent);
	}

	public float getTurnScale() {
		return creo4.getTurnScale();
	}

	public void setTurnScale(double turnScale) {
		creo4.setTurnScale((float) turnScale);
	}

	public float getWalkSpeed() {
		return creo4.getWalkSpeed();
	}

	public void setWalkSpeed(double walkSpeed) {
		creo4.setWalkSpeed((float) walkSpeed);
	}

	public float getWaterModPercent() {
		return creo4.getWaterModPercent();
	}
	
	public void setWaterModPercent(double waterModPercent) {
		creo4.setWaterModPercent((float) waterModPercent);
	}

	@NotNull
	public Set<GroupMissionCriticalObject> getMissionCriticalObjects() {
		return creo4.getMissionCriticalObjects();
	}

	public void setMissionCriticalObjects(@NotNull Set<GroupMissionCriticalObject> missionCriticalObjects) {
		creo4.setMissionCriticalObjects(missionCriticalObjects);
	}

	@NotNull
	public Set<String> getCommands() {
		return creo4.getCommands();
	}

	public void addCommand(@NotNull String command) {
		creo4.addCommand(command);
	}

	public void addCommand(@NotNull List<String> commands) {
		creo4.addCommands(commands);
	}

	public void removeCommand(@NotNull String command) {
		creo4.removeCommand(command);
	}

	public void removeCommands(@NotNull List<String> commands) {
		creo4.removeCommands(commands);
	}

	public boolean hasCommand(@NotNull String command) {
		return creo4.hasCommand(command);
	}

	public void setHeight(double height) {
		creo3.setHeight(height);
	}

	public void setGuildId(int guildId) {
		creo6.setGuildId(guildId);
		sendDelta(6, 9, guildId);
	}
	
	public void setLevel(int level) {
		creo6.setLevel(level);
		sendDelta(6, 2, (short) level);
	}
	
	public void setLevelHealthGranted(int levelHealthGranted) {
		creo6.setLevelHealthGranted(levelHealthGranted);
		sendDelta(6, 3, levelHealthGranted);
	}
	
	public void setDifficulty(CreatureDifficulty difficulty) {
		creo6.setDifficulty(difficulty);
		sendDelta(6, 21, difficulty.getDifficulty());
	}
	
	public String getMoodAnimation() {
		return creo6.getMoodAnimation();
	}

	public void setMoodAnimation(String moodAnimation) {
		creo6.setMoodAnimation(moodAnimation);
		sendDelta(6, 5, moodAnimation, StringType.ASCII);
	}

	public String getAnimation() {
		return creo6.getAnimation();
	}

	public void setAnimation(String animation) {
		creo6.setAnimation(animation);
		sendDelta(6, 4, animation, StringType.ASCII);
	}

	public WeaponObject getEquippedWeapon() {
		return getSlottedObjects().stream()
				.filter(obj -> obj.getObjectId() == creo6.getEquippedWeapon())
				.map(WeaponObject.class::cast)
				.findFirst()
				.orElse(null);
	}

	public void setEquippedWeapon(WeaponObject weapon) {
		WeaponObject equippedWeapon;
		
		if(weapon == null)
			equippedWeapon = (WeaponObject) getSlottedObject("default_weapon");
		else
			equippedWeapon = weapon;
		
		creo6.setEquippedWeapon(equippedWeapon.getObjectId());
		sendDelta(6, 6, equippedWeapon.getObjectId());
	}

	public byte getMoodId() {
		return creo6.getMoodId();
	}

	public void setMoodId(byte moodId) {
		creo6.setMoodId(moodId);
		sendDelta(6, 11, moodId);
	}

	public long getLookAtTargetId() {
		return creo6.getLookAtTargetId();
	}

	public void setLookAtTargetId(long lookAtTargetId) {
		creo6.setLookAtTargetId(lookAtTargetId);
		sendDelta(6, 10, lookAtTargetId);
	}

	public int getPerformanceCounter() {
		return creo6.getPerformanceCounter();
	}

	public void setPerformanceCounter(int performanceCounter) {
		creo6.setPerformanceCounter(performanceCounter);
		sendDelta(6, 12, performanceCounter);
	}

	public int getPerformanceId() {
		return creo6.getPerformanceId();
	}

	public void setPerformanceId(int performanceId) {
		creo6.setPerformanceId(performanceId);
		sendDelta(6, 13, performanceId);
	}

	public String getCostume() {
		return creo6.getCostume();
	}

	public void setCostume(String costume) {
		creo6.setCostume(costume);
		sendDelta(6, 17, costume, StringType.ASCII);
	}

	public long getGroupId() {
		return creo6.getGroupId();
	}

	public void updateGroupInviteData(Player sender, long groupId) {
		creo6.updateGroupInviteData(sender, groupId);
		sendDelta(6, 8, creo6.getInviterData());
	}

	public GroupInviterData getInviterData() {
		return creo6.getInviterData();
	}

	public void setGroupId(long groupId) {
		creo6.setGroupId(groupId);
		sendDelta(6, 7, groupId);
	}

	public byte getFactionRank() {
		return creo3.getFactionRank();
	}

	public void setFactionRank(byte factionRank) {
		creo3.setFactionRank(factionRank);
	}

	public long getOwnerId() {
		return creo3.getOwnerId();
	}

	public void setOwnerId(long ownerId) {
		creo3.setOwnerId(ownerId);
	}

	public int getBattleFatigue() {
		return creo3.getBattleFatigue();
	}

	public void setBattleFatigue(int battleFatigue) {
		creo3.setBattleFatigue(battleFatigue);
	}

	public long getStatesBitmask() {
		return creo3.getStatesBitmask();
	}
	
	public boolean isStatesBitmask(CreatureState ... states) {
		return creo3.isStatesBitmask(states);
	}

	public void setStatesBitmask(CreatureState ... states) {
		creo3.setStatesBitmask(states);
	}

	public void toggleStatesBitmask(CreatureState ... states) {
		creo3.toggleStatesBitmask(states);
	}

	public void clearStatesBitmask(CreatureState ... states) {
		creo3.clearStatesBitmask(states);
	}

	public void clearAllStatesBitmask() {
		creo3.clearAllStatesBitmask();
	}

	public void addBuff(CRC buffCrc, Buff buff) {
		creo6.putBuff(buffCrc, buff);
	}
	
	public Buff removeBuff(CRC buffCrc) {
		return creo6.removeBuff(buffCrc);
	}
	
	public boolean hasBuff(String buffName) {
		CRC crc = new CRC(CRC.getCrc(buffName.toLowerCase(Locale.ENGLISH)));
		return getBuffs().containsKey(crc);
	}

	@NotNull
	public Map<CRC, Buff> getBuffs() {
		return creo6.getBuffs();
	}

	public boolean isVisible() {
		return creo6.isVisible();
	}

	public void setVisible(boolean visible) {
		creo6.setVisible(visible);
		sendDelta(6, 18, visible);
	}

	public boolean isPerforming() {
		return creo6.isPerforming();
	}

	public void setPerforming(boolean performing) {
		creo6.setPerforming(performing);
		sendDelta(6, 20, performing);
	}

	public int getHealth() {
		return creo6.getHealth();
	}
	
	public int getMaxHealth() {
		return creo6.getMaxHealth();
	}
	
	public int getBaseHealth() {
		return baseAttributes.getHealth();
	}
	
	public int getAction() {
		return creo6.getAction();
	}
	
	public int getMaxAction() {
		return creo6.getMaxAction();
	}
	
	public int getBaseAction() {
		return baseAttributes.getAction();
	}
	
	public int getMind() {
		return creo6.getMind();
	}
	
	public int getMaxMind() {
		return creo6.getMaxMind();
	}
	
	public int getBaseMind() {
		return baseAttributes.getMind();
	}

	public void setBaseHealth(int baseHealth) {
		baseAttributes.setHealth(baseHealth);
	}
	
	public void setHealth(int health) {
		creo6.setHealth(health);
	}
	
	public void modifyHealth(int mod) {
		creo6.modifyHealth(mod);
	}
	
	public void setMaxHealth(int maxHealth) {
		int currentHealth = getHealth();
		if (currentHealth > maxHealth) {
			// Ensure it's not possible to have more health than the max health
			setHealth(maxHealth);
		}
		
		creo6.setMaxHealth(maxHealth);
	}
	
	public void setBaseAction(int baseAction) {
		baseAttributes.setAction(baseAction);
	}
	
	public void setAction(int action) {
		creo6.setAction(action);
	}
	
	public void modifyAction(int mod) {
		creo6.modifyAction(mod);
	}
	
	public void setMaxAction(int maxAction) {
		creo6.setMaxAction(maxAction);
	}
	
	public void setMind(int mind) {
		creo6.setMind(mind);
	}
	
	public void modifyMind(int mod) {
		creo6.modifyMind(mod);
	}
	
	public void setMaxMind(int maxMind) {
		creo6.setMaxMind(maxMind);
	}
	
	private void initBaseAttributes() {
		baseAttributes.setHealth(1000);
		baseAttributes.setAction(100);
		baseAttributes.setMind(100);
	}
	
	public Collection<SWGObject> getItemsByTemplate(String slotName, String template) {
		Collection<SWGObject> items = new ArrayList<>(getContainedObjects()); // We also search the creature itself - not just the inventory.
		SWGObject container = getSlottedObject(slotName);
		Collection<SWGObject> candidateChildren;
		
		for(SWGObject candidate : container.getContainedObjects()) {
			
			if(candidate.getTemplate().equals(template)) {
				items.add(candidate);
			} else {
				// check the children. This way we're also searching containers, such as backpacks.
				candidateChildren = candidate.getContainedObjects();
				
				for(SWGObject candidateChild : candidateChildren) {
					if(candidate.getTemplate().equals(template)) {
						items.add(candidateChild);
					}
				}
			}
		}
		return items;
	}
	
	public Map<CreatureObject, Integer> getHateMap(){
		return Collections.unmodifiableMap(hateMap);
	}
	
	public CreatureObject getMostHated(){
		synchronized (hateMap){
			return hateMap.keySet().stream().max(Comparator.comparingInt(hateMap::get)).orElse(null);
		}
	}
	
	public void handleHate(CreatureObject attacker, int hate){
		synchronized (hateMap){
			if(hateMap.containsKey(attacker))
				hateMap.put(attacker, hateMap.get(attacker) + hate);
			else 
				hateMap.put(attacker, hate);
		}
	}
	
	public boolean hasSentDuelRequestToPlayer(CreatureObject player) {
		return sentDuels.contains(player);
	}
	
	public boolean isDuelingPlayer(CreatureObject player) {
		return hasSentDuelRequestToPlayer(player) && player.hasSentDuelRequestToPlayer(this);
	}
	
	public void addPlayerToSentDuels(CreatureObject player) {
		sentDuels.add(player);
	}
	
	public void removePlayerFromSentDuels(CreatureObject player) {
		sentDuels.remove(player);
	}
	
	public boolean isBaselinesSent(SWGObject obj) {
		return awareness.isAware(obj);
	}
	
	@Override
	public void createBaseline1(Player target, @NotNull BaselineBuilder bb) {
		super.createBaseline1(target, bb); // 2 variables
		bb.addObject(baseAttributes); // Attributes player has without any gear on -- 2
		bb.addObject(skills); // 3
		
		bb.incrementOperandCount(2);
	}
	
	@Override
	public void createBaseline3(Player target, @NotNull BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 11 variables - TANO3 (7) + BASE3 (4)
		creo3.createBaseline3(bb);
	}

	public void setHealthWounds(int healthWounds) {
		creo3.setHealthWounds(healthWounds);
	}

	public int getHealthWounds() {
		return creo3.getHealthWounds();
	}

	/**
	 * Using this method is not recommended. Use {@link CreatureObject#getHealthWounds()} instead.
	 * <p> 
	 * The indexes of the wounds are as follows:
	 * 0: Health
	 * 
	 * @return a list of wounds
	 */
	public List<Integer> getWounds() {
		return creo3.getWounds();
	}
	
	@Override
	public void createBaseline4(Player target, @NotNull BaselineBuilder bb) {
		super.createBaseline4(target, bb); // 0 variables
		creo4.createBaseline4(bb);
	}
	
	@Override
	public void createBaseline6(Player target, @NotNull BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 2 variables - TANO6 (0) + BASE6 (2)
		creo6.createBaseline6(target, bb);
	}
	
	@Override
	protected void parseBaseline1(@NotNull NetBuffer buffer) {
		super.parseBaseline1(buffer);
		baseAttributes.decode(buffer);
		skills = SWGSet.getSwgSet(buffer, 1, 3, StringType.ASCII);
	}
	
	@Override
	protected void parseBaseline3(@NotNull NetBuffer buffer) {
		super.parseBaseline3(buffer);
		creo3.parseBaseline3(buffer);
	}
	
	@Override
	protected void parseBaseline4(@NotNull NetBuffer buffer) {
		super.parseBaseline4(buffer);
		creo4.parseBaseline4(buffer);
	}
	
	@Override
	protected void parseBaseline6(@NotNull NetBuffer buffer) {
		super.parseBaseline6(buffer);
		creo6.parseBaseline6(buffer);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		super.saveMongo(data);
		creo3.saveMongo(data.getDocument("base3"));
		creo4.saveMongo(data.getDocument("base4"));
		creo6.saveMongo(data.getDocument("base6"));
		data.putString("race", race.name());
		data.putArray("skills", skills);
		data.putDocument("baseAttributes", baseAttributes);
	}

	@Override
	public void readMongo(MongoData data) {
		super.readMongo(data);
		skills.clear();

		creo3.readMongo(data.getDocument("base3"));
		creo4.readMongo(data.getDocument("base4"));
		creo6.readMongo(data.getDocument("base6"));
		race = Race.valueOf(data.getString("race", race.name()));
		skills.addAll(data.getArray("skills", String.class));
		data.getDocument("baseAttributes", baseAttributes);
	}

	private static class Container {

		private final SWGObject container;
		private final String slot;
		private final int hash;

		public Container(SWGObject container, String slot) {
			this.container = container;
			this.slot = slot;
			this.hash = Objects.hash(container, slot);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			if (o == null || getClass() != o.getClass())
				return false;
			Container container1 = (Container) o;
			return Objects.equals(container, container1.container) && Objects.equals(slot, container1.slot);
		}

		@Override
		public int hashCode() {
			return hash;
		}
	}
	
	@Override
	public AttributeList getAttributeList(CreatureObject viewer) {
		AttributeList attributeList = new AttributeList();
		
		if (creo3.getOwnerId() > 0) {
			applyOwnerAttribute(attributeList);
		}
		
		return attributeList;
	}
	
	private void applyOwnerAttribute(AttributeList attributeList) {
		String displayedOwner;
		SWGObject objectById = ObjectStorageService.ObjectLookup.getObjectById(creo3.getOwnerId());
		
		if (objectById != null ) {
			displayedOwner = objectById.getObjectName();
		} else {
			displayedOwner = "Unknown";
		}
		
		attributeList.putText("@obj_attr_n:owner", displayedOwner);
	}
}
