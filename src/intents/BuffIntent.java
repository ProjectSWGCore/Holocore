package intents;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public class BuffIntent extends Intent {

	public static final String TYPE = "BuffIntent";
	
	private final String buffName;
	private final CreatureObject buffer, receiver;
	private final boolean remove;
	
	public BuffIntent(String buffName, CreatureObject buffer, CreatureObject receiver, boolean remove) {
		super(TYPE);
		this.buffName = buffName;
		this.buffer = buffer;
		this.receiver = receiver;
		this.remove = remove;
	}

	public CreatureObject getReceiver() {
		return receiver;
	}

	public CreatureObject getBuffer() {
		return buffer;
	}

	public String getBuffName() {
		return buffName;
	}

	public boolean isRemove() {
		return remove;
	}
	
}
