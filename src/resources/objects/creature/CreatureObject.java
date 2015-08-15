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

import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.object_controller.PostureUpdate;
import resources.HologramColour;
import resources.Posture;
import resources.Race;
import resources.collections.SWGList;
import resources.collections.SWGMap;
import resources.encodables.player.Equipment;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.OptionFlag;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import utilities.Encoder.StringType;

public class CreatureObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private transient long lastReserveOperation	= 0;
	
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
	private long 	groupId					= 0;
	private byte 	factionRank				= 0;
	private long 	ownerId					= 0;
	private int 	battleFatigue			= 0;
	private long 	statesBitmask			= 0;
	private String	currentCity				= "";
	private HologramColour hologramColour = HologramColour.DEFAULT;
	
	private SWGList<Integer>	baseAttributes	= new SWGList<Integer>(BaselineType.CREO, 1, 2);
	private SWGList<String>		skills			= new SWGList<String>(BaselineType.CREO, 1, 3, StringType.ASCII); // SWGSet
	private SWGList<Integer>	hamEncumbList	= new SWGList<Integer>(BaselineType.CREO, 4, 2);
	private SWGList<Integer>	attributes		= new SWGList<Integer>(BaselineType.CREO, 6, 21);
	private SWGList<Integer>	maxAttributes	= new SWGList<Integer>(BaselineType.CREO, 6, 22);
	private SWGList<Equipment>	equipmentList 	= new SWGList<Equipment>(BaselineType.CREO, 6, 23);
	private SWGList<Equipment>	appearanceList 	= new SWGList<Equipment>(BaselineType.CREO, 6, 33);
	
	private SWGMap<String, Long> 	skillMods			= new SWGMap<>(BaselineType.CREO, 4, 3, StringType.ASCII); // TODO: SkillMod structure
	private SWGMap<Long, Long>		missionCriticalObjs	= new SWGMap<>(BaselineType.CREO, 4, 13);
	private SWGMap<String, Integer>	abilities			= new SWGMap<>(BaselineType.CREO, 4, 14, StringType.ASCII);
	private SWGMap<Integer, Long>	buffs				= new SWGMap<>(BaselineType.CREO, 6, 26); // TODO: Buff structure


	public CreatureObject(long objectId) {
		super(objectId, BaselineType.CREO);
		initMaxAttributes();
		initCurrentAttributes();
		initBaseAttributes();
		setOptionFlags(OptionFlag.HAM_BAR);
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
	
	public PlayerObject getPlayerObject() {
		return (PlayerObject) (hasSlot("ghost") ? getSlottedObject("ghost") : null);
	}
	
	public void setPosture(Posture posture) {
		this.posture = posture;
		sendDelta(3, 13, posture.getId());
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
			sendDelta(1, 1, cashBalance);
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
			sendDelta(1, 0, bankBalance);
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
	
	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}
	
	@Override
	public int hashCode() {
		return (super.hashCode() * 7 + posture.getId()) * 13 + race.toString().hashCode();
	}
	
	public void sendBaselines(Player target) {
		BaselineBuilder bb = null;
		
		if (getOwner() == target) {
			bb = new BaselineBuilder(this, BaselineType.CREO, 1);
			createBaseline1(target, bb);
			bb.sendTo(target);
		}
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		if (getOwner() == target) {
			bb = new BaselineBuilder(this, BaselineType.CREO, 4);
			createBaseline4(target, bb);
			bb.sendTo(target);
		}
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);
		
		if (getOwner() == target) {
			bb = new BaselineBuilder(this, BaselineType.CREO, 8);
			createBaseline8(target, bb);
			bb.sendTo(target);
			
			bb = new BaselineBuilder(this, BaselineType.CREO, 9);
			createBaseline9(target, bb);
			bb.sendTo(target);
		}
	}

	@Override
	public void createObject(Player target) {
		super.createObject(target);

		target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));

		if (getOwner() != null && target != getOwner()) {
			target.sendPacket(new UpdatePvpStatusMessage(UpdatePvpStatusMessage.PLAYER, 0, getObjectId()));
		}
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb); // 0 variables
		bb.addInt(bankBalance); // 0
		bb.addInt(cashBalance); // 1
		bb.addObject(baseAttributes); // Attributes player has without any gear on -- 2
		bb.addObject(skills); // 3
		
		bb.incrementOperandCount(4);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 13 variables - TANO3 (9) + BASE3 (4)
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
		bb.addShort(level); // 8
		bb.addInt(levelHealthGranted); // 9
		bb.addAscii(animation); // 10
		bb.addAscii(moodAnimation); // 11
		bb.addLong(equippedWeaponId); // 12
		bb.addLong(groupId); // 13
		bb.addLong(0); // Group Inviter ID -- 14
			bb.addAscii(""); // Group Inviter Name
			bb.addLong(0); // Invite counter
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
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
	}
	
	public void sendDelta(int type, int update, Object value) {
		sendDelta(BaselineType.CREO, type, update, value);
	}
	
	public void sendDelta(int type, int update, Object value, StringType strType) {
		sendDelta(BaselineType.CREO, type, update, value, strType);
	}
}
