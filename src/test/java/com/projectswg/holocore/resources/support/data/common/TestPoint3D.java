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
package com.projectswg.holocore.resources.support.data.common;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.data.location.Quaternion;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestPoint3D extends TestRunnerNoIntents {
	
	@Test
	public void testRotation() {
		Point3D p = new Point3D(0, 0, 1);
		Quaternion q = new Quaternion(0, 1, 0, 0);
		p.rotateAround(0, 0, 0, q);
		Assert.assertEquals(0, p.getX(), 1E-7);
		Assert.assertEquals(0, p.getY(), 1E-7);
		Assert.assertEquals(-1, p.getZ(), 1E-7);
		p.set(0, 0, 1);
		q.setHeading(45);
		p.rotateAround(0, 0, 0, q);
		Assert.assertEquals(Math.sqrt(2)/2, p.getX(), 1E-7);
		Assert.assertEquals(0, p.getY(), 1E-7);
		Assert.assertEquals(Math.sqrt(2)/2, p.getZ(), 1E-7);
	}
	
}
