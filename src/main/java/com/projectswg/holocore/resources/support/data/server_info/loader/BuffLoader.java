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
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbRealColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbColumnArraySet.SdbTextColumnArraySet;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class BuffLoader extends DataLoader {
	
	private final Map<Integer, BuffInfo> buffsByCrc;
	private final Map<String, BuffInfo> buffsByName;
	
	BuffLoader() {
		this.buffsByCrc = new HashMap<>();
		this.buffsByName = new HashMap<>();
	}
	
	@Nullable
	public BuffInfo getBuff(int crc) {
		return buffsByCrc.get(crc);
	}
	
	@Nullable
	public BuffInfo getBuff(CRC crc) {
		return getBuff(crc.getCrc());
	}
	
	@Nullable
	public BuffInfo getBuff(String name) {
		return buffsByName.get(name.toLowerCase(Locale.US));
	}
	
	public boolean containsBuff(int crc) {
		return buffsByCrc.containsKey(crc);
	}
	
	public boolean containsBuff(String name) {
		return buffsByName.containsKey(name.toLowerCase(Locale.US));
	}
	
	@Override
	public final void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/buff/buff.sdb"))) {
			SdbTextColumnArraySet effectParams = set.getTextArrayParser("effect([0-9]+)_param");
			SdbRealColumnArraySet effectValues = set.getRealArrayParser("effect([0-9]+)_value");
			while (set.next()) {
				BuffInfo buff = new BuffInfo(set, effectParams, effectValues);
				buffsByCrc.put(buff.getCrc(), buff);
				buffsByName.put(buff.getName(), buff);
			}
		}
	}
	
	public static class BuffInfo {
		
		private final String name;
		private final int crc;
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
		private final String particleHardpoint;
		private final int visible;
		private final boolean debuff;
		private final String stanceParticle;
		private final boolean dispellPlayer;
		private final boolean removeOnDeath;
		private final boolean playerRemovable;
		private final boolean celestial;
		private final int maxStackCount;
		private final boolean persistent;
		private final boolean displayOrder;
		private final boolean removeOnRespec;
		private final boolean aiRemoveOnCombatEnd;
		private final boolean decayOnPvpDeath;
		
		public BuffInfo(SdbResultSet set, SdbTextColumnArraySet effectNames, SdbRealColumnArraySet effectValues) {
			this.name = set.getText("name").toLowerCase(Locale.US);
			this.crc = CRC.getCrc(this.name);
			this.group1 = set.getText("group1");
			this.group2 = set.getText("group2");
			this.block = set.getText("block");
			this.priority = (int) set.getInt("priority");
			this.icon = set.getText("icon");
			this.duration = set.getReal("duration");
			this.effectNames = effectNames.getArray(set).clone();
			this.effectValues = effectValues.getArray(set).clone();
			this.state = set.getText("state");
			this.callback = set.getText("callback");
			this.particle = set.getText("particle");
			this.particleHardpoint = set.getText("particle_hardpoint");
			this.visible = (int) set.getInt("visible");
			this.debuff = set.getBoolean("debuff");
			this.stanceParticle = set.getText("stance_particle");
			this.dispellPlayer = set.getInt("dispell_player") != 0;
			this.removeOnDeath = set.getInt("remove_on_death") != 0;
			this.playerRemovable = set.getInt("player_removable") != 0;
			this.celestial = set.getInt("is_celestial") != 0;
			this.maxStackCount = (int) set.getInt("max_stacks");
			this.persistent = set.getInt("is_persistent") != 0;
			this.displayOrder = set.getInt("display_order") != 0;
			this.removeOnRespec = set.getInt("remove_on_respec") != 0;
			this.aiRemoveOnCombatEnd = set.getInt("ai_remove_on_combat_end") != 0;
			this.decayOnPvpDeath = set.getInt("decay_on_pvp_death") != 0;
			
			assert this.effectNames.length == this.effectValues.length : "effect params and effect values differ in size";
		}
		
		private BuffInfo(String name, int crc, String group1, String group2, String block, int priority, String icon, double duration, String[] effectNames, double[] effectValues, String state, String callback, String particle, String particleHardpoint, int visible, boolean debuff, String stanceParticle, boolean dispellPlayer, boolean removeOnDeath, boolean playerRemovable, boolean celestial, int maxStackCount, boolean persistent, boolean displayOrder, boolean removeOnRespec, boolean aiRemoveOnCombatEnd, boolean decayOnPvpDeath) {
			this.name = name;
			this.crc = crc;
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
			this.particleHardpoint = particleHardpoint;
			this.visible = visible;
			this.debuff = debuff;
			this.stanceParticle = stanceParticle;
			this.dispellPlayer = dispellPlayer;
			this.removeOnDeath = removeOnDeath;
			this.playerRemovable = playerRemovable;
			this.celestial = celestial;
			this.maxStackCount = maxStackCount;
			this.persistent = persistent;
			this.displayOrder = displayOrder;
			this.removeOnRespec = removeOnRespec;
			this.aiRemoveOnCombatEnd = aiRemoveOnCombatEnd;
			this.decayOnPvpDeath = decayOnPvpDeath;
		}
		
		public String getName() {
			return name;
		}
		
		public int getCrc() {
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
		
		public String getParticleHardpoint() {
			return particleHardpoint;
		}
		
		public int getVisible() {
			return visible;
		}
		
		public boolean isDebuff() {
			return debuff;
		}
		
		public String getStanceParticle() {
			return stanceParticle;
		}
		
		public boolean isDispellPlayer() {
			return dispellPlayer;
		}
		
		public boolean isRemoveOnDeath() {
			return removeOnDeath;
		}
		
		public boolean isPlayerRemovable() {
			return playerRemovable;
		}
		
		public boolean isCelestial() {
			return celestial;
		}
		
		public int getMaxStackCount() {
			return maxStackCount;
		}
		
		public boolean isPersistent() {
			return persistent;
		}
		
		public boolean isDisplayOrder() {
			return displayOrder;
		}
		
		public boolean isRemoveOnRespec() {
			return removeOnRespec;
		}
		
		public boolean isAiRemoveOnCombatEnd() {
			return aiRemoveOnCombatEnd;
		}
		
		public boolean isDecayOnPvpDeath() {
			return decayOnPvpDeath;
		}
		
		public BuffInfoBuilder builder() {
			return new BuffInfoBuilder(this);
		}
		
	}
	
	public static class BuffInfoBuilder {
		
		private String name;
		private int crc;
		private String group1;
		private String group2;
		private String block;
		private int priority;
		private String icon;
		private double duration;
		private String[] effectNames;
		private double[] effectValues;
		private String state;
		private String callback;
		private String particle;
		private String particleHardpoint;
		private int visible;
		private boolean debuff;
		private String stanceParticle;
		private boolean dispellPlayer;
		private boolean removeOnDeath;
		private boolean playerRemovable;
		private boolean celestial;
		private int maxStackCount;
		private boolean persistent;
		private boolean displayOrder;
		private boolean removeOnRespec;
		private boolean aiRemoveOnCombatEnd;
		private boolean decayOnPvpDeath;
		
		private BuffInfoBuilder(BuffInfo buff) {
			this.name = buff.getName();
			this.crc = buff.getCrc();
			this.group1 = buff.getGroup1();
			this.group2 = buff.getGroup2();
			this.block = buff.getBlock();
			this.priority = buff.getPriority();
			this.icon = buff.getIcon();
			this.duration = buff.getDuration();
			this.effectNames = buff.getEffectNames().clone();
			this.effectValues = buff.getEffectValues().clone();
			this.state = buff.getState();
			this.callback = buff.getCallback();
			this.particle = buff.getParticle();
			this.particleHardpoint = buff.getParticleHardpoint();
			this.visible = buff.getVisible();
			this.debuff = buff.isDebuff();
			this.stanceParticle = buff.getStanceParticle();
			this.dispellPlayer = buff.isDispellPlayer();
			this.removeOnDeath = buff.isRemoveOnDeath();
			this.playerRemovable = buff.isPlayerRemovable();
			this.celestial = buff.isCelestial();
			this.maxStackCount = buff.getMaxStackCount();
			this.persistent = buff.isPersistent();
			this.displayOrder = buff.isDisplayOrder();
			this.removeOnRespec = buff.isRemoveOnRespec();
			this.aiRemoveOnCombatEnd = buff.isAiRemoveOnCombatEnd();
			this.decayOnPvpDeath = buff.isDecayOnPvpDeath();
		}
		
		public BuffInfoBuilder setGroup1(String group1) {
			this.group1 = group1;
			return this;
		}
		
		public BuffInfoBuilder setGroup2(String group2) {
			this.group2 = group2;
			return this;
		}
		
		public BuffInfoBuilder setBlock(String block) {
			this.block = block;
			return this;
		}
		
		public BuffInfoBuilder setPriority(int priority) {
			this.priority = priority;
			return this;
		}
		
		public BuffInfoBuilder setIcon(String icon) {
			this.icon = icon;
			return this;
		}
		
		public BuffInfoBuilder setDuration(double duration) {
			this.duration = duration;
			return this;
		}
		
		public BuffInfoBuilder addEffect(String param, double value) {
			String [] names = Arrays.copyOf(effectNames, effectNames.length+1);
			double [] values = Arrays.copyOf(effectValues, effectValues.length+1);
			
			names[names.length-1] = param;
			values[values.length-1] = value;
			return this;
		}
		
		public BuffInfoBuilder setState(String state) {
			this.state = state;
			return this;
		}
		
		public BuffInfoBuilder setCallback(String callback) {
			this.callback = callback;
			return this;
		}
		
		public BuffInfoBuilder setParticle(String particle) {
			this.particle = particle;
			return this;
		}
		
		public BuffInfoBuilder setParticleHardpoint(String particleHardpoint) {
			this.particleHardpoint = particleHardpoint;
			return this;
		}
		
		public BuffInfoBuilder setVisible(int visible) {
			this.visible = visible;
			return this;
		}
		
		public BuffInfoBuilder setDebuff(boolean debuff) {
			this.debuff = debuff;
			return this;
		}
		
		public BuffInfoBuilder setStanceParticle(String stanceParticle) {
			this.stanceParticle = stanceParticle;
			return this;
		}
		
		public BuffInfoBuilder setDispellPlayer(boolean dispellPlayer) {
			this.dispellPlayer = dispellPlayer;
			return this;
		}
		
		public BuffInfoBuilder setRemoveOnDeath(boolean removeOnDeath) {
			this.removeOnDeath = removeOnDeath;
			return this;
		}
		
		public BuffInfoBuilder setPlayerRemovable(boolean playerRemovable) {
			this.playerRemovable = playerRemovable;
			return this;
		}
		
		public BuffInfoBuilder setCelestial(boolean celestial) {
			this.celestial = celestial;
			return this;
		}
		
		public BuffInfoBuilder setMaxStackCount(int maxStackCount) {
			this.maxStackCount = maxStackCount;
			return this;
		}
		
		public BuffInfoBuilder setPersistent(boolean persistent) {
			this.persistent = persistent;
			return this;
		}
		
		public BuffInfoBuilder setDisplayOrder(boolean displayOrder) {
			this.displayOrder = displayOrder;
			return this;
		}
		
		public BuffInfoBuilder setRemoveOnRespec(boolean removeOnRespec) {
			this.removeOnRespec = removeOnRespec;
			return this;
		}
		
		public BuffInfoBuilder setAiRemoveOnCombatEnd(boolean aiRemoveOnCombatEnd) {
			this.aiRemoveOnCombatEnd = aiRemoveOnCombatEnd;
			return this;
		}
		
		public BuffInfoBuilder setDecayOnPvpDeath(boolean decayOnPvpDeath) {
			this.decayOnPvpDeath = decayOnPvpDeath;
			return this;
		}
		
		public BuffInfo createBuffInfo() {
			return null;
		}
	}
}
