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

package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.CRC;
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public final class PerformanceLoader extends DataLoader {
	
	private final Map<String, PerformanceInfo> nameMap;
	private final Map<Integer, PerformanceInfo> danceMap;
	
	PerformanceLoader() {
		this.nameMap = new HashMap<>();
		this.danceMap = new HashMap<>();
	}
	
	@Nullable
	public PerformanceInfo getPerformanceByName(String performanceName) {
		return nameMap.get(performanceName);
	}
	
	@Nullable
	public PerformanceInfo getPerformanceByDanceId(int danceVisualId) {
		return danceMap.get(danceVisualId);
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/performance/performance.sdb"))) {
			SdbTextColumnArraySet flourishes = set.getTextArrayParser("flourish(%d+)");
			while (set.next()) {
				PerformanceInfo performance = new PerformanceInfo(set, flourishes);
				nameMap.put(performance.getPerformanceName(), performance);
				danceMap.put(performance.getDanceVisualId(), performance);
			}
		}
	}
	
	public static class PerformanceInfo {
		
		private final String performanceName;
		private final int instrumentAudioId;
		private final String requiredSong;
		private final String requiredInstrument;
		private final String requiredDance;
		private final int danceVisualId;
		private final int actionPointsPerLoop;
		private final double loopDuration;
		private final CRC type;
		private final int baseXp;
		private final int flourishXpMod;
		private final int healMindWound;
		private final int healShockWound;
		private final String requiredSkillMod;
		private final int requiredSkillModValue;
		private final String mainloop;
		private final String [] flourishes;
		private final String intro;
		private final String outro;
		
		public PerformanceInfo(SdbResultSet set, SdbTextColumnArraySet flourishes) {
			this.performanceName = set.getText("performance_name");
			this.instrumentAudioId = (int) set.getInt("instrument_audio_id");
			this.requiredSong = set.getText("required_song");
			this.requiredInstrument = set.getText("required_instrument");
			this.requiredDance = set.getText("required_dance");
			this.danceVisualId = (int) set.getInt("dance_visual_id");
			this.actionPointsPerLoop = (int) set.getInt("action_points_per_loop");
			this.loopDuration = set.getReal("loop_duration");
			this.type = new CRC((int) set.getInt("type"));
			this.baseXp = (int) set.getInt("base_xp");
			this.flourishXpMod = (int) set.getInt("flourish_xp_mod");
			this.healMindWound = (int) set.getInt("heal_mind_wound");
			this.healShockWound = (int) set.getInt("heal_shock_wound");
			this.requiredSkillMod = set.getText("required_skill_mod");
			this.requiredSkillModValue = (int) set.getInt("required_skill_mod_value");
			this.mainloop = set.getText("mainloop");
			this.flourishes = flourishes.getArray(set).clone();
			this.intro = set.getText("intro");
			this.outro = set.getText("outro");
		}
		
		public String getPerformanceName() {
			return performanceName;
		}
		
		public int getInstrumentAudioId() {
			return instrumentAudioId;
		}
		
		public String getRequiredSong() {
			return requiredSong;
		}
		
		public String getRequiredInstrument() {
			return requiredInstrument;
		}
		
		public String getRequiredDance() {
			return requiredDance;
		}
		
		public int getDanceVisualId() {
			return danceVisualId;
		}
		
		public int getActionPointsPerLoop() {
			return actionPointsPerLoop;
		}
		
		public double getLoopDuration() {
			return loopDuration;
		}
		
		public CRC getType() {
			return type;
		}
		
		public int getBaseXp() {
			return baseXp;
		}
		
		public int getFlourishXpMod() {
			return flourishXpMod;
		}
		
		public int getHealMindWound() {
			return healMindWound;
		}
		
		public int getHealShockWound() {
			return healShockWound;
		}
		
		public String getRequiredSkillMod() {
			return requiredSkillMod;
		}
		
		public int getRequiredSkillModValue() {
			return requiredSkillModValue;
		}
		
		public String getMainloop() {
			return mainloop;
		}
		
		public String[] getFlourishes() {
			return flourishes.clone();
		}
		
		public String getIntro() {
			return intro;
		}
		
		public String getOutro() {
			return outro;
		}
	}
}
