package resources.objects.tangible;

import resources.objects.SWGObject;

public class TangibleObject extends SWGObject {
	
	private byte []	appearanceData	= new byte[0];
	private int		damageTaken		= 0;
	private int		maxHitPoints	= 0;
	private int		components		= 0;
	private boolean	visible			= true;
	private boolean	inCombat		= false;
	private int		condition		= 0;
	private int		pvpFlags		= 0;
	private int		pvpType			= 0;
	private long	pvpFactionId	= 0;
	private boolean	visibleGmOnly	= false;
	private byte []	objectEffects	= new byte[0];
	
	public TangibleObject(long objectId) {
		super(objectId);
	}
	
	public synchronized byte [] getAppearanceData() {
		return appearanceData;
	}
	
	public synchronized int getDamageTaken() {
		return damageTaken;
	}
	
	public synchronized int getMaxHitPoints() {
		return maxHitPoints;
	}
	
	public synchronized int getComponents() {
		return components;
	}
	
	public synchronized boolean isVisible() {
		return visible;
	}
	
	public synchronized boolean isInCombat() {
		return inCombat;
	}
	
	public synchronized int getCondition() {
		return condition;
	}
	
	public synchronized int getPvpFlags() {
		return pvpFlags;
	}
	
	public synchronized int getPvpType() {
		return pvpType;
	}
	
	public synchronized long getPvpFactionId() {
		return pvpFactionId;
	}
	
	public synchronized boolean isVisibleGmOnly() {
		return visibleGmOnly;
	}
	
	public synchronized byte [] getObjectEffects() {
		return objectEffects;
	}
	
	public synchronized void setAppearanceData(byte [] appearanceData) {
		this.appearanceData = appearanceData;
	}
	
	public synchronized void setDamageTaken(int damageTaken) {
		this.damageTaken = damageTaken;
	}
	
	public synchronized void setMaxHitPoints(int maxHitPoints) {
		this.maxHitPoints = maxHitPoints;
	}
	
	public synchronized void setComponents(int components) {
		this.components = components;
	}
	
	public synchronized void setVisible(boolean visible) {
		this.visible = visible;
	}
	
	public synchronized void setInCombat(boolean inCombat) {
		this.inCombat = inCombat;
	}
	
	public synchronized void setCondition(int condition) {
		this.condition = condition;
	}
	
	public synchronized void setPvpFlags(int pvpFlags) {
		this.pvpFlags = pvpFlags;
	}
	
	public synchronized void setPvpType(int pvpType) {
		this.pvpType = pvpType;
	}
	
	public synchronized void setPvpFactionId(long pvpFactionId) {
		this.pvpFactionId = pvpFactionId;
	}
	
	public synchronized void setVisibleGmOnly(boolean visibleGmOnly) {
		this.visibleGmOnly = visibleGmOnly;
	}
	
	public synchronized void setObjectEffects(byte [] objectEffects) {
		this.objectEffects = objectEffects;
	}
	
}
