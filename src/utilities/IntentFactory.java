package utilities;

import resources.encodables.player.Mail;
import resources.objects.SWGObject;
import resources.player.Player;
import intents.chat.ChatBroadcastIntent;
import intents.chat.ChatBroadcastIntent.BroadcastType;
import intents.chat.PersistentMessageIntent;

/**
 * @author Mads
 * This class is to be used exclusively in cases where using the intents directly isn't practical.
 */
public final class IntentFactory {

	private void broadcast(String message, Player source, BroadcastType type) {
		new ChatBroadcastIntent(message, source, source.getCreatureObject().getTerrain(), type).broadcast();
	}
	
	public void broadcastArea(String message, Player source) {
		broadcast(message, source, BroadcastType.AREA);
	}
	
	public void broadcastGalaxy(String message, Player source) {
		broadcast(message, source, BroadcastType.GALAXY);
	}
	
	public void broadcastPersonal(String message, Player source) {
		broadcast(message, source, BroadcastType.PERSONAL);
	}
	
	public void broadcastPlanet(String message, Player source) {
		broadcast(message, source, BroadcastType.PLANET);
	}
	
	public void sendMail(SWGObject receiver, String sender, String subject, String message) {
		Mail mail = new Mail(sender, subject, message, receiver.getObjectId());
		
		new PersistentMessageIntent(receiver, mail, receiver.getOwner().getGalaxyName()).broadcast();
	}
	
}
