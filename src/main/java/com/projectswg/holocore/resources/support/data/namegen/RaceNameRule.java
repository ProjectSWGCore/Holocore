/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.namegen;

import java.util.ArrayList;
import java.util.List;

class RaceNameRule {
	private final List<String> vowels = new ArrayList<>();
	private final List<String> startConsonants = new ArrayList<>();
	private final List<String> endConsonants = new ArrayList<>();
	private final List<String> instructions = new ArrayList<>();
	private int surnameChance = 0;
	private int maxLength = 15;
	
	public RaceNameRule() { }
	
	public void addVowel(String s) {
		vowels.add(s);
	}
	
	public void addStartConsonant(String s) {
		startConsonants.add(s);
	}
	
	public void addEndConsant(String s) {
		endConsonants.add(s);
	}
	
	public void addInstruction(String s) {
		instructions.add(s);
	}
	
	public void setSurnameChance(int chance) {
		this.surnameChance = chance;
	}
	
	public void setMaxLength(int maxLength) {
		this.maxLength = maxLength;
	}
	
	public int getSurnameChance() {
		return surnameChance;
	}
	
	public int getMaxLength() {
		return maxLength;
	}
	
	public List<String> getVowels() {
		return vowels;
	}
	
	public List<String> getStartConsonants() {
		return startConsonants;
	}

	public List<String> getEndConsonants() {
		return endConsonants;
	}

	public List<String> getInstructions() {
		return instructions;
	}
	
}
