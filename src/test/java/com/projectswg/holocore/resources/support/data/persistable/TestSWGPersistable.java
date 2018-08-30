/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of PSWGCommon.                                                *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * PSWGCommon is free software: you can redistribute it and/or modify              *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * PSWGCommon is distributed in the hope that it will be useful,                   *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with PSWGCommon.  If not, see <http://www.gnu.org/licenses/>.             *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.persistable;

import com.projectswg.common.data.encodables.tangible.Race;
import com.projectswg.common.persistable.InputPersistenceStream;
import com.projectswg.common.persistable.OutputPersistenceStream;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.building.BuildingObject;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TestSWGPersistable extends TestRunnerNoIntents {
	
	@Test
	public void testCreatureObject() throws IOException {
		test(Race.HUMAN_MALE.getFilename());
	}
	
	@Test
	public void testPlayerObject() throws IOException {
		test("object/player/shared_player.iff");
	}
	
	@Test
	public void testTangibleObject() throws IOException {
		test("object/tangible/inventory/shared_character_inventory.iff");
	}
	
	@Test
	public void testWeaponObject() throws IOException {
		test("object/weapon/melee/unarmed/shared_unarmed_default_player.iff");
	}
	
	@Test
	public void testResourceObject() throws IOException {
		test("object/resource_container/shared_resource_container_energy_gas.iff");
	}
	
	@Test
	public void testWaypointObject() throws IOException {
		test("object/waypoint/shared_world_waypoint_orange.iff");
	}
	
	@Test
	public void testMissionObject() throws IOException {
		test("object/mission/shared_mission_object.iff");
	}
	
	@Test
	public void testFactoryObject() throws IOException {
		test("object/factory/shared_factory_crate_weapon.iff");
	}
	
	@Test
	public void testInstallationObject() throws IOException {
		test("object/installation/generators/shared_power_generator_wind_style_1.iff");
	}
	
	@Test
	public void testShipObject() throws IOException {
		test("object/ship/shared_grievous_starship.iff");
	}
	
	@Test
	public void testManufactureObject() throws IOException {
		test("object/manufacture_schematic/shared_generic_schematic.iff");
	}
	
	@Test
	public void testBuildingObject() throws IOException {
		BuildingObject expected = (BuildingObject) ObjectCreator.createObjectFromTemplate("object/building/tatooine/shared_palace_tatooine_jabba.iff");
		expected.populateCells();
		test(expected);
	}
	
	@Test
	public void testCellObject() throws IOException {
		test("object/cell/shared_cell.iff");
	}
	
	private void test(String template) throws IOException {
		test(ObjectCreator.createObjectFromTemplate(template));
	}
	
	private void test(SWGObject object) throws IOException {
		byte [] written = write(object);
		SWGObject read = read(written);
		test(object, read);
	}
	
	private void test(SWGObject expected, SWGObject actual) {
		Assert.assertNotNull(actual);
		Assert.assertTrue("actual instanceof expected", expected.getClass().isAssignableFrom(actual.getClass()));
		Assert.assertEquals("expected.equals(actual)", expected, actual);
		Assert.assertEquals("contained objects", expected.getContainedObjects(), actual.getContainedObjects());
		Assert.assertEquals("slots", expected.getSlots(), actual.getSlots());
	}
	
	private byte [] write(SWGObject obj) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputPersistenceStream os = new OutputPersistenceStream(baos);
		os.write(obj, SWGObjectFactory::save);
		os.close();
		return baos.toByteArray();
	}
	
	private SWGObject read(byte [] data) throws IOException {
		try (InputPersistenceStream is = new InputPersistenceStream(new ByteArrayInputStream(data))) {
			SWGObject obj = is.read(SWGObjectFactory::create);
			Assert.assertEquals(0, is.available());
			return obj;
		}
	}
	
}
