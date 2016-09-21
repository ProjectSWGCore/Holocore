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
package resources.spawn;

import java.util.Random;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureDifficulty;
import resources.objects.custom.AIBehavior;

public final class Spawner {

	private final Random random;
	private final SWGObject spawnerObject;
	private String creatureName;
	private String[] iffTemplates;
	private CreatureDifficulty creatureDifficulty;
	private int minRespawnDelay;
	private int maxRespawnDelay;
	private short combatLevel;
	private AIBehavior aiBehavior;
	private int floatRadius;
	private String moodAnimation;
	private int maxHealth;
	private int maxAction;
	private String flagString;

	public Spawner(SWGObject spawnerObject) {
		random = new Random();
		this.spawnerObject = spawnerObject;
	}

	/**
	 * Calculates a random number between {@code minRespawnDelay} and
	 * {@code maxRespawnDelay}
	 * @return a random number between {@code minRespawnDelay} and
	 * {@code maxRespawnDelay}
	 */
	public int getRespawnDelay() {
		return random.nextInt((maxRespawnDelay - minRespawnDelay) + 1) + minRespawnDelay;
	}
	
	/**
	 * Returns a random IFF template 
	 * @return 
	 */
	public String getRandomIffTemplate() {
		return iffTemplates[random.nextInt(iffTemplates.length)];
	}
	
	public SWGObject getSpawnerObject() {
		return spawnerObject;
	}

	public void setIffTemplates(String[] iffTemplates) {
		this.iffTemplates = iffTemplates;
	}

	public String getCreatureName() {
		return creatureName;
	}

	public void setCreatureName(String creatureName) {
		this.creatureName = creatureName;
	}

	public CreatureDifficulty getCreatureDifficulty() {
		return creatureDifficulty;
	}

	public void setCreatureDifficulty(CreatureDifficulty creatureDifficulty) {
		this.creatureDifficulty = creatureDifficulty;
	}

	public int getMinRespawnDelay() {
		return minRespawnDelay;
	}

	public void setMinRespawnDelay(int minRespawnDelay) {
		this.minRespawnDelay = minRespawnDelay;
	}

	public int getMaxRespawnDelay() {
		return maxRespawnDelay;
	}

	public void setMaxRespawnDelay(int maxRespawnDelay) {
		this.maxRespawnDelay = maxRespawnDelay;
	}

	public short getCombatLevel() {
		return combatLevel;
	}

	public void setCombatLevel(short combatLevel) {
		this.combatLevel = combatLevel;
	}

	public AIBehavior getAIBehavior() {
		return aiBehavior;
	}

	public void setAIBehavior(AIBehavior aiBehavior) {
		this.aiBehavior = aiBehavior;
	}

	public int getFloatRadius() {
		return floatRadius;
	}

	public void setFloatRadius(int floatRadius) {
		this.floatRadius = floatRadius;
	}

	public String getMoodAnimation() {
		return moodAnimation;
	}

	public void setMoodAnimation(String moodAnimation) {
		this.moodAnimation = moodAnimation;
	}

	public int getMaxHealth() {
		return maxHealth;
	}

	public void setMaxHealth(int maxHealth) {
		this.maxHealth = maxHealth;
	}

	public int getMaxAction() {
		return maxAction;
	}

	public void setMaxAction(int maxAction) {
		this.maxAction = maxAction;
	}

	public String getFlagString() {
		return flagString;
	}

	public void setFlagString(String flagString) {
		this.flagString = flagString;
	}
	
}
