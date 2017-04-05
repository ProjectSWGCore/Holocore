/***********************************************************************************
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
package resources.objects.creature;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.HologramColour;
import resources.Posture;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.PvpStatus;
import resources.Race;
import resources.collections.SWGList;
import resources.collections.SWGSet;
import resources.common.CRC;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.network.NetBufferStream;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.persistable.SWGObjectFactory;
import resources.player.Player;
import services.group.GroupInviterData;
import utilities.Encoder.StringType;

public class CreatureObject extends TangibleObject {
	
	private transient long lastReserveOperation		= 0;
	
	private final CreatureObjectClientServerNP	creo4 = new CreatureObjectClientServerNP();
	private final CreatureObjectSharedNP		creo6 = new CreatureObjectSharedNP();
	
	private Posture	posture					= Posture.UPRIGHT;
	private Race	race					= Race.HUMAN_MALE;
	private double	height					= 0;
	private int		cashBalance				= 0;
	private int		bankBalance				= 0;
	private long	reserveBalance			= 0; // Galactic Reserve - capped at 3 billion
	private byte 	factionRank				= 0;
	private long 	ownerId					= 0;
	private int 	battleFatigue			= 0;
	private long 	statesBitmask			= 0;
	private long	lastTransform			= 0;
	
	private SWGSet<String> skills					= new SWGSet<String>(1, 3, StringType.ASCII);
	
	private SWGList<Integer> baseAttributes			= new SWGList<Integer>(1, 2);
	
	private List<CreatureObject> sentDuels			= new ArrayList<>();
	
	public CreatureObject(long objectId) {
		super(objectId, BaselineType.CREO);
		initBaseAttributes();
		setPrefLoadRange(200);
	}
	
	@Override
	public void addObject(SWGObject obj) {
		super.addObject(obj);
		if (obj.getSlotArrangement() != -1 && !(obj instanceof PlayerObject)) {
			addEquipment(obj);
		}
	}
	
	@Override
	public void removeObject(SWGObject obj) {
		super.removeObject(obj);
		removeEquipment(obj);
	}
	
	@Override
	protected void handleSlotReplacement(SWGObject oldParent, SWGObject obj, int arrangement) {
		SWGObject inventory = getSlottedObject("inventory");
		for (String slot : obj.getArrangement().get(arrangement-4)) {
			SWGObject slotObj = getSlottedObject(slot);
			if (slotObj != null) {
				slotObj.moveToContainer(inventory);
			}
		}
	}

	private void addEquipment(SWGObject obj) {
		creo6.addEquipment(obj, this);
	}

	private void removeEquipment(SWGObject obj) {
		creo6.removeEquipment(obj, this);
	}
	
	public void addAppearanceItem(SWGObject obj) {
		creo6.addAppearanceItem(obj, this);
	}

	public void removeAppearanceItem(SWGObject obj) {
		creo6.removeAppearanceItem(obj, this);
	}
	
	public void addSkill(String ... skillList) {
		synchronized (skills) {
			boolean delta = false;
			for (String skillName : skillList)
				delta |= skills.add(skillName);
			if (delta)
				skills.sendDeltaMessage(this);
		}
	}
	
	public boolean hasSkill(String skillName) {
		return skills.contains(skillName);
	}
	
	public Set<String> getSkills() {
		return Collections.unmodifiableSet(skills);
	}
	
	public int getCashBalance() {
		return cashBalance;
	}

	public int getBankBalance() {
		return bankBalance;
	}
	
	public long getReserveBalance() {
		return reserveBalance;
	}

	public Posture getPosture() {
		return posture;
	}
	
	public Race getRace() {
		return race;
	}
	
	public double getMovementScale() {
		return creo4.getMovementScale();
	}
	
	public double getMovementPercent() {
		return creo4.getMovementPercent();
	}
	
	public double getWalkSpeed() {
		return creo4.getWalkSpeed();
	}
	
	public double getRunSpeed() {
		return creo4.getRunSpeed();
	}
	
	public double getAccelScale() {
		return creo4.getAccelScale();
	}
	
	public double getAccelPercent() {
		return creo4.getAccelPercent();
	}
	
	public double getTurnScale() {
		return creo4.getTurnScale();
	}
	
	public double getSlopeModAngle() {
		return creo4.getSlopeModAngle();
	}
	
	public double getSlopeModPercent() {
		return creo4.getSlopeModPercent();
	}
	
	public double getWaterModPercent() {
		return creo4.getWaterModPercent();
	}
	
	public double getHeight() {
		return height;
	}
	
	public long getPerformanceListenTarget() {
		return creo4.getPerformanceListenTarget();
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
	
	public int getTotalLevelXp() {
		return creo4.getTotalLevelXp();
	}
	
	public CreatureDifficulty getDifficulty() {
		return creo6.getDifficulty();
	}
	
	public double getTimeSinceLastTransform() {
		return (System.nanoTime()-lastTransform)/1E6;
	}
	
	public PlayerObject getPlayerObject() {
		return (PlayerObject) getSlottedObject("ghost");
	}
	
	public boolean isPlayer() {
		return getSlottedObject("ghost") != null;
	}
	
	public boolean isLoggedInPlayer() {
		return getOwner() != null && isPlayer();
	}
	
	public boolean isLoggedOutPlayer() {
		return getOwner() == null && isPlayer();
	}
	
	public void setPosture(Posture posture) {
		this.posture = posture;
		sendDelta(3, 13, posture.getId());
		if (isPlayer())
			sendObserversAndSelf(new PostureUpdate(getObjectId(), posture));
	}
	
	public void setRace(Race race) {
		this.race = race;
	}
	
	public void setCashBalance(long cashBalance) {
		if (cashBalance < 0)
			cashBalance = 0;
		if (cashBalance > 2E9) { // 2 billion cap
			long leftover = cashBalance - (long)2E9;
			cashBalance = (long) 2E9;
			long bank = bankBalance + leftover;
			long reserve = reserveBalance;
			leftover = bank - (long) 2E9;
			if (leftover > 0) {
				bank = (long)2E9;
				reserve += leftover;
			}
			this.cashBalance = (int) cashBalance;
			sendDelta(1, 1, (int) cashBalance);
			setBankBalance(bank);
			setReserveBalance(reserve);
		} else {
			this.cashBalance = (int) cashBalance;
			sendDelta(1, 1, (int) cashBalance);
		}
	}

	public void setBankBalance(long bankBalance) {
		if (bankBalance < 0)
			bankBalance = 0;
		if (bankBalance > 2E9) { // 2 billion cap
			long leftover = bankBalance - (long)2E9;
			bankBalance = (long) 2E9;
			long cash = cashBalance + leftover;
			long reserve = reserveBalance;
			leftover = cash - (long) 2E9;
			if (leftover > 0) {
				cash = (long)2E9;
				reserve += leftover;
			}
			this.bankBalance = (int) bankBalance;
			sendDelta(1, 0, (int) bankBalance);
			setCashBalance(cash);
			setReserveBalance(reserve);
		} else {
			this.bankBalance = (int) bankBalance;
			sendDelta(1, 0, (int) bankBalance);
		}
	}
	
	public void setReserveBalance(long reserveBalance) {
		if (reserveBalance < 0)
			reserveBalance = 0;
		else if (reserveBalance > 3E9)
			reserveBalance = (long) 3E9; // 3 billion cap
		this.reserveBalance = reserveBalance;
	}
	
	public boolean canPerformGalacticReserveTransaction() {
		return (System.nanoTime() - lastReserveOperation) / 1E9 >= 15*60;
	}
	
	public void updateLastGalacticReserveTime() {
		lastReserveOperation = System.nanoTime();
	}
	
	public void setMovementScale(double movementScale) {
		creo4.setMovementScale(movementScale);
		sendDelta(4, 5, movementScale);
	}
	
	public void setMovementPercent(double movementPercent) {
		creo4.setMovementPercent(movementPercent);
		sendDelta(4, 4, movementPercent);
	}
	
	public void setWalkSpeed(double walkSpeed) {
		creo4.setWalkSpeed(walkSpeed);
		sendDelta(4, 11, walkSpeed);
	}
	
	public void setRunSpeed(double runSpeed) {
		creo4.setRunSpeed(runSpeed);
		sendDelta(4, 7, runSpeed);
	}
	
	public void setAccelScale(double accelScale) {
		creo4.setAccelScale(accelScale);
		sendDelta(4, 1, accelScale);
	}
	
	public void setAccelPercent(double accelPercent) {
		creo4.setAccelPercent(accelPercent);
		sendDelta(4, 0, accelPercent);
	}
	
	public void setTurnScale(double turnScale) {
		creo4.setTurnScale(turnScale);
		sendDelta(4, 10, turnScale);
	}
	
	public void setSlopeModAngle(double slopeModAngle) {
		creo4.setSlopeModAngle(slopeModAngle);
		sendDelta(4, 8, slopeModAngle);
	}
	
	public void setSlopeModPercent(double slopeModPercent) {
		creo4.setSlopeModPercent(slopeModPercent);
		sendDelta(4, 9, slopeModPercent);
	}
	
	public void setWaterModPercent(double waterModPercent) {
		creo4.setWaterModPercent(waterModPercent);
		sendDelta(4, 12, waterModPercent);
	}
	
	public void setHeight(double height) {
		this.height = height;
		sendDelta(3, 16, height);
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		creo4.setPerformanceListenTarget(performanceListenTarget);
		sendDelta(4, 6, performanceListenTarget);
	}
	
	public void setGuildId(int guildId) {
		creo6.setGuildId(guildId);
		sendDelta(6, 15, guildId);
	}
	
	public void setLevel(int level) {
		creo6.setLevel(level);
		sendDelta(6, 8, (short) level);
	}
	
	public void setLevelHealthGranted(int levelHealthGranted) {
		creo6.setLevelHealthGranted(levelHealthGranted);
		sendDelta(6, 9, levelHealthGranted);
	}
	
	public void setTotalLevelXp(int totalLevelXp) {
		creo4.setTotalLevelXp(totalLevelXp);
		sendDelta(4, 15, totalLevelXp);
	}
	
	public void setDifficulty(CreatureDifficulty difficulty) {
		creo6.setDifficulty(difficulty);
		sendDelta(6, 26, difficulty.getDifficulty());
	}
	
	public void updateLastTransformTime() {
		lastTransform = System.nanoTime();
	}
	
	public String getMoodAnimation() {
		return creo6.getMoodAnimation();
	}

	public void setMoodAnimation(String moodAnimation) {
		creo6.setMoodAnimation(moodAnimation);
		sendDelta(6, 11, moodAnimation, StringType.ASCII);
	}

	public boolean isBeast() {
		return creo6.isBeast();
	}

	public void setBeast(boolean beast) {
		creo6.setBeast(beast);
		sendDelta(6, 31, beast);
	}

	public String getAnimation() {
		return creo6.getAnimation();
	}

	public void setAnimation(String animation) {
		creo6.setAnimation(animation);
		sendDelta(6, 10, animation, StringType.ASCII);
	}

	public WeaponObject getEquippedWeapon() {
		return creo6.getEquippedWeapon();
	}

	public void setEquippedWeapon(WeaponObject weapon) {
		WeaponObject equippedWeapon;
		
		if(weapon == null)
			equippedWeapon = (WeaponObject) getSlottedObject("default_weapon");
		else
			equippedWeapon = weapon;
		
		creo6.setEquippedWeapon(equippedWeapon);
		sendDelta(6, 12, equippedWeapon.getObjectId());
	}

	public byte getMoodId() {
		return creo6.getMoodId();
	}

	public void setMoodId(byte moodId) {
		creo6.setMoodId(moodId);
		sendDelta(6, 18, moodId);
	}

	public long getLookAtTargetId() {
		return creo6.getLookAtTargetId();
	}

	public void setLookAtTargetId(long lookAtTargetId) {
		creo6.setLookAtTargetId(lookAtTargetId);
		sendDelta(6, 16, lookAtTargetId);
	}

	public long getIntendedTargetId() {
		return creo6.getIntendedTargetId();
	}

	public void setIntendedTargetId(long intendedTargetId) {
		creo6.setIntendedTargetId(intendedTargetId);
		sendDelta(6, 17, intendedTargetId);
	}

	public int getPerformanceCounter() {
		return creo6.getPerformanceCounter();
	}

	public void setPerformanceCounter(int performanceCounter) {
		creo6.setPerformanceCounter(performanceCounter);
		sendDelta(6, 19, performanceCounter);
	}

	public int getPerformanceId() {
		return creo6.getPerformanceId();
	}

	public void setPerformanceId(int performanceId) {
		creo6.setPerformanceId(performanceId);
		sendDelta(6, 20, performanceId);
	}

	public String getCostume() {
		return creo6.getCostume();
	}

	public void setCostume(String costume) {
		creo6.setCostume(costume);
		sendDelta(6, 24, costume, StringType.ASCII);
	}

	public long getGroupId() {
		return creo6.getGroupId();
	}

	public void updateGroupInviteData(Player sender, long groupId, String name) {
		creo6.updateGroupInviteData(sender, groupId, name);
		sendDelta(6, 14, creo6.getInviterData());
	}

	public GroupInviterData getInviterData() {
		return creo6.getInviterData();
	}

	public void setGroupId(long groupId) {
		creo6.setGroupId(groupId);
		sendDelta(6, 13, groupId);
	}

	public byte getFactionRank() {
		return factionRank;
	}

	public void setFactionRank(byte factionRank) {
		this.factionRank = factionRank;
		sendDelta(3, 14, factionRank);
	}

	public long getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(long ownerId) {
		this.ownerId = ownerId;
		sendDelta(3, 15, ownerId);
	}

	public int getBattleFatigue() {
		return battleFatigue;
	}

	public void setBattleFatigue(int battleFatigue) {
		this.battleFatigue = battleFatigue;
		sendDelta(3, 17, battleFatigue);
	}

	public long getStatesBitmask() {
		return statesBitmask;
	}

	public void setStatesBitmask(CreatureState ... states) {
		for (CreatureState state : states)
			statesBitmask |= state.getBitmask();
		sendDelta(3, 18, statesBitmask);
	}

	public void toggleStatesBitmask(CreatureState ... states) {
		for (CreatureState state : states)
			statesBitmask ^= state.getBitmask();
		sendDelta(3, 18, statesBitmask);
	}

	public void clearStatesBitmask(CreatureState ... states) {
		for (CreatureState state : states)
			statesBitmask &= ~state.getBitmask();
		sendDelta(3, 18, statesBitmask);
	}

	public void clearAllStatesBitmask() {
		statesBitmask = 0;
		sendDelta(3, 18, statesBitmask);
	}

	public synchronized void adjustSkillmod(String skillModName, int base, int modifier) {
		creo4.adjustSkillmod(skillModName, base, modifier, this);
	}
	
	public int getSkillModValue(String skillModName) {
		return creo4.getSkillModValue(skillModName);
	}
	
	public void addBuff(Buff buff) {
		creo6.putBuff(buff, this);
	}
	
	public Buff removeBuff(CRC buffCrc) {
		return creo6.removeBuff(buffCrc, this);
	}
	
	public boolean hasBuff(String buffName) {
		return getBuffEntries(buff -> CRC.getCrc(buffName.toLowerCase(Locale.ENGLISH)) == buff.getCrc()).count() > 0;
	}
	
	public Stream<Buff> getBuffEntries(Predicate<Buff> predicate) {
		return creo6.getBuffEntries(predicate);
	}
	
	public void adjustBuffStackCount(CRC buffCrc, int adjustment) {
		creo6.adjustBuffStackCount(buffCrc, adjustment, this);
	}
	
	public void setBuffDuration(CRC buffCrc, int playTime, int duration) {
		creo6.setBuffDuration(buffCrc, playTime, duration, this);
	}
	
	public boolean isVisible() {
		return creo6.isVisible();
	}

	public void setVisible(boolean visible) {
		creo6.setVisible(visible);
		sendDelta(6, 25, visible);
	}

	public boolean isPerforming() {
		return creo6.isPerforming();
	}

	public void setPerforming(boolean performing) {
		creo6.setPerforming(performing);
		sendDelta(6, 27, performing);
	}
	
	public void setHologramColour(HologramColour hologramColour) {
		creo6.setHologramColour(hologramColour);
		sendDelta(6, 29, hologramColour.getValue());
	}

	public boolean isShownOnRadar() {
		return creo6.isShownOnRadar();
	}

	public void setShownOnRadar(boolean shownOnRadar) {
		creo6.setShownOnRadar(shownOnRadar);
		sendDelta(6, 30, shownOnRadar);
	}

	public int getHealth() {
		return creo6.getHealth();
	}
	
	public int getMaxHealth() {
		return creo6.getMaxHealth();
	}
	
	public int getBaseHealth() {
		synchronized (baseAttributes) {
			return baseAttributes.get(0);
		}
	}
	
	public int getAction() {
		return creo6.getAction();
	}
	
	public int getMaxAction() {
		return creo6.getMaxAction();
	}
	
	public int getBaseAction() {
		synchronized (baseAttributes) {
			return baseAttributes.get(2);
		}
	}
	
	public int getMind() {
		return creo6.getMind();
	}
	
	public int getMaxMind() {
		return creo6.getMaxMind();
	}
	
	public int getBaseMind() {
		synchronized (baseAttributes) {
			return baseAttributes.get(4);
		}
	}

	public void addAbility(String ... abilities) {
		creo4.addAbility(this, abilities);
	}
	
	public void removeAbility(String abilityName) {
		creo4.removeAbility(abilityName);
	}
	
	public boolean hasAbility(String abilityName) {
		return creo4.hasAbility(abilityName);
	}
	
	public Set<String> getAbilityNames() {
		return creo4.getAbilityNames();
	}

	public void setBaseHealth(int baseHealth) {
		synchronized(baseAttributes) {
			baseAttributes.set(0, baseHealth);
			baseAttributes.sendDeltaMessage(this);
		}
	}
	
	public void setHealth(int health) {
		creo6.setHealth(health, this);
	}
	
	public int modifyHealth(int mod) {
		return creo6.modifyHealth(mod, this);
	}
	
	public void setMaxHealth(int maxHealth) {
		creo6.setMaxHealth(maxHealth, this);
	}
	
	public void setBaseAction(int baseAction) {
		synchronized(baseAttributes) {
			baseAttributes.set(2, baseAction);
			baseAttributes.sendDeltaMessage(this);
		}
	}
	
	public void setAction(int action) {
		creo6.setAction(action, this);
	}
	
	public int modifyAction(int mod) {
		return creo6.modifyAction(mod, this);
	}
	
	public void setMaxAction(int maxAction) {
		creo6.setMaxAction(maxAction, this);
	}
	
	public void setMind(int mind) {
		creo6.setMind(mind, this);
	}
	
	public int modifyMind(int mod) {
		return creo6.modifyMind(mod, this);
	}
	
	public void setMaxMind(int maxMind) {
		creo6.setMaxMind(maxMind, this);
	}
	
	private void initBaseAttributes() {
		baseAttributes.add(0, 1000); // Health
		baseAttributes.add(1, 0);
		baseAttributes.add(2, 300); // Action
		baseAttributes.add(3, 0);
		baseAttributes.add(4, 300); // Mind
		baseAttributes.add(5, 0);
		baseAttributes.clearDeltaQueue();
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

	public boolean isAttackable(CreatureObject otherObject) {
		Posture otherPosture = otherObject.getPosture();
		
		return isEnemy(otherObject) && otherPosture != Posture.INCAPACITATED && otherPosture != Posture.DEAD;
	}
	
	public boolean hasSentDuelRequestToPlayer(CreatureObject player) {
		return sentDuels.contains(player);
	}
	
	public boolean isDuelingPlayer(CreatureObject player) {
		return sentDuelRequestToPlayer(player) && player.sentDuelRequestToPlayer(this);
	}
	
	public void addPlayerToSentDuels(CreatureObject player) {
		sentDuels.add(player);
	}
	
	public void removePlayerFromSentDuels(CreatureObject player) {
		sentDuels.remove(player);
	}
	
	@Override
	public boolean isEnemy(TangibleObject otherObject) {
		boolean tangibleEnemy = super.isEnemy(otherObject);
		
		if (tangibleEnemy || !(otherObject instanceof CreatureObject)) {
			return tangibleEnemy;
		}
		
		if (isDuelingPlayer((CreatureObject)otherObject))
			return true;
		
		return isPlayer() && ((CreatureObject) otherObject).isPlayer()
				&& getPvpFaction() != PvpFaction.NEUTRAL
				&& otherObject.getPvpFaction() != PvpFaction.NEUTRAL
				&& getPvpFaction() != otherObject.getPvpFaction()
				&& getPvpStatus() == PvpStatus.SPECIALFORCES
				&& otherObject.getPvpStatus() == PvpStatus.SPECIALFORCES;
	}
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode() * 20 + race.toString().hashCode();
	}
	
	public void sendBaselines(Player target) {
		boolean targetSelf = getOwner() == target;
		
		if (targetSelf)
			target.sendPacket(createBaseline1(target));
		
		target.sendPacket(createBaseline3(target));
		
		if (targetSelf)
			target.sendPacket(createBaseline4(target));
		
		target.sendPacket(createBaseline6(target));
		
		if (targetSelf) {
			target.sendPacket(createBaseline8(target));
			target.sendPacket(createBaseline9(target));
		}
	}
	
	protected void sendFinalBaselinePackets(Player target) {
		super.sendFinalBaselinePackets(target);
		
		if (isGenerated()) {
			target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));
			
			Set<PvpFlag> flags = PvpFlag.getFlags(getPvpFlags());
			target.sendPacket(new UpdatePvpStatusMessage(getPvpFaction(), getObjectId(), flags.toArray(new PvpFlag[flags.size()])));
		}
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb); // 0 variables
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		bb.addInt(bankBalance); // 0
		bb.addInt(cashBalance); // 1
		bb.addObject(baseAttributes); // Attributes player has without any gear on -- 2
		bb.addObject(skills); // 3
		
		bb.incrementOperandCount(4);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 13 variables - TANO3 (9) + BASE3 (4)
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		bb.addByte(posture.getId()); // 13
		bb.addByte(factionRank); // 14
		bb.addLong(ownerId); // 15
		bb.addFloat((float) height); // 16
		bb.addInt(battleFatigue); // 17
		bb.addLong(statesBitmask); // 18
		
		bb.incrementOperandCount(6);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {
		super.createBaseline4(target, bb); // 0 variables
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		creo4.createBaseline4(target, bb);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 8 variables - TANO6 (6) + BASE6 (2)
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		creo6.createBaseline6(target, bb);
	}
	
	protected void parseBaseline1(NetBuffer buffer) {
		super.parseBaseline1(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		bankBalance = buffer.getInt();
		cashBalance = buffer.getInt();
		baseAttributes = buffer.getSwgList(1, 2, Integer.class);
		skills = buffer.getSwgSet(1, 3, StringType.ASCII);
	}
	
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		posture = Posture.getFromId(buffer.getByte());
		factionRank = buffer.getByte();
		ownerId = buffer.getLong();
		height = buffer.getFloat();
		battleFatigue = buffer.getInt();
		statesBitmask = buffer.getLong();
	}
	
	protected void parseBaseline4(NetBuffer buffer) {
		super.parseBaseline4(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		creo4.parseBaseline4(buffer);
	}
	
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		creo6.parseBaseline6(buffer);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		super.save(stream);
		stream.addByte(1);
		creo4.save(stream);
		creo6.save(stream);
		stream.addAscii(posture.name());
		stream.addAscii(race.name());
		stream.addFloat((float) height);
		stream.addInt(battleFatigue);
		stream.addInt(cashBalance);
		stream.addInt(bankBalance);
		stream.addLong(reserveBalance);
		stream.addLong(ownerId);
		stream.addLong(statesBitmask);
		stream.addByte(factionRank);
		synchronized (skills) {
			stream.addList(skills, (s) -> stream.addAscii(s));
		}
		synchronized (baseAttributes) {
			stream.addList(baseAttributes, (s) -> stream.addInt(s));
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		super.read(stream);
		switch(stream.getByte()) {
			case 0: readVersion0(stream); break;
			case 1: readVersion1(stream); break;
		}
		
	}
	
	private void readVersion0(NetBufferStream stream) {
		creo4.read(stream);
		creo6.read(stream);
		posture = Posture.valueOf(stream.getAscii());
		race = Race.valueOf(stream.getAscii());
		height = stream.getFloat();
		battleFatigue = stream.getInt();
		cashBalance = stream.getInt();
		bankBalance = stream.getInt();
		reserveBalance = stream.getLong();
		ownerId = stream.getLong();
		statesBitmask = stream.getLong();
		factionRank = stream.getByte();
		if (stream.getBoolean()) {
			SWGObject defaultWeapon = (WeaponObject) SWGObjectFactory.create(stream);
			defaultWeapon.moveToContainer(this);	// The weapon will be moved into the default_weapon slot
		}
		stream.getList((i) -> skills.add(stream.getAscii()));
		stream.getList((i) -> baseAttributes.set(i, stream.getInt()));
	}
	
	private void readVersion1(NetBufferStream stream) {
		creo4.read(stream);
		creo6.read(stream);
		posture = Posture.valueOf(stream.getAscii());
		race = Race.valueOf(stream.getAscii());
		height = stream.getFloat();
		battleFatigue = stream.getInt();
		cashBalance = stream.getInt();
		bankBalance = stream.getInt();
		reserveBalance = stream.getLong();
		ownerId = stream.getLong();
		statesBitmask = stream.getLong();
		factionRank = stream.getByte();
		stream.getList((i) -> skills.add(stream.getAscii()));
		stream.getList((i) -> baseAttributes.set(i, stream.getInt()));
	}
	
}
