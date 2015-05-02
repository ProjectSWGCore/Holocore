package resources.control;


public interface IntentReceiver {
	
	/**
	 * This function will be called if an intent is broadcasted and this
	 * manager is listening for it, or if this manager is specifically given
	 * this intent.
	 * @param i the intent received
	 */
	public void onIntentReceived(Intent i);
	
}
