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
package services.player;

import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.projectswg.common.debug.Log;

import resources.player.Player;

public class CharacterCreationRestriction {
	
	private static final long TIME_INCREMENT = TimeUnit.MINUTES.toMillis(15);
	
	private final Map <Integer, PlayerRestriction> restrictions;
	private int creationsPerPeriod;
	
	public CharacterCreationRestriction(int creationsPerPeriod) {
		this.restrictions = new HashMap<>();
		this.creationsPerPeriod = creationsPerPeriod;
	}
	
	public void setCreationsPerPeriod(int creationsPerPeriod) {
		this.creationsPerPeriod = creationsPerPeriod;
		synchronized (restrictions) {
			for (PlayerRestriction pr : restrictions.values())
				pr.setCreationsPerPeriod(creationsPerPeriod);
		}
	}
	
	public boolean isAbleToCreate(Player player) {
		PlayerRestriction pr = getRestriction(player);
		return pr.isAbleToCreate();
	}
	
	public boolean createdCharacter(Player player) {
		PlayerRestriction pr = getRestriction(player);
		return pr.createdCharacter();
	}
	
	private PlayerRestriction getRestriction(Player player) {
		PlayerRestriction pr;
		synchronized (restrictions) {
			pr = restrictions.get(player.getUserId());
		}
		if (pr == null) {
			pr = new PlayerRestriction(creationsPerPeriod);
			synchronized (restrictions) {
				restrictions.put(player.getUserId(), pr);
			}
		}
		return pr;
	}
	
	private static long now() {
		return System.currentTimeMillis();
	}
	
	private static boolean isWithinPeriod(long time) {
		long cur = now();
		return time > (cur-TIME_INCREMENT) && time <= cur;
	}
	
	private static class PlayerRestriction {
		
		private final Deque <Long> lastCreations;
		private int creationsPerPeriod;
		
		public PlayerRestriction(int creationsPerPeriod) {
			lastCreations = new LinkedList<>();
			setCreationsPerPeriod(creationsPerPeriod);
		}
		
		public void setCreationsPerPeriod(int creationsPerPeriod) {
			this.creationsPerPeriod = creationsPerPeriod;
		}
		
		public boolean isAbleToCreate() {
			synchronized (lastCreations) {
				return lastCreations.size() < creationsPerPeriod || creationsPerPeriod == 0 || !isWithinPeriod(lastCreations.getLast());
			}
		}
		
		public boolean createdCharacter() {
			if (creationsPerPeriod == 0)
				return true;
			synchronized (lastCreations) {
				final boolean hitMax = lastCreations.size() >= creationsPerPeriod;
				final boolean hackSuccess = hitMax && isWithinPeriod(lastCreations.getLast());
				final long time = now();
				if (hackSuccess) {
					final String state = Arrays.toString(lastCreations.toArray(new Long[lastCreations.size()]));
					Log.e("Character created when not allowed! Current time/state: %s/%s", time, state);
				}
				if (hitMax)
					lastCreations.pollLast();
				lastCreations.addFirst(time);
				return !hackSuccess;
			}
		}
		
	}
	
}
