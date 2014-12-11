package resources.objects.creature;

import network.packets.swg.zone.SceneEndBaselines;
import network.packets.swg.zone.UpdatePostureMessage;
import network.packets.swg.zone.baselines.Baseline.BaselineType;
import network.packets.swg.zone.baselines.CREO6;
import resources.Posture;
import resources.Race;
import resources.network.BaselineBuilder;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class CreatureObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
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
	private int		level					= 90;
	private int		levelHealthGranted		= 0;
	private int		totalLevelXp			= 0;
	private CreatureDifficulty	difficulty	= CreatureDifficulty.NORMAL;
	private boolean	beast					= false;
	
	public CreatureObject(long objectId) {
		super(objectId);
		setStfFile("species"); // TODO: Remove when automatic stf is in
		setStfKey(race.getSpecies()); // TODO: remove when automatic stf is in
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
		
		BaselineBuilder bb = null;

		bb = new BaselineBuilder(this, BaselineType.CREO, 1); // ZONED IN! ( crash when pressing escape )
		createBaseline1(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 3);
		createBaseline3(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 4); // ZONED IN! ( crash when pressing escape )
		createBaseline4(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 6);
		createBaseline6(target, bb);
		bb.sendTo(target);

		bb = new BaselineBuilder(this, BaselineType.CREO, 8);
		createBaseline8(target, bb);
		bb.sendTo(target);
		
		bb = new BaselineBuilder(this, BaselineType.CREO, 9);
		createBaseline9(target, bb);
		bb.sendTo(target);

		
		createChildrenObjects(target);
		target.sendPacket(new SceneEndBaselines(getObjectId()));
	}
	
	public void createChildrenObjects(Player target) {
		target.sendPacket(new UpdatePostureMessage(posture.getId(), getObjectId()));
		super.createChildrenObjects(target);
	}
	
	public void createBaseline1(Player target, BaselineBuilder bb) {
		super.createBaseline1(target, bb);
		bb.addInt(getBankBalance());
		bb.addInt(getCashBalance());
		bb.addInt(6); // Base HAM Mod List Size (List, Integer)
			bb.addInt(0); // update counter
			bb.addInt(1000); // Max Health
			bb.addInt(0); // ??
			bb.addInt(300); // Max Action
			bb.addInt(0); // ??
			bb.addInt(300); // Max Mind
			bb.addInt(0); // ??
		bb.addInt(1); // Skills List Size (List, Integer)
			bb.addInt(0); // update counter
			bb.addAscii(race.getSpecies());
		
		bb.incremeantOperandCount(4);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addByte(posture.getId());
		bb.addByte(0); // Faction Rank
		bb.addLong(0); // Owner - mainly used for pets and vehicles
		bb.addFloat((float) height);
		bb.addInt(0); // Battle Fatigue
		bb.addLong(0); // States Bitmask
		
		bb.incremeantOperandCount(6);
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {
		// TODO: Double check structure with an NGE packet
		super.createBaseline4(target, bb);
		bb.addFloat((float) accelScale);
		bb.addFloat((float) accelPercent);
		bb.addInt(0); // Encumberance HAM List Size (List, Integer)
			bb.addInt(0);
		bb.addInt(0); // Skill Mod List Size (Map, k = String v= SkillMod structure)
			bb.addInt(0);
		bb.addFloat((float) movementScale);
		bb.addFloat((float) movementPercent);
		bb.addLong(0); // Listen to ID
		bb.addFloat((float) runSpeed);
		bb.addFloat((float) slopeModAngle);
		bb.addFloat((float) slopeModPercent);
		bb.addFloat((float) turnScale);
		bb.addFloat((float) walkSpeed);
		bb.addFloat((float) waterModPercent);
		bb.addInt(0); // Mission Critical Objects list size (Map, k = long v = long)
			bb.addInt(0);
		bb.addInt(0); // abilities list size (Map, k = string v = integer)
			bb.addInt(0);
		bb.addInt(0); // XP Display Counter (remaining experience to next level up, updates the experience bar on client)
		
		bb.incremeantOperandCount(16);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		
		bb.addShort(level);
		bb.addInt(0); // Granted Health
		bb.addAscii(""); // Current Animation
		bb.addAscii("neutral"); // Animation Mood
		bb.addLong(0); // Weapon ID
		bb.addLong(0); // Group ID
		bb.addLong(0); // Group Inviter ID
			bb.addAscii(""); // Group Inviter Name
			bb.addLong(0); // Invite counter
		bb.addInt(0); // Guild ID
		bb.addLong(0); // Look-at Target ID
		bb.addLong(0); // Intended ID
		bb.addByte(0); // Mood ID
		bb.addInt(0); // Performance Counter
		bb.addInt(0); // Performance ID
		bb.addInt(6); // Attributes List Size (List, Integer)
			bb.addInt(5);
			bb.addInt(2363); // Health
			bb.addInt(0);
			bb.addInt(3050); // Action
			bb.addInt(0);
			bb.addInt(1000); // ?? it's 300 on new characters
			bb.addInt(0);
		bb.addInt(6); // Max Attributes List Size (List, Integer)
			bb.addInt(5);
			bb.addInt(2363); // Health
			bb.addInt(0);
			bb.addInt(3050); // Action
			bb.addInt(0);
			bb.addInt(1000); // ?? it's 300 on new characters
			bb.addInt(0);
		bb.addInt(0); // Equipment List (List, Equipment structure)
			bb.addInt(0);
		bb.addAscii(""); // Appearance (costume)
		bb.addBoolean(true); // Visible
		bb.addInt(0); // Buff list (Map, k = Integer v = Buff structure)
			bb.addInt(0);
		bb.addBoolean(false); // Is Performing
		bb.addByte(difficulty.getDifficulty());
		bb.addInt(-1); // Hologram Color
		bb.addBoolean(true); // Visible On Radar
		bb.addBoolean(false); // Is Pet
		bb.addByte(0); // Unknown
		bb.addInt(0); // Appearance Equipment List Size (List, Equipment structure)
			bb.addInt(0);
		bb.addLong(0); // unknown
		
		bb.incremeantOperandCount(27);
	}
	
	public void createBaseline8(Player target, BaselineBuilder bb) {
		super.createBaseline8(target, bb);
	}
	
	public void createBaseline9(Player target, BaselineBuilder bb) {
		super.createBaseline9(target, bb);
	}
	
}
