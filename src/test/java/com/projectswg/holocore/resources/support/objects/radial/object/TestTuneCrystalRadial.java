package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import com.projectswg.holocore.resources.support.objects.swg.player.Profession;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class TestTuneCrystalRadial {
	
	private TuneCrystalRadial radial;
	private GenericPlayer player;
	private SWGObject crystal;
	
	@Before
	public void setup() {
		radial = new TuneCrystalRadial();
		player = new GenericPlayer();
		CreatureObject creatureObject = new GenericCreatureObject(1, "Some Player", true);
		creatureObject.getPlayerObject().setProfession(Profession.FORCE_SENSITIVE);	// Only Jedi can tune crystals
		player.setCreatureObject(creatureObject);
		
		crystal = new GenericTangibleObject(3);
		crystal.setGameObjectType(GameObjectType.GOT_COMPONENT_SABER_CRYSTAL);
		crystal.addAttribute("@obj_attr_n:crystal_owner", "\\#D1F56F UNTUNED \\#FFFFFF ");
	}
	
	@Test
	public void testNotCrystal() {
		List<RadialOption> options = new ArrayList<>();
		crystal.setGameObjectType(GameObjectType.GOT_CLOTHING_JACKET);	// Let's change the object type to something different
		
		radial.getOptions(options, player, crystal);
		
		assertTrue("You should not be able to tune objects that are not lightsaber crystals", options.isEmpty());
	}
	
	@Test
	public void testCrystalUntuned() {
		List<RadialOption> options = new ArrayList<>();
		radial.getOptions(options, player, crystal);
		
		assertEquals("Untuned crystals should have one radial option", 1, options.size());
		
		RadialOption radialOption = options.get(0);
		assertEquals("Untuned crystals should present the option of tuning them", "@jedi_spam:tune_crystal", radialOption.getLabel());
	}
	
	@Test
	public void testCrystalAlreadyTuned() {
		List<RadialOption> options = new ArrayList<>();
		
		// Let's tune the crystal
		crystal.addAttribute("@obj_attr_n:crystal_owner", "Some Player");
		
		radial.getOptions(options, player, crystal);
		
		assertTrue("Tuned crystals should have no options", options.isEmpty());
	}
	
	@Test
	public void testNotJedi() {
		List<RadialOption> options = new ArrayList<>();
		player.getPlayerObject().setProfession(Profession.MEDIC);	// Something that's not Jedi - doesn't really matter what
		
		radial.getOptions(options, player, crystal);
		
		assertTrue("Only Jedi should be able to tune crystals", options.isEmpty());
	}
}
