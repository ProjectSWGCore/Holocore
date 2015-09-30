package resources;

public enum PvpStatus {

	ONLEAVE(0),
	COMBATANT(1),
	SPECIALFORCES(2);
	
	private int value;
	
	PvpStatus(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
	
}