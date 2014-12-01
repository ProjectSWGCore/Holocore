package resources.objects.creature;

public enum CreatureDifficulty {
	NORMAL	(0x00),
	ELITE	(0x01),
	BOSS	(0x01);
	
	private int difficulty;
	
	CreatureDifficulty(int difficulty) {
		this.difficulty = difficulty;
	}
	
	public byte getDifficulty() {
		return (byte) difficulty;
	}
	
	public static final CreatureDifficulty getForDifficulty(int difficulty) {
		for (CreatureDifficulty d : values())
			if (d.getDifficulty() == difficulty)
				return d;
		return NORMAL;
	}
}
