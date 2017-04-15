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
package intents.travel;

import com.projectswg.common.control.Intent;

import resources.objects.creature.CreatureObject;

public final class TicketPurchaseIntent extends Intent {
	
	private final CreatureObject purchaser;
	private final boolean roundTrip;
	private final String destinationName;
	private final String destinationPlanet;
	
	public TicketPurchaseIntent(CreatureObject purchaser, String destinationPlanet, String destinationName, boolean roundTrip) {
		this.purchaser = purchaser;
		this.destinationPlanet = destinationPlanet;
		this.destinationName = destinationName;
		this.roundTrip = roundTrip;
	}
	
	public CreatureObject getPurchaser() {
		return purchaser;
	}
	
	public boolean isRoundTrip() {
		return roundTrip;
	}
	
	public String getDestinationPlanet() {
		return destinationPlanet;
	}
	
	public String getDestinationName() {
		return destinationName;
	}
	
}
