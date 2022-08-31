/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.gameplay.crafting.resource.galactic;

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.holocore.resources.gameplay.crafting.resource.raw.RawResource;

import java.util.Random;

public class GalacticResourceStats implements MongoPersistable {
	
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
	
	public void generateRandomStats(RawResource resource) {
		Random random = new Random();
		this.coldResistance		= resource.isAttrColdResistance() ? generateRandomNumber(random) : 0;
		this.conductivity		= resource.isAttrConductivity() ? generateRandomNumber(random) : 0;
		this.decayResistance	= resource.isAttrDecayResistance() ? generateRandomNumber(random) : 0;
		this.entangleResistance	= resource.isAttrEntangleResistance() ? generateRandomNumber(random) : 0;
		this.flavor				= resource.isAttrFlavor() ? generateRandomNumber(random) : 0;
		this.heatResistance		= resource.isAttrHeatResistance() ? generateRandomNumber(random) : 0;
		this.malleability		= resource.isAttrMalleability() ? generateRandomNumber(random) : 0;
		this.overallQuality		= resource.isAttrOverallQuality() ? generateRandomNumber(random) : 0;
		this.potentialEnergy	= resource.isAttrPotentialEnergy() ? generateRandomNumber(random) : 0;
		this.shockResistance	= resource.isAttrShockResistance() ? generateRandomNumber(random) : 0;
		this.unitToughness		= resource.isAttrUnitToughness() ? generateRandomNumber(random) : 0;
	}
	
	@Override
	public void readMongo(MongoData data) {
		coldResistance     = data.getInteger("coldResistance", coldResistance);
		conductivity       = data.getInteger("conductivity", conductivity);
		decayResistance    = data.getInteger("decayResistance", decayResistance);
		entangleResistance = data.getInteger("entangleResistance", entangleResistance);
		flavor             = data.getInteger("flavor", flavor);
		heatResistance     = data.getInteger("heatResistance", heatResistance);
		malleability       = data.getInteger("malleability", malleability);
		overallQuality     = data.getInteger("overallQuality", overallQuality);
		potentialEnergy    = data.getInteger("potentialEnergy", potentialEnergy);
		shockResistance    = data.getInteger("shockResistance", shockResistance);
		unitToughness      = data.getInteger("unitToughness", unitToughness);
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putInteger("coldResistance", coldResistance);
		data.putInteger("conductivity", conductivity);
		data.putInteger("decayResistance", decayResistance);
		data.putInteger("entangleResistance", entangleResistance);
		data.putInteger("flavor", flavor);
		data.putInteger("heatResistance", heatResistance);
		data.putInteger("malleability", malleability);
		data.putInteger("overallQuality", overallQuality);
		data.putInteger("potentialEnergy", potentialEnergy);
		data.putInteger("shockResistance", shockResistance);
		data.putInteger("unitToughness", unitToughness);
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
