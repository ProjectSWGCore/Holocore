package resources.objects.creature;

import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.Posture;
import resources.Race;
import resources.collections.SWGList;
import resources.collections.SWGMap;
import resources.encodables.player.Equipment;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.objects.player.PlayerObject;
import resources.objects.tangible.TangibleObject;
import resources.objects.weapon.WeaponObject;
import resources.player.Player;
import utilities.Encoder.StringType;

public class CreatureObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private Posture	posture					= Posture.STANDING;
	private Race	race					= Race.HUMAN;
	private int		unmodifiedMaxAtributes	= 0;
	private int		attributeBonus			= 0;
	private int		shockWounds				= 0;
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
	private int		performanceType			= 0;
	private int		performanceStartTime	= 0;
	private long	performanceListenTarget	= 0;
	private int		guildId					= 0;
	private byte	rank					= 0;
	private int		level					= 1;
	private int		levelHealthGranted		= 0;
	private int		totalLevelXp			= 0;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;
	private boolean	beast					= false;
	private int		cashBalance				= 0;
	private int		bankBalance				= 0;
	
	private SWGList<Integer>	baseAttributes	= new SWGList<Integer>(BaselineType.CREO, 1, 2);
	private SWGList<String>		skills			= new SWGList<String>(BaselineType.CREO, 1, 3, false, StringType.ASCII);
	private SWGList<Integer>	hamEncumbList	= new SWGList<Integer>(BaselineType.CREO, 4, 10, true);
	private SWGList<Integer>	attributes		= new SWGList<Integer>(BaselineType.CREO, 6, 21, true);
	private SWGList<Integer>	maxAttributes	= new SWGList<Integer>(BaselineType.CREO, 6, 22, true);
	private SWGList<Equipment>	equipmentList 	= new SWGList<Equipment>(BaselineType.CREO, 6, 23);
	private SWGList<Equipment>	appearanceList 	= new SWGList<Equipment>(BaselineType.CREO, 6, 33);
	
	private SWGMap<String, Long> 	skillMods			= new SWGMap<>(BaselineType.CREO, 4, 11, false, StringType.ASCII); // TODO: SkillMod structure
	private SWGMap<Long, Long>		missionCriticalObjs	= new SWGMap<>(BaselineType.CREO, 4, 21);
	private SWGMap<String, Integer>	abilities			= new SWGMap<>(BaselineType.CREO, 4, 22, false, StringType.ASCII);
	private SWGMap<Integer, Long>	buffs				= new SWGMap<>(BaselineType.CREO, 6, 26); // TODO: Buff structure
	
	public CreatureObject(long objectId) {
		super(objectId);
		initMaxAttributes();
		initCurrentAttributes();
		initBaseAttributes();
	}
	
	public void addEquipment(SWGObject obj) {
		synchronized(equipmentList) {
			if (obj instanceof WeaponObject)
				equipmentList.add(new Equipment((WeaponObject) obj));
			else
				equipmentList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
		}
	}
	
	public void addAppearanceItem(SWGObject obj) {
		synchronized(appearanceList) {
			appearanceList.add(new Equipment(obj.getObjectId(), obj.getTemplate()));
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

	public Posture getPosture() {
		return posture;
	}
	
	public Race getRace() {
		return race;
	}
	
	public int getUnmodifiedMaxAtributes() {
		return unmodifiedMaxAtributes;
	}
	
	public int getAttributeBonus() {
		return attributeBonus;
	}
	
	public int getShockWounds() {
		return shockWounds;
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
	
	public int getPerformanceType() {
		return performanceType;
	}
	
	public int getPerformanceStartTime() {
		return performanceStartTime;
	}
	
	public long getPerformanceListenTarget() {
		return performanceListenTarget;
	}
	
	public int getGuildId() {
		return guildId;
	}
	
	public byte getRank() {
		return rank;
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
	
	public PlayerObject getPlayerObject() {
		return (PlayerObject) (hasSlot("ghost") ? getSlottedObject("ghost") : null);
	}
	
	public boolean isBeast() {
		return beast;
	}
	
	public void setPosture(Posture posture) {
		this.posture = posture;
		sendDelta(3, 14, posture);
	}
	
	public void setRace(Race race) {
		this.race = race;
	}
	
	public void setCashBalance(int cashBalance) {
		this.cashBalance = cashBalance;
		sendDelta(1, 0, cashBalance);
	}

	public void setBankBalance(int bankBalance) {
		this.bankBalance = bankBalance;
		sendDelta(1, 1, bankBalance);
	}
	
	public void setUnmodifiedMaxAtributes(int unmodifiedMaxAtributes) {
		this.unmodifiedMaxAtributes = unmodifiedMaxAtributes;
	}
	
	public void setAttributeBonus(int attributeBonus) {
		this.attributeBonus = attributeBonus;
	}
	
	public void setShockWounds(int shockWounds) {
		this.shockWounds = shockWounds;
	}
	
	public void setMovementScale(double movementScale) {
		this.movementScale = movementScale;
	}
	
	public void setMovementPercent(double movementPercent) {
		this.movementPercent = movementPercent;
	}
	
	public void setWalkSpeed(double walkSpeed) {
		this.walkSpeed = walkSpeed;
	}
	
	public void setRunSpeed(double runSpeed) {
		this.runSpeed = runSpeed;
	}
	
	public void setAccelScale(double accelScale) {
		this.accelScale = accelScale;
	}
	
	public void setAccelPercent(double accelPercent) {
		this.accelPercent = accelPercent;
	}
	
	public void setTurnScale(double turnScale) {
		this.turnScale = turnScale;
	}
	
	public void setSlopeModAngle(double slopeModAngle) {
		this.slopeModAngle = slopeModAngle;
	}
	
	public void setSlopeModPercent(double slopeModPercent) {
		this.slopeModPercent = slopeModPercent;
	}
	
	public void setWaterModPercent(double waterModPercent) {
		this.waterModPercent = waterModPercent;
	}
	
	public void setHeight(double height) {
		this.height = height;
		sendDelta(3, 17, height);
	}
	
	public void setPerformanceType(int performanceType) {
		this.performanceType = performanceType;
	}
	
	public void setPerformanceStartTime(int performanceStartTime) {
		this.performanceStartTime = performanceStartTime;
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		this.performanceListenTarget = performanceListenTarget;
	}
	
	public void setGuildId(int guildId) {
		this.guildId = guildId;
	}
	
	public void setRank(byte rank) {
		this.rank = rank;
	}
	
	public void setLevel(int level) {
		this.level = level;
	}
	
	public void setLevelHealthGranted(int levelHealthGranted) {
		this.levelHealthGranted = levelHealthGranted;
	}
	
	public void setTotalLevelXp(int totalLevelXp) {
		this.totalLevelXp = totalLevelXp;
	}
	
	public void setDifficulty(CreatureDifficulty difficulty) {
		this.difficulty = difficulty;
	}
	
	public void setBeast(boolean beast) {
		this.beast = beast;
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
	
	public void createObject(Player target) {
		sendSceneCreateObject(target);
		
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
		
		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	protected void createChildrenObjects(Player target) {
		target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));
		if (target != getOwner()) target.sendPacket(new UpdatePvpStatusMessage(UpdatePvpStatusMessage.PLAYER, 0, getObjectId())); // TODO: Change this when adding non-players
		super.createChildrenObjects(target);
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb); // 0 variables
		bb.addInt(cashBalance); // 0
		bb.addInt(bankBalance); // 1
		bb.addObject(baseAttributes); // Attributes player has without any gear on -- 2
		bb.addObject(skills); // 3
		
		bb.incremeantOperandCount(4);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 13 variables - TANO3 (9) + BASE3 (4)
		bb.addByte(posture.getId()); // 13
		bb.addByte(0); // Faction Rank -- 14
		bb.addLong(0); // Owner - mainly used for pets and vehicles -- 15
		bb.addFloat((float) height); // 16
		bb.addInt(0); // Battle Fatigue -- 17
		bb.addLong(0); // States Bitmask -- 18
		
		bb.incremeantOperandCount(6);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {
		super.createBaseline4(target, bb); // 0 variables
		bb.addFloat((float) accelScale); // 8
		bb.addFloat((float) accelPercent); // 9
		bb.addObject(hamEncumbList); // 10
		bb.addObject(skillMods); // 11
		bb.addFloat((float) movementScale); // 12
		bb.addFloat((float) movementPercent); // 13
		bb.addLong(0); // Listen to ID - 14
		bb.addFloat((float) runSpeed); // 15
		bb.addFloat((float) slopeModAngle); // 16
		bb.addFloat((float) slopeModPercent); // 17
		bb.addFloat((float) turnScale); // 18
		bb.addFloat((float) walkSpeed); // 19
		bb.addFloat((float) waterModPercent); // 20
		bb.addObject(missionCriticalObjs); // 21
		bb.addObject(abilities); // 22
		bb.addInt(0); // XP Display Counter (remaining experience to next level up, updates the experience bar on client) - 23
		
		bb.incremeantOperandCount(16);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 8 variables - TANO6 (6) + BASE6 (2)
		bb.addShort(level); // 8
		bb.addInt(0); // Granted Health -- 9
		bb.addAscii(""); // Current Animation -- 10
		bb.addAscii("neutral"); // Animation Mood -- 11
		bb.addLong(0); // Weapon ID -- 12
		bb.addLong(0); // Group ID -- 13
		bb.addLong(0); // Group Inviter ID -- 14
			bb.addAscii(""); // Group Inviter Name
			bb.addLong(0); // Invite counter
		bb.addInt(0); // Guild ID -- 15
		bb.addLong(0); // Look-at Target ID -- 16
		bb.addLong(0); // Intended ID -- 17
		bb.addByte(0); // Mood ID -- 18
		bb.addInt(0); // Performance Counter -- 19
		bb.addInt(0); // Performance ID -- 20
		bb.addObject(attributes); // 21
		bb.addObject(maxAttributes); // 22
		bb.addObject(equipmentList); // 23
		bb.addAscii(""); // Appearance (costume) -- 24
		bb.addBoolean(true); // Visible -- 25
		bb.addObject(buffs); // 26
		bb.addBoolean(false); // Is Performing -- 27
		bb.addByte(difficulty.getDifficulty()); // 28
		bb.addInt(-1); // Hologram Color -- 29
		bb.addBoolean(true); // Visible On Radar -- 30
		bb.addBoolean(false); // Is Pet -- 31
		bb.addByte(0); // Unknown -- 32
		bb.addObject(appearanceList); // 33
		bb.addLong(0); // unknown -- 34
		
		bb.incremeantOperandCount(27);
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
}
