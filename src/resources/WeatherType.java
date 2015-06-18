package resources;

/**
 * Make sure that the chances for each weather type add up to 1.0 (100%).
 * @author Ziggy
 *
 */
public enum WeatherType {
	CLEAR(0, .60f),	// 60% chance
	LIGHT(1, .20f),	// 20% chance
	MEDIUM(2, .15f),	// 15% chance
	HEAVY(3, .05f);	// 5% chance
	
	private int value;
	private float chance;
	
	WeatherType(int value, float chance) {
		this.value = value;
		this.chance = chance;
	}
	
	public int getValue() {
		return value;
	}
	
	public float getChance() {
		return chance;
	}
	
}
