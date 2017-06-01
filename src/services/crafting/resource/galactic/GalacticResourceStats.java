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
package services.crafting.resource.galactic;

import java.util.Random;

import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;

public class GalacticResourceStats implements Persistable {
	
	private int coldResistance;
	private int conductivity;
	private int decayResistance;
	private int entangleResistance;
	private int flavor;
	private int heatResistance;
	private int malleability;
	private int overallQuality;
	private int potentialEnergy;
	private int shockResistance;
	private int unitToughness;
	
	public GalacticResourceStats() {
		this.coldResistance = 0;
		this.conductivity = 0;
		this.decayResistance = 0;
		this.entangleResistance = 0;
		this.flavor = 0;
		this.heatResistance = 0;
		this.malleability = 0;
		this.overallQuality = 0;
		this.potentialEnergy = 0;
		this.shockResistance = 0;
		this.unitToughness = 0;
	}
	
	public void generateRandomStats() {
		Random random = new Random();
		this.coldResistance		= generateRandomNumber(random);
		this.conductivity		= generateRandomNumber(random);
		this.decayResistance	= generateRandomNumber(random);
		this.entangleResistance	= generateRandomNumber(random);
		this.flavor				= generateRandomNumber(random);
		this.heatResistance		= generateRandomNumber(random);
		this.malleability		= generateRandomNumber(random);
		this.overallQuality		= generateRandomNumber(random);
		this.potentialEnergy	= generateRandomNumber(random);
		this.shockResistance	= generateRandomNumber(random);
		this.unitToughness		= generateRandomNumber(random);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addShort(coldResistance);
		stream.addShort(conductivity);
		stream.addShort(decayResistance);
		stream.addShort(entangleResistance);
		stream.addShort(flavor);
		stream.addShort(heatResistance);
		stream.addShort(malleability);
		stream.addShort(overallQuality);
		stream.addShort(potentialEnergy);
		stream.addShort(shockResistance);
		stream.addShort(unitToughness);
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		this.coldResistance		= stream.getShort();
		this.conductivity		= stream.getShort();
		this.decayResistance	= stream.getShort();
		this.entangleResistance	= stream.getShort();
		this.flavor				= stream.getShort();
		this.heatResistance		= stream.getShort();
		this.malleability		= stream.getShort();
		this.overallQuality		= stream.getShort();
		this.potentialEnergy	= stream.getShort();
		this.shockResistance	= stream.getShort();
		this.unitToughness		= stream.getShort();
	}
	
	private int generateRandomNumber(Random random) {
		double x = random.nextDouble();
		return (int) ((0.5 * Math.pow(x, 3) + 0.125 * Math.pow(x + 1, 2)) * 1000);
	}
	
	public int getColdResistance() {
		return coldResistance;
	}
	
	public int getConductivity() {
		return conductivity;
	}
	
	public int getDecayResistance() {
		return decayResistance;
	}
	
	public int getEntangleResistance() {
		return entangleResistance;
	}
	
	public int getFlavor() {
		return flavor;
	}
	
	public int getHeatResistance() {
		return heatResistance;
	}
	
	public int getMalleability() {
		return malleability;
	}
	
	public int getOverallQuality() {
		return overallQuality;
	}
	
	public int getPotentialEnergy() {
		return potentialEnergy;
	}
	
	public int getShockResistance() {
		return shockResistance;
	}
	
	public int getUnitToughness() {
		return unitToughness;
	}
	
	public void setColdResistance(int coldResistance) {
		this.coldResistance = coldResistance;
	}
	
	public void setConductivity(int conductivity) {
		this.conductivity = conductivity;
	}
	
	public void setDecayResistance(int decayResistance) {
		this.decayResistance = decayResistance;
	}
	
	public void setEntangleResistance(int entangleResistance) {
		this.entangleResistance = entangleResistance;
	}
	
	public void setFlavor(int flavor) {
		this.flavor = flavor;
	}
	
	public void setHeatResistance(int heatResistance) {
		this.heatResistance = heatResistance;
	}
	
	public void setMalleability(int malleability) {
		this.malleability = malleability;
	}
	
	public void setOverallQuality(int overallQuality) {
		this.overallQuality = overallQuality;
	}
	
	public void setPotentialEnergy(int potentialEnergy) {
		this.potentialEnergy = potentialEnergy;
	}
	
	public void setShockResistance(int shockResistance) {
		this.shockResistance = shockResistance;
	}
	
	public void setUnitToughness(int unitToughness) {
		this.unitToughness = unitToughness;
	}
}
