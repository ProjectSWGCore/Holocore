package resources.combat;

public enum ValidTarget {
	NONE	(-1),
	STANDARD(0),
	MOB		(1),
	CREATURE(2),
	NPC		(3),
	DROID	(4),
	PVP		(5),
	JEDI	(6),
	DEAD	(7),
	FRIEND	(8);
	
	private static final ValidTarget [] VALUES = values();
	
	private int num;
	
	ValidTarget(int num) {
		this.num = num;
	}
	
	public int getNum() {
		return num;
	}
	
	public static ValidTarget getValidTarget(int num) {
		++num;
		if (num < 0 || num >= VALUES.length)
			return STANDARD;
		return VALUES[num];
	}
	
}
