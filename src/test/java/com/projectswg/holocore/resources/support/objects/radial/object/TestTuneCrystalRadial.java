package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestTuneCrystalRadial {
	
	private TuneCrystalRadial radial;
	private GenericPlayer player;
	private SWGObject crystal;
	
	@BeforeEach
	public void setup() {
		radial = new TuneCrystalRadial();
		player = new GenericPlayer();
		CreatureObject creatureObject = new GenericCreatureObject(1, "Some Player", true);
		player.setCreatureObject(creatureObject);
		
		crystal = new GenericTangibleObject(3);
		crystal.setGameObjectType(GameObjectType.GOT_COMPONENT_SABER_CRYSTAL);
	}
	
	@Test
	public void youShouldNotBeAbleToTuneObjectsThatAreNotLightsaberCrystals() {
		player.getCreatureObject().addSkill("jedi");	// Only Jedi can tune crystals
		List<RadialOption> options = new ArrayList<>();
		crystal.setGameObjectType(GameObjectType.GOT_CLOTHING_JACKET);	// Let's change the object type to something different
		
		radial.getOptions(options, player, crystal);
		
		assertTrue(options.isEmpty());
	}
	
	@Test
	public void untunedCrystalsShouldHaveTheTuneRadialOptionForJedi() {
		player.getCreatureObject().addSkill("jedi");	// Only Jedi can tune crystals
		List<RadialOption> options = new ArrayList<>();
		radial.getOptions(options, player, crystal);
		
		assertEquals(1, options.size());
		
		RadialOption radialOption = options.get(0);
		assertEquals("@jedi_spam:tune_crystal", radialOption.getLabel());
	}
	
	@Test
	public void tunedCrystalShouldHaveNoOptions() {
		player.getCreatureObject().addSkill("jedi");	// Only Jedi can tune crystals
		List<RadialOption> options = new ArrayList<>();
		
		// Let's tune the crystal
		crystal.setServerAttribute(ServerAttribute.LINK_OBJECT_ID, 12345678);
		
		radial.getOptions(options, player, crystal);
		
		assertTrue(options.isEmpty());
	}
	
	@Test
	public void onlyJediCanTuneCrystals() {
		player.getCreatureObject().addSkill("swg_dev");	// Something that's not Jedi - doesn't really matter what
		List<RadialOption> options = new ArrayList<>();

		radial.getOptions(options, player, crystal);
		
		assertTrue(options.isEmpty());
	}
}
