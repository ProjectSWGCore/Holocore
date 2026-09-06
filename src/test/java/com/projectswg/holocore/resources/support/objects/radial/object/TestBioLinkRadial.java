package com.projectswg.holocore.resources.support.objects.radial.object;

import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.test.resources.GenericCreatureObject;
import com.projectswg.holocore.test.resources.GenericPlayer;
import com.projectswg.holocore.test.resources.GenericTangibleObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestBioLinkRadial {

	private BioLinkRadial radial;
	private GenericPlayer player;
	private TangibleObject item;

	@BeforeEach
	public void setup() {
		radial = new BioLinkRadial();
		player = new GenericPlayer();
		CreatureObject creatureObject = new GenericCreatureObject(1, "Some Player", true);
		player.setCreatureObject(creatureObject);

		item = new GenericTangibleObject(3);
	}

	@Test
	public void itemsWithoutBioLinkShouldHaveNoOptions() {
		List<RadialOption> options = new ArrayList<>();

		radial.getOptions(options, player, item);

		assertTrue(options.isEmpty());
	}

	@Test
	public void unlinkedItemsShouldHaveTheBioLinkOption() {
		item.setBioLinkRequired(true);
		List<RadialOption> options = new ArrayList<>();

		radial.getOptions(options, player, item);

		assertEquals(1, options.size());
		assertEquals("@ui_radial:bio_link", options.get(0).getLabel());
	}

	@Test
	public void linkedItemsShouldHaveNoOptions() {
		item.setBioLinkRequired(true);
		item.setBioLinkedTo(12345678L);
		List<RadialOption> options = new ArrayList<>();

		radial.getOptions(options, player, item);

		assertTrue(options.isEmpty());
	}
}
