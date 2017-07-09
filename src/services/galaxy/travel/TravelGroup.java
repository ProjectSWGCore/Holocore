/***********************************************************************************
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
package services.galaxy.travel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.projectswg.common.concurrency.Delay;
import com.projectswg.common.debug.Log;

import resources.Posture;
import resources.objects.creature.CreatureObject;

public class TravelGroup implements Runnable {
	
	private final List<TravelPoint> points;
	private final AtomicLong timeRemaining;
	private final long landTime;
	private final long groundTime;
	private final long airTime;
	private ShuttleStatus status;
	
	public TravelGroup(long landTime, long groundTime, long airTime) {
		this.points = new ArrayList<>();
		this.timeRemaining = new AtomicLong(airTime / 1000);
		this.landTime = landTime + 10000;
		this.groundTime = groundTime;
		this.airTime = airTime;
		this.status = ShuttleStatus.GROUNDED;
	}
	
	public void addTravelPoint(TravelPoint point) {
		this.points.add(point);
	}
	
	public int getTimeRemaining() {
		return timeRemaining.intValue();
	}
	
	public ShuttleStatus getStatus() {
		return status;
	}
	
	@Override
	public void run() {
		try {
			while (!Delay.isInterrupted()) {
				// GROUNDED
				handleStatusGrounded();
				// LEAVING
				handleStatusLeaving();
				// AWAY
				handleStatusAway();
				// LANDING
				handleStatusLanding();
			}
		} catch (Exception e) {
			Log.e(e);
		}
	}
	
	private void handleStatusGrounded() {
		status = ShuttleStatus.GROUNDED;
		Delay.sleepMilli(groundTime);
	}
	
	private void handleStatusLeaving() {
		status = ShuttleStatus.LEAVING;
		updateShuttlePostures(false);
		Delay.sleepMilli(landTime);
	}
	
	private void handleStatusAway() {
		status = ShuttleStatus.AWAY;
		for (int timeElapsed = 0; timeElapsed < airTime / 1000; timeElapsed++) {
			if (Delay.sleepSeconds(1))
				break;
			timeRemaining.decrementAndGet();
		}
		timeRemaining.set(airTime / 1000);	// Reset the timer
	}
	
	private void handleStatusLanding() {
		status = ShuttleStatus.LANDING;
		updateShuttlePostures(true);
		Delay.sleepMilli(landTime);
	}
	
	private void updateShuttlePostures(boolean landed) {
		synchronized (points) {
			for (TravelPoint tp : points) {
				CreatureObject shuttle = tp.getShuttle();
				if (shuttle == null) // No associated shuttle
					continue;
				
				shuttle.setPosture(landed ? Posture.UPRIGHT : Posture.PRONE);
			}
		}
	}
	
	public enum ShuttleStatus {
		LANDING,
		GROUNDED,
		LEAVING,
		AWAY
	}
	
}
