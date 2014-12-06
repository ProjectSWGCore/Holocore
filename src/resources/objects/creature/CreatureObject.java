package resources.objects.creature;

import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.baselines.Baseline;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.baselines.CREO3;
import network.packets.swg.zone.baselines.CREO6;
import network.packets.swg.zone.baselines.CREO8;
import network.packets.swg.zone.baselines.CREO9;
import resources.Posture;
import resources.Race;
import resources.network.BaselineBuilder;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class CreatureObject extends TangibleObject {
	
	private Posture	posture					= Posture.STANDING;
	private Race	race					= Race.HUMAN;
	private int		attributes				= 0;
	private int		maxAttributes			= 0;
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
	private int		level					= 0;
	private int		levelHealthGranted		= 0;
	private int		totalLevelXp			= 0;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;
	private boolean	beast					= false;
	
	public CreatureObject(long objectId) {
		super(objectId);
	}
	
	public Posture getPosture() {
		return posture;
	}
	
	public Race getRace() {
		return race;
	}
	
	public int getAttributes() {
		return attributes;
	}
	
	public int getMaxAttributes() {
		return maxAttributes;
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
	
	public boolean isBeast() {
		return beast;
	}
	
	public void setPosture(Posture posture) {
		this.posture = posture;
	}
	
	public void setRace(Race race) {
		this.race = race;
	}
	
	public void setAttributes(int attributes) {
		this.attributes = attributes;
	}
	
	public void setMaxAttributes(int maxAttributes) {
		this.maxAttributes = maxAttributes;
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
	
	public void createObject(Player target) {
		sendSceneCreateObject(target);
		
//		BaselineBuilder bb = null;
//		bb = new BaselineBuilder(this, BaselineType.CREO, 3);
//		createBaseline3(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.CREO, 6);
//		createBaseline6(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.CREO, 1);
//		createBaseline1(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.CREO, 4);
//		createBaseline4(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.CREO, 8);
//		createBaseline8(target, bb);
//		bb.sendTo(target);
//		bb = new BaselineBuilder(this, BaselineType.CREO, 9);
//		createBaseline9(target, bb);
//		bb.sendTo(target);
		CREO3 c3 = new CREO3(getTemplate(), getName(), getAppearanceData(), posture.getId(), (float)height, 0);
		CREO6 c6 = new CREO6(getObjectId());
		CREO8 c8 = new CREO8();
		CREO9 c9 = new CREO9();
		c3.setId(getObjectId()); c3.setType(BaselineType.CREO); c3.setNum(3);
		c6.setId(getObjectId()); c6.setType(BaselineType.CREO); c6.setNum(6);
		c8.setId(getObjectId()); c8.setType(BaselineType.CREO); c8.setNum(8);
		c9.setId(getObjectId()); c9.setType(BaselineType.CREO); c9.setNum(9);
		target.sendPacket(new Baseline(getObjectId(), c3));
		target.sendPacket(new Baseline(getObjectId(), c6));
		target.sendPacket(new Baseline(getObjectId(), c8));
		target.sendPacket(new Baseline(getObjectId(), c9));

		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	public void createChildrenObjects(Player target) {
		target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));
		super.createChildrenObjects(target);
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb);
		bb.addShort(5);
		bb.addInt(getBankBalance());
		bb.addInt(getCashBalance());
		bb.addInt(0); // Basic HAM Mod List Size
		bb.addInt(0); // Skills List Size
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addByte(posture.getId());
		bb.addByte(0); // Faction Rank
		bb.addLong(0); // Owner - mainly used for pets and vehicles
		bb.addFloat((float) height);
		bb.addInt(0); // Battle Fatigue
		bb.addLong(0); // States Bitmask
		bb.addInt(0); // Wound HAM List Size
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {
		super.createBaseline4(target, bb);
		bb.addShort(5);
		bb.addFloat((float) accelScale);
		bb.addFloat((float) accelPercent);
		bb.addInt(0); // Encumberance HAM List Size
		bb.addInt(0); // Skill Mod List Size
		bb.addFloat((float) movementScale);
		bb.addFloat((float) movementPercent);
		bb.addInt(0); // Listen to ID
		bb.addFloat((float) runSpeed);
		bb.addFloat((float) slopeModAngle);
		bb.addFloat((float) slopeModPercent);
		bb.addFloat((float) turnScale);
		bb.addFloat((float) walkSpeed);
		bb.addFloat((float) waterModPercent);
		bb.addInt(0); // Group Mission Critical Objects List Size
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addShort(difficulty.getDifficulty());
		bb.addAscii(""); // Current Animation
		bb.addAscii("neutral"); // Animation Mood
		bb.addLong(0); // Weapon ID
		bb.addLong(0); // Group ID
		bb.addLong(0); // Group Inviter ID
		bb.addLong(0); // Some odd counter
		bb.addInt(0); // Guild ID
		bb.addLong(0); // Target ID
		bb.addByte(0); // Mood ID
		bb.addInt(0); // Performance Start Time
		bb.addInt(0); // Performance ID
		bb.addInt(0); // Current HAM List Size
		bb.addInt(0); // Max HAM List Size
		bb.addInt(0); // Equipment List Size
		bb.addAscii(""); // Appearance?
		bb.addBoolean(false);
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
		bb.addShort(0);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
		bb.addShort(0);
	}
	
}
