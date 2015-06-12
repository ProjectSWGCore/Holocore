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
package resources.objects.intangible;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.network.BaselineBuilder;
import resources.objects.SWGObject;
import resources.player.Player;

public class IntangibleObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private int	count	= 0;
	
	public IntangibleObject(long objectId) {
		super(objectId, BaselineType.ITNO);
	}
	
	public IntangibleObject(long objectId, BaselineType objectType) {
		super(objectId, objectType);
	}
	
	public int getCount() {
		return count;
	}
	
	public void setCount(int count) {
		this.count = count;
	}

	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb); // 4 variables
		bb.addInt(count); // 4
		
		bb.incrementOperandCount(1);
	}

	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb); // 2 variables
	}
	
	@Override
	public boolean equals(Object o) {
		return super.equals(o);
	}
	
	@Override
	public int hashCode() {
		return super.hashCode();
	}
}
