/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbRealColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BuffLoader extends DataLoader {
	
	private final Map<CRC, BuffInfo> buffsByCrc;
	
	BuffLoader() {
		this.buffsByCrc = new HashMap<>();
	}
	
	@Nullable
	public BuffInfo getBuff(CRC crc) {
		return buffsByCrc.get(crc);
	}

	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buff/buff.sdb"))) {
			SdbTextColumnArraySet effectParams = set.getTextArrayParser("effect([0-9]+)_param", null);
			SdbRealColumnArraySet effectValues = set.getRealArrayParser("effect([0-9]+)_value", 0);
			while (set.next()) {
				BuffInfo buff = new BuffInfo(set, effectParams, effectValues);
				buffsByCrc.put(buff.getCrc(), buff);
			}
		}
	}
	
	public static class BuffInfo {
		
		private final String name;
		private final CRC crc;
		private final String group1;
		private final String group2;
		private final String block;
		private final int priority;
		private final String icon;
		private final double duration;
		private final String [] effectNames;
		private final double [] effectValues;
		private final String state;
		private final String callback;
		private final String particle;
		private final int visible;
		private final boolean debuff;
		
		public BuffInfo(SdbResultSet set, SdbTextColumnArraySet effectNames, SdbRealColumnArraySet effectValues) {
			this(
					set.getText("name"),
					set.getText("group1"),
					set.getText("group2"),
					set.getText("block"),
					(int) set.getInt("priority"),
					set.getText("icon"),
					set.getReal("duration"),
					effectNames.getArray(set).clone(),
					effectValues.getArray(set).clone(),
					set.getText("state"),
					set.getText("callback"),
					set.getText("particle"),
					(int) set.getInt("visible"),
					set.getBoolean("debuff")
			);
			
			assert this.effectNames.length == this.effectValues.length : "effect params and effect values differ in size";
		}
		
		private BuffInfo(String name, String group1, String group2, String block, int priority, String icon, double duration, String[] effectNames, double[] effectValues, String state, String callback, String particle, int visible, boolean debuff) {
			this.name = name;
			this.crc = new CRC(CRC.getCrc(this.name.toLowerCase(Locale.US)));
			this.group1 = group1;
			this.group2 = group2;
			this.block = block;
			this.priority = priority;
			this.icon = icon;
			this.duration = duration;
			this.effectNames = effectNames;
			this.effectValues = effectValues;
			this.state = state;
			this.callback = callback;
			this.particle = particle;
			this.visible = visible;
			this.debuff = debuff;
		}
		
		public String getName() {
			return name;
		}
		
		public CRC getCrc() {
			return crc;
		}
		
		public String getGroup1() {
			return group1;
		}
		
		public String getGroup2() {
			return group2;
		}
		
		public String getBlock() {
			return block;
		}
		
		public int getPriority() {
			return priority;
		}
		
		public String getIcon() {
			return icon;
		}
		
		public double getDuration() {
			return duration;
		}
		
		public String[] getEffectNames() {
			return effectNames.clone();
		}
		
		public String getEffectName(int index) {
			return effectNames[index];
		}
		
		public double[] getEffectValues() {
			return effectValues.clone();
		}
		
		public double getEffectValue(int index) {
			return effectValues[index];
		}
		
		public int getEffects() {
			return effectNames.length;
		}
		
		public String getState() {
			return state;
		}
		
		public String getCallback() {
			return callback;
		}
		
		public String getParticle() {
			return particle;
		}
		
		public int getVisible() {
			return visible;
		}
		
		public boolean isDebuff() {
			return debuff;
		}
		
	}
	
}
