package resources;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestWeatherType {

	/**
	 * Tests if the chance of each weather type sum up to 100%
	 */
	@Test
	public void verifyChances() {
		float expected = 1;
		float actual = 0;
		
		for(WeatherType type : WeatherType.values())
			actual += type.getChance();
		
		assertEquals(expected, actual, 0);
		
	}
	
}
