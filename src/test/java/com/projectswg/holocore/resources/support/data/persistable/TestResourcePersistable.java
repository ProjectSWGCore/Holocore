/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.persistable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.projectswg.common.persistable.InputPersistenceStream;
import com.projectswg.common.persistable.OutputPersistenceStream;

import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;

@RunWith(JUnit4.class)
public class TestResourcePersistable {
	
	@Test
	public void testGalacticResourcePersistable() throws IOException {
		GalacticResource res = new GalacticResource(1, "MYRESOURCE", 15);
		res.generateRandomStats();
		test(res);
		res = new GalacticResource(200, "Test", 100);
		res.generateRandomStats();
		test(res);
		for (int i = 0; i < 50; i++) {
			res.generateRandomStats();
			test(res);
		}
	}
	
	private void test(GalacticResource resource) throws IOException {
		byte [] written = write(resource);
		GalacticResource read = read(written);
		test(resource, read);
	}
	
	private void test(GalacticResource write, GalacticResource read) {
		Assert.assertEquals(write.getId(), read.getId());
		Assert.assertEquals(write.getName(), read.getName());
		Assert.assertEquals(write.getRawResourceId(), read.getRawResourceId());
		Assert.assertEquals(write.getStats().getColdResistance(), read.getStats().getColdResistance());
		Assert.assertEquals(write.getStats().getConductivity(), read.getStats().getConductivity());
		Assert.assertEquals(write.getStats().getDecayResistance(), read.getStats().getDecayResistance());
		Assert.assertEquals(write.getStats().getEntangleResistance(), read.getStats().getEntangleResistance());
		Assert.assertEquals(write.getStats().getFlavor(), read.getStats().getFlavor());
		Assert.assertEquals(write.getStats().getHeatResistance(), read.getStats().getHeatResistance());
		Assert.assertEquals(write.getStats().getMalleability(), read.getStats().getMalleability());
		Assert.assertEquals(write.getStats().getOverallQuality(), read.getStats().getOverallQuality());
		Assert.assertEquals(write.getStats().getPotentialEnergy(), read.getStats().getPotentialEnergy());
		Assert.assertEquals(write.getStats().getShockResistance(), read.getStats().getShockResistance());
		Assert.assertEquals(write.getStats().getUnitToughness(), read.getStats().getUnitToughness());
	}
	
	private byte [] write(GalacticResource mail) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		OutputPersistenceStream os = new OutputPersistenceStream(baos);
		os.write(mail, GalacticResource::save);
		os.close();
		return baos.toByteArray();
	}
	
	private GalacticResource read(byte [] data) throws IOException {
		try (InputPersistenceStream is = new InputPersistenceStream(new ByteArrayInputStream(data))) {
			GalacticResource mail = is.read(GalacticResource::create);
			Assert.assertEquals(0, is.available());
			return mail;
		}
	}
	
}
