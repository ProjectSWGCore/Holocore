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
import java.util.Set;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.Map;

import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.HologramColour;
import resources.Posture;
import resources.PvpFlag;
import resources.Race;
import resources.SkillMod;
import resources.collections.SWGList;
import resources.collections.SWGMap;
import resources.collections.SWGSet;
import resources.common.CRC;
import resources.encodables.player.Equipment;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.OptionFlag;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import services.group.GroupInviterData;
import utilities.Encoder.StringType;

public class CreatureObject extends TangibleObject {
	
	private static final long serialVersionUID = 3L;
	
	private transient GroupInviterData inviterData	= new GroupInviterData(0, null, "", 0);
	private transient long lastReserveOperation		= 0;
	private transient long groupId					= 0;
	
	private Posture	posture					= Posture.UPRIGHT;
	private Race	race					= Race.HUMAN; 
	private double	movementScale			= 1;
	private double	movementPercent			= 1;
	private double	walkSpeed				= 1.549;
	private double	runSpeed				= 7.3;
	private double	accelScale				= 1;
	private double	accelPercent			= 1;
	private double	turnScale				= 1;
	private double	slopeModAngle			= 1;
	private double	slopeModPercent			= 1; 
	private double	waterModPercent			= 0.75;
	private double	height					= 0;
	private long	performanceListenTarget	= 0;
	private int		guildId					= 0;
	private short	level					= 1;
	private int		levelHealthGranted		= 0;
	private int		totalLevelXp			= 0;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;
	private int		cashBalance				= 0;
	private int		bankBalance				= 0;
	private long	reserveBalance			= 0; // Galactic Reserve - capped at 3 billion
	private String	moodAnimation			= "neutral";
	private String	animation				= "";
	private long	equippedWeaponId		= 0;
	private byte	moodId					= 0;
	private long 	lookAtTargetId			= 0;
	private long 	intendedTargetId		= 0;
	private int 	performanceCounter		= 0;
	private int 	performanceId			= 0;
	private String 	costume					= "";
	private boolean visible					= true;
	private boolean performing				= false;
	private boolean shownOnRadar			= true;
	private boolean beast					= false;
	private byte 	factionRank				= 0;
	private long 	ownerId					= 0;
	private int 	battleFatigue			= 0;
	private long 	statesBitmask			= 0;
	private String	currentCity				= "";
	private long	lastTransform			= 0;
	private HologramColour hologramColour = HologramColour.DEFAULT;
	
	private SWGSet<String>		missionCriticalObjs			= new SWGSet<>(4, 13);
	
	private SWGList<Integer>	baseAttributes	= new SWGList<Integer>(1, 2);
	private SWGList<String>		skills			= new SWGList<String>(1, 3, StringType.ASCII); // SWGSet
	private SWGList<Integer>	hamEncumbList	= new SWGList<Integer>(4, 2);
	private SWGList<Integer>	attributes		= new SWGList<Integer>(6, 21);
	private SWGList<Integer>	maxAttributes	= new SWGList<Integer>(6, 22);
	private SWGList<Equipment>	equipmentList 	= new SWGList<Equipment>(6, 23);
	private SWGList<Equipment>	appearanceList 	= new SWGList<Equipment>(6, 33);
	
	private SWGMap<String, SkillMod> 	skillMods			= new SWGMap<>(4, 3, StringType.ASCII); // TODO: SkillMod structure
	private SWGMap<String, Integer>	abilities				= new SWGMap<>(4, 14, StringType.ASCII);
	private SWGMap<CRC, Buff>	buffs				= new SWGMap<>(6, 26);

	public CreatureObject(long objectId) {
		super(objectId, BaselineType.CREO);
		initMaxAttributes();
		initCurrentAttributes();
		initBaseAttributes();
		setOptionFlags(OptionFlag.HAM_BAR);
	}

