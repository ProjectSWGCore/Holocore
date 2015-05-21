package resources;

public enum HologramColour {
	DEFAULT(-1), BLUE(0), PURPLE(4), ORANGE(8);
	
	private int value;
	
	HologramColour(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}