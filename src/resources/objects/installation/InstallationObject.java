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
package resources.objects.installation;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.network.BaselineBuilder;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public class InstallationObject extends TangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private boolean activated	= false;
	private double	power		= 0;
	private double	powerRate	= 0;
	
	public InstallationObject(long objectId) {
		super(objectId, BaselineType.INSO);
	}
	
	public boolean isActivated() {
		return activated;
	}
	
	public double getPower() {
		return power;
	}
	
	public double getPowerRate() {
		return powerRate;
	}
	
	public void setActivated(boolean activated) {
		this.activated = activated;
	}
	
	public void setPower(double power) {
		this.power = power;
	}
	
	public void setPowerRate(double powerRate) {
		this.powerRate = powerRate;
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		
		bb.addBoolean(activated);
		bb.addFloat((float) power);
		bb.addFloat((float) powerRate);
		
		bb.incrementOperandCount(3);
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof InstallationObject))
			return super.equals(o);
		InstallationObject inso = (InstallationObject) o;
		if (!super.equals(o))
			return false;
		return activated == inso.activated && Double.compare(power, inso.power) == 0 && Double.compare(powerRate, inso.powerRate) == 0;
	}
	
	@Override
	public int hashCode() {
		int hash = super.hashCode();
		hash = (hash << 1) | (activated?1:0);
		hash ^= Double.hashCode(power);
		hash ^= Double.hashCode(powerRate);
		return hash;
	}
	
}
