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


public class Attributes {
	
	private int agility = 0;
	private int constitution = 0;
	private int luck = 0;
	private int precision = 0;
	private int stamina = 0;
	private int strength = 0;
	
	public void setAgility(int agility) { this.agility = agility; }
	public void setConstitution(int constitution) { this.constitution = constitution; }
	public void setLuck(int luck) { this.luck = luck; }
	public void setPrecision(int precision) { this.precision = precision; }
	public void setStamina(int stamina) { this.stamina = stamina; }
	public void setStrength(int strength) { this.strength = strength; }
	
	public int getAgility() { return agility; }
	public int getConstitution() { return constitution; }
	public int getLuck() { return luck; }
	public int getPrecision() { return precision; }
	public int getStamina() { return stamina; }
	public int getStrength() { return strength; }
}
