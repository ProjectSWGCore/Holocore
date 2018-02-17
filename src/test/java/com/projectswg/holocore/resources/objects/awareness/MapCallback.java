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
package com.projectswg.holocore.resources.objects.awareness;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.projectswg.common.debug.Log;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.awareness.TerrainMap.TerrainMapCallback;

class MapCallback implements TerrainMapCallback {
	
	private final AtomicInteger withinRange = new AtomicInteger(0);
	private final AtomicInteger outOfRange = new AtomicInteger(0);
	private final AtomicInteger successfulMove = new AtomicInteger(0);
	private final AtomicInteger failedMove = new AtomicInteger(0);
	
	@Override
	public void onWithinRange(SWGObject obj, SWGObject inRange) {
		withinRange.incrementAndGet();
	}
	
	@Override
	public void onOutOfRange(SWGObject obj, SWGObject outRange) {
		outOfRange.incrementAndGet();
	}
	
	@Override
	public void onMoveSuccess(SWGObject obj) {
		successfulMove.incrementAndGet();
	}
	
	@Override
	public void onMoveFailure(SWGObject obj) {
		failedMove.incrementAndGet();
		Log.e("Move failed!");
	}
	
	public void set(int withinRange, int outOfRange, int successful, int failed) {
		this.withinRange.set(withinRange);
		this.outOfRange.set(outOfRange);
		this.successfulMove.set(successful);
		this.failedMove.set(failed);
	}
	
	public int getWithinRange() {
		return withinRange.get();
	}
	
	public int getOutOfRange() {
		return outOfRange.get();
	}
	
	public int getSuccess() {
		return successfulMove.get();
	}
	
	public int getFailed() {
		return failedMove.get();
	}
	
//	public void waitAndTest(int withinRange, int outOfRange, int successful, int failed, long timeout) {
//		waitFor(withinRange, outOfRange, successful, failed, timeout);
//		testAssert(withinRange, outOfRange, successful, failed);
//	}
	
//	public void waitFor(int withinRange, int outOfRange, int successful, int failed, long timeout) {
//		long start = System.nanoTime();
//		this.withinRange.waitUntil(withinRange, timeout);
//		this.outOfRange.waitUntil(outOfRange, timeout-(long)((System.nanoTime()-start)/1E6));
//		this.successfulMove.waitUntil(successful, timeout-(long)((System.nanoTime()-start)/1E6));
//		this.failedMove.waitUntil(failed, timeout-(long)((System.nanoTime()-start)/1E6));
//	}
	
	public void testAssert(int withinRange, int outOfRange, int successful, int failed) {
		Assert.assertEquals("TEST-WITHIN-RANGE", withinRange, this.withinRange.get());
		Assert.assertEquals("TEST-OUT-OF-RANGE", outOfRange, this.outOfRange.get());
		Assert.assertEquals("TEST-SUCCESSFUL-MOVE", successful, this.successfulMove.get());
		Assert.assertEquals("TEST-FAILED-MOVE", failed, this.failedMove.get());
	}
	
}
