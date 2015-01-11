package intents.chat;

import resources.control.Intent;
import resources.encodables.player.Mail;
import resources.objects.SWGObject;

public class PersistentMessageIntent extends Intent {
	public static final String TYPE = "PersistentMessageIntent";
	
	private Mail mail;
	private SWGObject receiver;
	private String galaxy;
	
	public PersistentMessageIntent(SWGObject receiver, Mail mail, String galaxy) {
		super(TYPE);
		this.receiver = receiver;
		this.mail = mail;
		this.galaxy = galaxy;
	}

	public Mail getMail() { return this.mail; }
	public SWGObject getReceiver() { return this.receiver; }
	public String getGalaxy() { return this.galaxy; }
}
