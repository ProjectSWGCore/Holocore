package resources.objects.factory;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.objects.tangible.TangibleObject;

public class FactoryObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	public FactoryObject(long objectId) {
		super(objectId, BaselineType.FCYT);
	}
	
}
