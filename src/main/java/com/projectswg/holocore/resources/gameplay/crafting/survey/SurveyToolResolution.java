/***********************************************************************************
 * Copyright (c) 2019 /// Project SWG /// www.projectswg.com                       *
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

package com.projectswg.holocore.resources.gameplay.crafting.survey;

import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SurveyToolResolution {
	
	private static final double RANGE_START = 64;
	private static final double RANGE_INCREMENT = 64;
	private static final double RESOLUTION_START = 3.5;
	private static final double RESOLUTION_INCREMENT = 0.5;
	
	private final int counter;
	private final int range;
	private final int resolution;
	
	public SurveyToolResolution(int counter, int range, int resolution) {
		this.counter = counter;
		this.range = range;
		this.resolution = resolution;
	}
	
	/**
	 * Returns the skill mod increment required. If the player's skill mod for "surveying" is 20,
	 * the caller should ensure this method does not return a number greater than 1. If the
	 * player's skill mod is 40, the value should be no higher than 2, etc.
	 * @return the surveying skill mod increment
	 */
	public int getCounter() {
		return counter;
	}
	
	/**
	 * Gets the survey range
	 * @return the range, in meters
	 */
	public int getRange() {
		return range;
	}
	
	/**
	 * Gets the survey resolution
	 * @return the survey resolution, in horizontal/vertical points
	 */
	public int getResolution() {
		return resolution;
	}
	
	public static List<SurveyToolResolution> getOptions(@NotNull CreatureObject creature) {
		List<SurveyToolResolution> resolutions = new ArrayList<>();
		int surveyIncrements = creature.getSkillModValue("surveying") / 20;
		
		double range = RANGE_START;
		double resolution = RESOLUTION_START;
		for (int i = 1; i <= surveyIncrements; i++) {
			resolutions.add(new SurveyToolResolution(i, (int) range, (int) resolution));
			range += RANGE_INCREMENT;
			resolution += RESOLUTION_INCREMENT;
		}
		return resolutions;
	}
	
}
