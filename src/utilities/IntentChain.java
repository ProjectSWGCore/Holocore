package utilities;

import resources.control.Intent;

public class IntentChain {
	
	private final Object mutex;
	private Intent i;
	
	public IntentChain() {
		mutex = new Object();
		i = null;
	}
	
	public void reset() {
		synchronized (mutex) {
			i = null;
		}
	}
	
	public void broadcastAfter(Intent i) {
		synchronized (mutex) {
			i.broadcastAfterIntent(this.i);
			this.i = i;
		}
	}
	
}
