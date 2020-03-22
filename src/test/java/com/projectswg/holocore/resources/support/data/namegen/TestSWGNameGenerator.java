package com.projectswg.holocore.resources.support.data.namegen;

import com.projectswg.common.data.encodables.tangible.Race;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertNotEquals;

@RunWith(Parameterized.class)
public class TestSWGNameGenerator {
	
	@Parameterized.Parameters(name = "{0}")
	public static Collection<Race> input() {
		return Arrays.asList(Race.values());
	}
	
	@Parameterized.Parameter
	public Race race;
	
	/**
	 * Verifies that we can generate names for all races without encountering an error
	 */
	@Test
	public void testCanGenerate() {
		SWGNameGenerator generator = new SWGNameGenerator();
		
		assertNotEquals("Should be able to generate non-empty name for race " +  race.getSpecies(), "", generator.generateName(race));
	}
}
