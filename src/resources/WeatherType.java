package resources;

public enum WeatherType {
	CLEAR(0),
	CLOUDY(1),
	LIGHT(2),
	MEDIUM(3),
	HEAVY(4);
	
	private int value;
	
	WeatherType(int value) {
		this.value = value;
	}
	
	public int getValue() {
		return value;
	}
}
