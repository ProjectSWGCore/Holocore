/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.support.data.common;

import com.projectswg.common.data.location.Quaternion;
import com.projectswg.holocore.test.runners.TestRunnerNoIntents;
import org.junit.Assert;
import org.junit.Test;

public class TestQuaternion extends TestRunnerNoIntents {
	
	@Test
	public void testRotation() {
		test(30, -210, 0, -1, 0, 0);
		test(30, 150, 0, 1, 0, 0);
		test(-90, -90, 0, -1, 0, 0);
		test(90, 90, 0, 1, 0, 0);
		test(90, -45, 0, Math.sin(Math.PI/8), 0, Math.cos(Math.PI/8));
//		test(180, -45, 0, Math.sin(-Math.PI/8), 0, Math.cos(-Math.PI/8));
	}
	
	private void test(double heading1, double heading2, double x, double y, double z, double w) {
		Quaternion q1 = new Quaternion(0, 0, 0, 1);
		q1.setHeading(heading1);
		Quaternion q2 = new Quaternion(0, 0, 0, 1);
		q2.setHeading(heading2);
		Quaternion ret = new Quaternion(q1);
		ret.rotateByQuaternion(q2);
		Assert.assertEquals(x, ret.getX(), 1E-7);
		Assert.assertEquals(y, ret.getY(), 1E-7);
		Assert.assertEquals(z, ret.getZ(), 1E-7);
		Assert.assertEquals(w, ret.getW(), 1E-7);
//		Assert.assertEquals((heading1+heading2+360)%360, (ret.getYaw()+360)%360, 1E-7);
	}
}
