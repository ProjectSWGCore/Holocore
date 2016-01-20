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

import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import services.galaxy.travel.TravelGroup;

public final class TravelPoint implements Comparable<TravelPoint> {
	
	private final String name;
	private final Location location;
	private final boolean reachable;
	private TravelGroup group;
	private CreatureObject shuttle;
	private SWGObject collector;
	private final boolean starport;
	
	public TravelPoint(String name, Location location, boolean starport, boolean reachable) {
		this.name = name;
		this.location = location;
		this.starport = starport;
		this.reachable = reachable;	// Not sure which effect this has on the client.
		this.group = null;
	}
	
	public String getName() {
		return name;
	}
	
	public Location getLocation() {
		return location;
	}
	
	public TravelGroup getGroup() {
		return group;
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
	
	public SWGObject getCollector() {
		return collector;
	}

	public void setShuttle(CreatureObject shuttle) {
		this.shuttle = shuttle;
	}
	
	public void setCollector(SWGObject collector) {
		this.collector = collector;
	}
	
	public void setGroup(TravelGroup group) {
		this.group = group;
	}
	
	public String getSuiFormat() {
		return String.format("@planet_n:%s -- %s", location.getTerrain().getName(), name);
	}
	
	@Override
	public int compareTo(TravelPoint o) {
		int comp = location.getTerrain().compareTo(o.getLocation().getTerrain());
		if (comp != 0)
			return comp;
		return name.compareTo(o.getName());
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof TravelPoint))
			return false;
		return name.equals(((TravelPoint) o).getName());
	}
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
}
