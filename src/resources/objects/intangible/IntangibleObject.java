package resources.objects.intangible;

import resources.objects.SWGObject;

public class IntangibleObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
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