	private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
		ois.defaultReadObject();
		// Transient Variables
		inviterData = new GroupInviterData(0, null, "", 0);
		lastReserveOperation = 0;
		groupId = 0;
	}

	public void removeEquipment(SWGObject obj) {
		synchronized (equipmentList) {
			for (Equipment equipment : equipmentList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					equipmentList.remove(equipment);
					equipmentList.sendDeltaMessage(this);
					break;
				}
			}
		}
	}

	public void addEquipment(SWGObject obj) {
		synchronized(equipmentList) {
			if (obj instanceof WeaponObject)
				equipmentList.add(new Equipment((WeaponObject) obj));
			else
				equipmentList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
			equipmentList.sendDeltaMessage(this);
		}
	}
	
	public void addAppearanceItem(SWGObject obj) {
		synchronized(appearanceList) {
			appearanceList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
			appearanceList.sendDeltaMessage(this);
		}
	}

	public void removeAppearanceItem(SWGObject obj) {
		synchronized (appearanceList) {
			for (Equipment equipment : appearanceList) {
				if (equipment.getObjectId() == obj.getObjectId()) {
					appearanceList.remove(equipment);
					appearanceList.sendDeltaMessage(this);
				}
			}
		}
	}

	public SWGList<Equipment> getEquipmentList() {
		return equipmentList;
	}
	
	public SWGList<Equipment> getAppearanceList() {
		return appearanceList;
	}
	
	public SWGList<String> getSkills() {
		return skills;
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
		return movementScale;
	}
	
	public double getMovementPercent() {
		return movementPercent;
	}
	
	public double getWalkSpeed() {
		return walkSpeed;
	}
	
	public double getRunSpeed() {
		return runSpeed;
	}
	
	public double getAccelScale() {
		return accelScale;
	}
	
	public double getAccelPercent() {
		return accelPercent;
	}
	
	public double getTurnScale() {
		return turnScale;
	}
	
	public double getSlopeModAngle() {
		return slopeModAngle;
	}
	
	public double getSlopeModPercent() {
		return slopeModPercent;
	}
	
	public double getWaterModPercent() {
		return waterModPercent;
	}
	
	public double getHeight() {
		return height;
	}
	
	public long getPerformanceListenTarget() {
		return performanceListenTarget;
	}
	
	public int getGuildId() {
		return guildId;
	}
	
	public int getLevel() {
		return level;
	}
	
	public int getLevelHealthGranted() {
		return levelHealthGranted;
	}
	
	public int getTotalLevelXp() {
		return totalLevelXp;
	}
	
	public CreatureDifficulty getDifficulty() {
		return difficulty;
	}
	
	public String getCurrentCity() {
		return currentCity;
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
		this.movementScale = movementScale;
		sendDelta(4, 5, movementScale);
	}
	
	public void setMovementPercent(double movementPercent) {
		this.movementPercent = movementPercent;
		sendDelta(4, 4, movementPercent);
	}
	
	public void setWalkSpeed(double walkSpeed) {
		this.walkSpeed = walkSpeed;
		sendDelta(4, 11, walkSpeed);
	}
	
	public void setRunSpeed(double runSpeed) {
		this.runSpeed = runSpeed;
		sendDelta(4, 7, runSpeed);
	}
	
	public void setAccelScale(double accelScale) {
		this.accelScale = accelScale;
		sendDelta(4, 1, accelScale);
	}
	
	public void setAccelPercent(double accelPercent) {
		this.accelPercent = accelPercent;
		sendDelta(4, 0, accelPercent);
	}
	
	public void setTurnScale(double turnScale) {
		this.turnScale = turnScale;
		sendDelta(4, 10, turnScale);
	}
	
	public void setSlopeModAngle(double slopeModAngle) {
		this.slopeModAngle = slopeModAngle;
		sendDelta(4, 8, slopeModAngle);
	}
	
	public void setSlopeModPercent(double slopeModPercent) {
		this.slopeModPercent = slopeModPercent;
		sendDelta(4, 9, slopeModPercent);
	}
	
	public void setWaterModPercent(double waterModPercent) {
		this.waterModPercent = waterModPercent;
		sendDelta(4, 12, waterModPercent);
	}
	
	public void setHeight(double height) {
		this.height = height;
		sendDelta(3, 16, height);
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		this.performanceListenTarget = performanceListenTarget;
		sendDelta(4, 6, performanceListenTarget);
	}
	
	public void setGuildId(int guildId) {
		this.guildId = guildId;
		sendDelta(6, 15, guildId);
	}
	
	public void setLevel(short level) {
		this.level = level;
		sendDelta(6, 8, level);
	}
	
	public void setLevelHealthGranted(int levelHealthGranted) {
		this.levelHealthGranted = levelHealthGranted;
		sendDelta(6, 9, levelHealthGranted);
	}
	
	public void setTotalLevelXp(int totalLevelXp) {
		this.totalLevelXp = totalLevelXp;
		sendDelta(4, 15, totalLevelXp);
	}
	
	public void setDifficulty(CreatureDifficulty difficulty) {
		this.difficulty = difficulty;
		sendDelta(6, 26, difficulty.getDifficulty());
	}
	
	public void setCurrentCity(String currentCity) {
		this.currentCity = currentCity;
	}
	
	public void updateLastTransformTime() {
		lastTransform = System.nanoTime();
	}
	
	public String getMoodAnimation() {
		return moodAnimation;
	}

	public void setMoodAnimation(String moodAnimation) {
		this.moodAnimation = moodAnimation;
		sendDelta(6, 11, moodAnimation, StringType.ASCII);
	}

	public boolean isBeast() {
		return beast;
	}

	public void setBeast(boolean beast) {
		this.beast = beast;
		sendDelta(6, 31, beast);
	}

	public String getAnimation() {
		return animation;
	}

	public void setAnimation(String animation) {
		this.animation = animation;
		sendDelta(6, 10, animation, StringType.ASCII);
	}

	public long getEquippedWeaponId() {
		return equippedWeaponId;
	}

	public void setEquippedWeaponId(long equippedWeaponId) {
		this.equippedWeaponId = equippedWeaponId;
		sendDelta(6, 12, equippedWeaponId);
	}

	public byte getMoodId() {
		return moodId;
	}

	public void setMoodId(byte moodId) {
		this.moodId = moodId;
		sendDelta(6, 18, moodId);
	}

	public long getLookAtTargetId() {
		return lookAtTargetId;
	}

	public void setLookAtTargetId(long lookAtTargetId) {
		this.lookAtTargetId = lookAtTargetId;
		sendDelta(6, 16, lookAtTargetId);
	}

	public long getIntendedTargetId() {
		return intendedTargetId;
	}

	public void setIntendedTargetId(long intendedTargetId) {
		this.intendedTargetId = intendedTargetId;
		sendDelta(6, 17, intendedTargetId);
	}

	public int getPerformanceCounter() {
		return performanceCounter;
	}

	public void setPerformanceCounter(int performanceCounter) {
		this.performanceCounter = performanceCounter;
		sendDelta(6, 19, performanceCounter);
	}

	public int getPerformanceId() {
		return performanceId;
	}

	public void setPerformanceId(int performanceId) {
		this.performanceId = performanceId;
		sendDelta(6, 20, performanceId);
	}

	public String getCostume() {
		return costume;
	}

	public void setCostume(String costume) {
		this.costume = costume;
		sendDelta(6, 24, costume, StringType.ASCII);
	}

	public long getGroupId() {
		return groupId;
	}

	public void updateGroupInviteData(Player sender, long groupId, String name) {
		inviterData.setName(name);
		inviterData.setSender(sender);
		inviterData.setId(groupId);
		inviterData.incrementCounter();

		sendDelta(6, 14, inviterData);
	}

	public GroupInviterData getInviterData() {
		return inviterData;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
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
		SkillMod skillMod = skillMods.get(skillModName);
		
		if(skillMod == null) {
			// They didn't have this SkillMod already.
			// Therefore, we send a full delta.
			skillMods.put(skillModName, new SkillMod(base, modifier));
		} else {
			// They already had this skillmod.
			// All we need to do is adjust the base and the modifier and send an update from the SWGMap
			skillMod.adjustBase(base);
			skillMod.adjustModifier(modifier);
			
			// If this skillmod now has a total value of 0
			if(skillMod.getValue() == 0) {
				// Then remove it from the map
				skillMods.remove(skillModName);
				skillMods.sendDeltaMessage(this);
				return;
			}
		}
		skillMods.update(skillModName, this);
	}
	
	public int getSkillModValue(String skillModName) {
		SkillMod skillMod = skillMods.get(skillModName);
		return skillMod != null ? skillMod.getValue() : 0;
	}
	
	public void addBuff(CRC buffCrc, Buff buff) {
		if(!buffs.containsKey(buffCrc)) {
			buffs.put(buffCrc, buff);
			buffs.sendDeltaMessage(this);
			System.out.println("added buff");
		}
	}
	
	public void removeBuff(CRC buffCrc) {
		// If a value was associated with the key, then send a delta.
		if(buffs.containsKey(buffCrc)) {
			buffs.remove(buffCrc);
			buffs.sendDeltaMessage(this);
			System.out.println("removed buff");
		}
	}
	
	public Buff getBuffByCrc(CRC buffCrc) {
		return buffs.get(buffCrc);
	}
	
	public void adjustBuffStackCount(CRC buffCrc, int adjustment) {
		Buff buff = buffs.get(buffCrc);
		buff.adjustStackCount(adjustment);	// Adjust the stack count
		// TODO reset time remaining?
		buffs.update(buffCrc, this);	// Send deltas for this key.
	}
	
	/**
	 * @return a copy of the buffs map. Removing and adding entries in this
	 * map will not affect the internal {@code SWGMap}. Do not edit the
	 * {@code Buff} values in the belief that deltas will be sent because
	 * they won't - this is incorrect usage.
	 */
	public Map<CRC, Buff> getBuffs() {
		return new HashMap<>(buffs);
	}
	
	public boolean isVisible() {
		return visible;
	}

	public void setVisible(boolean visible) {
		this.visible = visible;
		sendDelta(6, 25, visible);
	}

	public boolean isPerforming() {
		return performing;
	}

	public void setPerforming(boolean performing) {
		this.performing = performing;
		sendDelta(6, 27, performing);
	}
	
	public void setHologramColour(HologramColour hologramColour) {
		this.hologramColour = hologramColour;
		sendDelta(6, 29, hologramColour.getValue());
	}

	public boolean isShownOnRadar() {
		return shownOnRadar;
	}

	public void setShownOnRadar(boolean shownOnRadar) {
		this.shownOnRadar = shownOnRadar;
		sendDelta(6, 30, shownOnRadar);
	}

	public int getHealth() {
		return attributes.get(0);
	}
	
	public int getMaxHealth() {
		return maxAttributes.get(0);
	}
	
	public int getBaseHealth() {
		return baseAttributes.get(0);
	}
	
	public int getAction() {
		return attributes.get(2);
	}
	
	public int getMaxAction() {
		return attributes.get(2);
	}
	
	public int getBaseAction() {
		return attributes.get(2);
	}

	public void addAbility(String abilityName){ abilities.put(abilityName, 1); }//TODO: Figure out what the integer value should be for each ability

	public void removeAbility(String abilityName) { abilities.remove(abilityName); }

	public boolean hasAbility(String abilityName) { return abilities.get(abilityName) != null; }
	
	public void setHealth(int health) {
		synchronized(attributes) {
			attributes.set(0, health);
			attributes.sendDeltaMessage(this);
		}
	}
	
	public void setMaxHealth(int maxHealth) {
		synchronized(maxAttributes) {
			maxAttributes.set(0, maxHealth);
			maxAttributes.sendDeltaMessage(this);
		}
	}
	
	public void setAction(int action) {
		synchronized(attributes) {
			attributes.set(2, action);
			attributes.sendDeltaMessage(this);
		}
	}
	
	public void setMaxAction(int maxAction) {
		synchronized(maxAttributes) {
			maxAttributes.set(2, maxAction);
			maxAttributes.sendDeltaMessage(this);
		}
	}
	
	private void initMaxAttributes() {
		maxAttributes.add(0, 1000); // Health
		maxAttributes.add(1, 0);
		maxAttributes.add(2, 300); // Action
		maxAttributes.add(3, 0);
		maxAttributes.add(4, 300); // ??
		maxAttributes.add(5, 0);
		maxAttributes.clearDeltaQueue();
	}
	
	private void initCurrentAttributes() {
		attributes.add(0, 1000); // Health
		attributes.add(1, 0);
		attributes.add(2, 300); // Action
		attributes.add(3, 0);
		attributes.add(4, 300); // ??
		attributes.add(5, 0);
		attributes.clearDeltaQueue();
	}
	
	private void initBaseAttributes() {
		baseAttributes.add(0, 1000); // Health
		baseAttributes.add(1, 0);
		baseAttributes.add(2, 300); // Action
		baseAttributes.add(3, 0);
		baseAttributes.add(4, 300); // ??
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
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return (super.hashCode() * 7 + posture.getId()) * 13 + race.toString().hashCode();
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

	@Override
	public void createObject(Player target) {
		super.createObject(target);

		target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));

		if (target != getOwner()) {
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
		bb.addFloat((float) accelPercent); // 0
		bb.addFloat((float) accelScale); // 1
		bb.addObject(hamEncumbList); // Rename to bonusAttributes? 2
		bb.addObject(skillMods); // 3
		bb.addFloat((float) movementPercent); // 4
		bb.addFloat((float) movementScale); // 5
		bb.addLong(performanceListenTarget); // 6
		bb.addFloat((float) runSpeed); // 7
		bb.addFloat((float) slopeModAngle); // 8
		bb.addFloat((float) slopeModPercent); // 9
		bb.addFloat((float) turnScale); // 10
		bb.addFloat((float) walkSpeed); // 11
		bb.addFloat((float) waterModPercent); // 12
		bb.addObject(missionCriticalObjs); // Group Missions? 13
		bb.addObject(abilities); // 14
		bb.addInt(totalLevelXp); // 15
		
		bb.incrementOperandCount(16);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 8 variables - TANO6 (6) + BASE6 (2)
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		bb.addShort(level); // 8
		bb.addInt(levelHealthGranted); // 9
		bb.addAscii(animation); // 10
		bb.addAscii(moodAnimation); // 11
		bb.addLong(equippedWeaponId); // 12
		bb.addLong(groupId); // 13
		bb.addObject(inviterData); // TODO: Check structure -- 14
		bb.addInt(guildId); // 15
		bb.addLong(lookAtTargetId); // 16
		bb.addLong(intendedTargetId); // 17
		bb.addByte(moodId); // 18
		bb.addInt(performanceCounter); // 19
		bb.addInt(performanceId); // 20
		bb.addObject(attributes); // 21
		bb.addObject(maxAttributes); // 22
		bb.addObject(equipmentList); // 23
		bb.addAscii(costume); // 24
		bb.addBoolean(visible); // 25
		bb.addObject(buffs); // 26
		bb.addBoolean(performing); // 27
		bb.addByte(difficulty.getDifficulty()); // 28
		bb.addInt((hologramColour == null) ? -1 : hologramColour.getValue()); // Hologram Color -- 29
		bb.addBoolean(shownOnRadar); // 30
		bb.addBoolean(beast); // 31
		bb.addByte(0); // forceShowHam? -- 32
		bb.addObject(appearanceList); // 33
		bb.addLong(0); // decoy? -- 34
		
		bb.incrementOperandCount(27);
	}
	
	protected void parseBaseline1(NetBuffer buffer) {
		super.parseBaseline1(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		bankBalance = buffer.getInt();
		cashBalance = buffer.getInt();
		baseAttributes = buffer.getSwgList(1, 2, Integer.class);
		skills = buffer.getSwgList(1, 2, StringType.ASCII);
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
		accelPercent = buffer.getFloat();
		accelScale = buffer.getFloat();
		hamEncumbList = buffer.getSwgList(4, 2, Integer.class);
		skillMods = buffer.getSwgMap(4, 3, StringType.ASCII, SkillMod.class);
		movementPercent = buffer.getFloat();
		movementScale = buffer.getFloat();
		performanceListenTarget = buffer.getLong();
		runSpeed = buffer.getFloat();
		slopeModAngle = buffer.getFloat();
		slopeModPercent = buffer.getFloat();
		turnScale = buffer.getFloat();
		walkSpeed = buffer.getFloat();
		waterModPercent = buffer.getFloat();
		missionCriticalObjs = buffer.getSwgSet(4, 13, StringType.ASCII);
		abilities = buffer.getSwgMap(4, 14, StringType.ASCII, Integer.class);
		totalLevelXp = buffer.getInt();
	}
	
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		if (getStringId().toString().equals("@obj_n:unknown_object"))
			return;
		level = buffer.getShort();
		levelHealthGranted = buffer.getInt();
		animation = buffer.getAscii();
		moodAnimation = buffer.getAscii();
		equippedWeaponId = buffer.getLong();
		groupId = buffer.getLong();
		inviterData = buffer.getEncodable(GroupInviterData.class);
		guildId = buffer.getInt();
		lookAtTargetId = buffer.getLong();
		intendedTargetId = buffer.getLong();
		moodId = buffer.getByte();
		performanceCounter = buffer.getInt();
		performanceId = buffer.getInt();
		attributes = buffer.getSwgList(6, 21, Integer.class);
		maxAttributes = buffer.getSwgList(6, 22, Integer.class);
		equipmentList = buffer.getSwgList(6, 23, Equipment.class);
		costume = buffer.getAscii();
		visible = buffer.getBoolean();
		buffs = buffer.getSwgMap(6, 26, CRC.class, Buff.class);
		performing = buffer.getBoolean();
		difficulty = CreatureDifficulty.getForDifficulty(buffer.getByte());
		hologramColour = HologramColour.getForValue(buffer.getInt());
		shownOnRadar = buffer.getBoolean();
		beast = buffer.getBoolean();
		buffer.getBoolean();
		appearanceList = buffer.getSwgList(6, 33, Equipment.class);
		buffer.getLong();
	}
	
}
