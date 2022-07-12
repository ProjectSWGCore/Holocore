package com.projectswg.holocore.resources.support.data.namegen;

import com.projectswg.common.data.encodables.tangible.Race;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

public class TestSWGNameGenerator {
	
	@ParameterizedTest
	@EnumSource(Race.class)
	public void canGenerateNamesForAllRacesWithoutError(Race race) {
		SWGNameGenerator generator = new SWGNameGenerator();
		
		assertNotEquals("Should be able to generate non-empty name for race " +  race.getSpecies(), "", generator.generateName(race));
	}
}
