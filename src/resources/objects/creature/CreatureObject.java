package resources.objects.creature;

import resources.Posture;
import resources.objects.tangible.TangibleObject;

public class CreatureObject extends TangibleObject {
	
	private Posture	posture					= Posture.STANDING;
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
	
}
