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
package resources;

import resources.objects.creature.CreatureObject;

public final class TravelPoint {
	
	private final String name;
	private final Location location;
	private final int additionalCost; // Additional cost. Perhaps based on distance from source to destination?
	private final boolean reachable;
	private CreatureObject shuttle;
	private final boolean starport;
	
	public TravelPoint(String name, Location location, int additionalCost, boolean starport, boolean reachable) {
		this.name = name;
		this.location = location;
		this.additionalCost = additionalCost;
		this.starport = starport;
		this.reachable = reachable;	// Not sure which effect this has on the client.
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}

	public int getAdditionalCost() {
		return additionalCost;
	}
	
	public boolean isStarport() {
		return starport;
	}
	
	public boolean isReachable() {
		return reachable;
	}
	
	public CreatureObject getShuttle() {
		return shuttle;
	}

	public void setShuttle(CreatureObject shuttle) {
		this.shuttle = shuttle;
	}
	
	public String getSuiFormat() {
		return String.format("@planet_n:%s -- %s", location.getTerrain().getName(), name);
	}
}
