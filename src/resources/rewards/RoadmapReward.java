/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *
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
 ************************************************************************************/

package resources.rewards;

/**
 * Created by skylerlehan on 9/15/16.
 */
public class RoadmapReward {
	private String roadmapTemplate;
	private String roadmapSkillName;
	private String appearanceName;
	private String stringId;
	private String itemDefault;
	private String itemWookiee;
	private String itemIthorian;
	private boolean isUniversalReward; // This means the reward is for Wookiees, Ithorians and everything else

	private String[] defaultRewardItems;
	private String[] wookieeRewardItems;
	private String[] ithorianRewardItems;

	public RoadmapReward(String roadmapTemplate, String roadmapSkillName, String appearanceName, String stringId, String itemDefault, String itemWookiee, String itemIthorian) {
		this.roadmapTemplate = roadmapTemplate;
		this.roadmapSkillName = roadmapSkillName;
		this.appearanceName = appearanceName;
		this.stringId = stringId;
		this.itemDefault = itemDefault;
		this.itemWookiee = itemWookiee;
		this.itemIthorian = itemIthorian;

		parseItems();
	}

	public String getRoadmapTemplate() {
		return roadmapTemplate;
	}

	public String getRoadmapSkillName() {
		return roadmapSkillName;
	}

	public String getAppearanceName() {
		return appearanceName;
	}

	public String getStringId() {
		return stringId;
	}

	public String getItemDefault() {
		return itemDefault;
	}

	public String getItemWookiee() {
		return itemWookiee;
	}

	public String getItemIthorian() {
		return itemIthorian;
	}

	public boolean isUniversalReward() {
		return isUniversalReward;
	}

	public String[] getDefaultRewardItems() {
		return defaultRewardItems;
	}

	public String[] getWookieeRewardItems() {
		return wookieeRewardItems;
	}

	public String[] getIthorianRewardItems() {
		return ithorianRewardItems;
	}

	private void parseItems() {
		defaultRewardItems = itemDefault.split(",");
		wookieeRewardItems = itemWookiee.split(",");
		ithorianRewardItems = itemIthorian.split(",");

		isUniversalReward = !itemDefault.isEmpty() && (itemWookiee.isEmpty() && itemIthorian.isEmpty());
	}
}
