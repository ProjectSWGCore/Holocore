package resources.objects.intangible;

import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class IntangibleObject extends SWGObject {
	
	private int	count	= 0;
	
	public IntangibleObject(long objectId) {
		super(objectId);
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}
	
}
