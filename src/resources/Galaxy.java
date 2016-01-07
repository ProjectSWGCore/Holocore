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


public class Galaxy {
	
	// Population status values. Values are in percent.
	private static final double VERY_LIGHT = 10;
	private static final double LIGHT = 20;
	private static final double MEDIUM = 30;
	private static final double HEAVY = 40;
	private static final double VERY_HEAVY = 50;
	private static final double EXTREMELY_HEAVY = 100;
	
	private int id = 0;
	private String name = "";
	private String address = "";
	private int zonePort = 44463;
	private int pingPort = 44462;
	private int population = 0;
	private GalaxyStatus status = GalaxyStatus.DOWN;
	private int timeZone = 0;
	private int maxCharacters = 0;
	private int onlinePlayerLimit = 0;
	private int onlineFreeTrialLimit = 0;
	private boolean recommended = true;
	
	public Galaxy() {
		
	}
	
	public synchronized int    getId()                   { return id; }
	public synchronized String getName()                 { return name; }
	public synchronized String getAddress()              { return address; }
	public synchronized int    getZonePort()             { return zonePort; }
	public synchronized int    getPingPort()             { return pingPort; }
	public synchronized int    getPopulation()           { return population; }
	public synchronized GalaxyStatus getStatus()         { return status; }
	public synchronized int    getTimeZone()             { return timeZone; }
	public synchronized int    getMaxCharacters()        { return maxCharacters; }
	public synchronized int    getOnlinePlayerLimit()    { return onlinePlayerLimit; }
	public synchronized int    getOnlineFreeTrialLimit() { return onlineFreeTrialLimit; }
	public synchronized boolean isRecommended()          { return recommended; }
	
	public synchronized int getPopulationStatus() {
		if (population < VERY_LIGHT)
			return 0;
		else if(population < LIGHT)
			return 1;
		else if(population < MEDIUM)
			return 2;
		else if(population < HEAVY)
			return 3;
		else if(population < VERY_HEAVY)
			return 4;
		else if(population < EXTREMELY_HEAVY)
			return 5;
		return 6;
	}
	
	public synchronized void setId(int id)                    { this.id = id; }
	public synchronized void setName(String name)             { this.name = name; }
	public synchronized void setAddress(String addr)          { this.address = addr; }
	public synchronized void setZonePort(int port)            { this.zonePort = port; }
	public synchronized void setPingPort(int port)            { this.pingPort = port; }
	public synchronized void setPopulation(int population)    { this.population = population; }
	public synchronized void setStatus(GalaxyStatus status)   { this.status = status; }
	public synchronized void setTimeZone(int timeZone)        { this.timeZone = timeZone; }
	public synchronized void setMaxCharacters(int max)        { this.maxCharacters = max; }
	public synchronized void setOnlinePlayerLimit(int max)    { this.onlinePlayerLimit = max; }
	public synchronized void setOnlineFreeTrialLimit(int max) { this.onlineFreeTrialLimit = max; }
	public synchronized void setRecommended(boolean r)        { this.recommended = r; }
	public synchronized void incrementPopulationCount()       { population++; }
	public synchronized void decrementPopulationCount()       { population--; }
	
	public String toString() {
		return String.format("Galaxy[ID=%d Name=%s Address=%s Zone=%d Ping=%d Pop=%d PopStat=%d Status=%s Time=%d Max=%d Rec=%b]",
				id, name, address, zonePort, pingPort, population, getPopulationStatus(), status, timeZone, maxCharacters, recommended);
	}
	
	public void setStatus(int status) {
		for (GalaxyStatus gs : GalaxyStatus.values()) {
			if (gs.getStatus() == status) {
				setStatus(gs);
				return;
			}
		}
	}
	
	public enum GalaxyStatus {
		DOWN		(0x00),
		LOADING		(0x01),
		UP			(0x02),
		LOCKED		(0x03),
		RESTRICTED	(0x04),
		FULL		(0x05);
		
		private byte status;
		
		GalaxyStatus(int status) {
			this.status = (byte) status;
		}
		
		public byte getStatus() {
			return status;
		}
	}
	
}
