package resources.control;

import network.OutboundPacketService;
import network.packets.Packet;
import resources.config.ConfigFile;
import resources.player.Player;
import resources.server_info.DataManager;
import resources.server_info.RelationalDatabase;
import resources.services.Config;


/**
 * A Service is a class that does a specific job for the application
 */
public class Service implements IntentReceiver {
	
	private static final OutboundPacketService outboundPacketService = new OutboundPacketService();
	
	/**
	 * Initializes this service. If the service returns false on this method
	 * then the initialization failed and may not work as intended.
	 * @return TRUE if initialization was successful, FALSE otherwise
	 */
	public boolean initialize() {
		IntentManager.getInstance().initialize();
		return DataManager.getInstance().isInitialized() && ServerManager.getInstance().initialize();
	}
	
	/**
	 * Starts this service. If the service returns false on this method then
	 * the service failed to start and may not work as intended.
	 * @return TRUE if starting was successful, FALSE otherwise
	 */
	public boolean start() {
		return DataManager.getInstance().isInitialized();
	}
	
	/**
	 * Terminates this service. If the service returns false on this method
	 * then the service failed to shut down and resources may not have been
	 * cleaned up.
	 * @return TRUE if termination was successful, FALSE otherwise
	 */
	public boolean terminate() {
		IntentManager.getInstance().terminate();
		return ServerManager.getInstance().terminate();
	}
	
	/**
	 * Determines whether or not this service is operational
	 * @return TRUE if this service is operational, FALSE otherwise
	 */
	public boolean isOperational() {
		return true;
	}
	
	/**
	 * Registers for the specified intent string
	 * @param type the intent string
	 */
	public void registerForIntent(String type) {
		IntentManager.getInstance().registerForIntent(type, this);
	}
	
	/**
	 * Unregisters for the specified intent string
	 * @param type the intent string
	 */
	public void unregisterForIntent(String type) {
		IntentManager.getInstance().unregisterForIntent(type, this);
	}
	
	/**
	 * Callback when an intent is received from the system
	 */
	public void onIntentReceived(Intent i) {
		System.out.println("Warning: " + getClass().getSimpleName() + " did not override onIntentReceived");
	}
	
	/**
	 * Stores the packet(s) in a buffer until flushPackets() is called.
	 * flushPackets() is called every couple milliseconds.
	 * @param player the player to send the packet(s) to
	 * @param packets the packet(s) to send
	 */
	public void sendPacket(Player player, Packet ... packets) {
		sendPacket(player.getNetworkId(), packets);
	}
	
	/**
	 * Stores the packet(s) in a buffer until flushPackets() is called.
	 * flushPackets() is called every couple milliseconds if you forget.
	 * @param networkId the network id to send the packet(s) to
	 * @param packets the packet(s) to send
	 */
	public void sendPacket(final long networkId, final Packet ... packets) {
		outboundPacketService.sendPacket(networkId, packets);
	}
	
	/**
	 * Sends all packets that were stored in the buffer via sendPacket()
	 * @return the number of packets sent (includes SWG packets inside
	 * multi/data packets)
	 */
	public int flushPackets() {
		return outboundPacketService.flushPackets();
	}
	
	/**
	 * Gets the config object associated with a certain file, or NULL if the
	 * file failed to load on startup
	 * @param file the file to get the config for
	 * @return the config object associated with the file, or NULL if the
	 * config failed to load
	 */
	public synchronized final Config getConfig(ConfigFile file) {
		return DataManager.getInstance().getConfig(file);
	}
	
	/**
	 * Gets the relational database associated with the local postgres database
	 * @return the database for the local postgres database
	 */
	public synchronized final RelationalDatabase getLocalDatabase() {
		return DataManager.getInstance().getLocalDatabase();
	}
	
	
	
}
