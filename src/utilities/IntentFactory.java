package utilities;

import intents.sui.SuiWindowIntent;
import network.packets.swg.zone.chat.ChatSystemMessage;
import resources.Terrain;
import resources.encodables.OutOfBandPackage;
import resources.encodables.ProsePackage;
import resources.encodables.StringId;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatBroadcastIntent.BroadcastType;
import intents.chat.PersistentMessageIntent;
import resources.sui.SuiWindow;

/**
 * @author Mads
 * This class is to be used exclusively in cases where using the intents directly isn't practical.
 */
public final class IntentFactory {

	private void broadcast(String message, Player source, BroadcastType type) {
		new ChatBroadcastIntent(message, source, source.getCreatureObject().getTerrain(), type).broadcast();
	}

	/**
	 * Sends a system message around the observing players for the source {@link Player}.
	 * @param message System message to broadcast.
	 * @param source Source of the system message, anyone observing this player will receive the system message.
	 */
	public void broadcastArea(String message, Player source) {
		broadcast(message, source, BroadcastType.AREA);
	}

	/**
	 * Sends a system message to the entire galaxy.
	 * @param message System message to broadcast.
	 */
	public void broadcastGalaxy(String message) {
		broadcast(message, null, BroadcastType.GALAXY);
	}

	/**
	 * Sends a system message to all players who are on the specified {@link Terrain}.
	 * @param terrain Terrain to broadcast system message on.
	 * @param message System message to broadcast.
	 */
	public void broadcastPlanet(Terrain terrain, String message) {
		new ChatBroadcastIntent(message, null, terrain, BroadcastType.PLANET).broadcast();
	}

	/**
	 * Sends a system message to the target.
	 * @param target Player receiving the message.
	 * @param message System message to send.
	 */
	public void sendSystemMessage(Player target, String message) {
		broadcast(message, target, BroadcastType.PERSONAL);
	}

	/**
	 * Sends a system message to the target as a ProsePackage which allows prose keys to be used.
	 * <br><br>
	 * Example: <br>
	 * &nbsp&nbsp&nbsp&nbsp <i>sendSystemMessage(target, "base_player", "prose_deposit_success", "DI", 500);</i>
	 * <br><br>
	 * Using this method is the same as: <br>
	 * &nbsp <i>new ChatBroadcastIntent(target, new ProsePackage("StringId", new StringId(table, key), objects)).broadcast();</i>
	 * @param target Player receiving the system message.
	 * @param stf The string stf table to use for this system message. It should be formatted as such: <br><br>
	 * <code> example_key where @example refers to the stf example.stf in string/en folder and example_key is the key to use for the stf.</cZzz>
	 * @param objects Collection of prose keys followed by with the value for the prose key.<br>As an example, <i>("DI", 500)</i> would
	 *                set the %DI to the value of 500 for the StringId.
	 *                Note that the prose key must always come first and the value for that key must always come second.
	 */
	public void sendSystemMessage(Player target, String stf, Object ... objects) {
		Object [] prose = new Object[objects.length + 2];
		prose[0] = "StringId";
		prose[1] = new StringId(stf);
		System.arraycopy(objects, 0, prose, 2, objects.length);
		new ChatBroadcastIntent(target, new ProsePackage(prose)).broadcast();
	}

	/**
	 * Sends a message to just the console chat for the target.
	 * @param target Player to receive the message
	 * @param message Message to display in the players chat console.
	 */
	public void sendConsoleMessage(Player target, String message) {
		target.sendPacket(new ChatSystemMessage(ChatSystemMessage.SystemChatType.CHAT, message));
	}

	/**
	 * Sends a mail to the receiver with the specified subject, message, from the specified sender.
	 * @param receiver Player receiving the mail.
	 * @param sender The sender of this mail.
	 * @param subject Subject of the mail.
	 * @param message Message for the mail.
	 */
	public void sendMail(SWGObject receiver, String sender, String subject, String message) {
		Mail mail = new Mail(sender, subject, message, receiver.getObjectId());

		new PersistentMessageIntent(receiver, mail, receiver.getOwner().getGalaxyName()).broadcast();
	}

	/**
	 * Sends a mail to the receiver with the specified subject, message, from the specified sender. In addition to a normal
	 * string based mail, the provided waypoints are added as attachments to the message.
	 * @param receiver Player receiving the mail.
	 * @param sender The sender of this mail.
	 * @param subject Subject of the mail.
	 * @param message Message for the mail.
	 * @param waypoints Waypoints to attach to the mail.
	 */
	public void sendMail(SWGObject receiver, String sender, String subject, String message, WaypointObject... waypoints) {
		Mail mail = new Mail(sender, subject, message, receiver.getObjectId());
		mail.setOutOfBandPackage(new OutOfBandPackage(waypoints));

		new PersistentMessageIntent(receiver, mail, receiver.getOwner().getGalaxyName()).broadcast();
	}

}
