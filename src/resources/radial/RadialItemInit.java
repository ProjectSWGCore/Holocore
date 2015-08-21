package resources.radial;

import java.util.concurrent.atomic.AtomicInteger;

class RadialItemInit {
	
	private static final AtomicInteger ID = new AtomicInteger(0);
	
	public static int getNextItemId() {
		return ID.getAndIncrement();
	}
	
}
