package resources.objects.installation;

import resources.objects.tangible.TangibleObject;

public class InstallationObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private boolean activated	= false;
	private double	power		= 0;
	private double	powerRate	= 0;
	
	public InstallationObject(long objectId) {
		super(objectId);
	}
	
	public boolean isActivated() {
		return activated;
	}
	
	public double getPower() {
		return power;
	}
	
	public double getPowerRate() {
		return powerRate;
	}
	
	public void setActivated(boolean activated) {
		this.activated = activated;
	}
	
	public void setPower(double power) {
		this.power = power;
	}
	
	public void setPowerRate(double powerRate) {
		this.powerRate = powerRate;
	}
	
}
