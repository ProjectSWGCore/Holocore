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
package com.projectswg.holocore.resources.support.objects.swg.creature;

import com.projectswg.common.data.encodables.tangible.SkillMod;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.data.collections.SWGList;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.HashSet;
import java.util.Set;

class CreatureObjectClientServerNP implements Persistable {
	
	private double movementScale = 1;
	private double movementPercent = 1;
	private double walkSpeed = 1.549;
	private double runSpeed = 7.3;
	private double accelScale = 1;
	private double accelPercent = 1;
	private double turnScale = 1;
	private double slopeModAngle = 1;
	private double slopeModPercent = 1;
	private double waterModPercent = 0.75;
	private long performanceListenTarget = 0;
	private int totalLevelXp = 0;
	
	private final SWGSet<String> missionCriticalObjs = new SWGSet<>(4, 13);
	
	private final SWGList<Integer> hamEncumbList = new SWGList<>(4, 2);
	
	private final SWGMap<String, SkillMod> skillMods = new SWGMap<>(4, 3, StringType.ASCII);
	private final SWGMap<String, Integer> abilities = new SWGMap<>(4, 14, StringType.ASCII);
	
	public CreatureObjectClientServerNP() {
		
	}
	
	public void setMovementScale(double movementScale) {
		this.movementScale = movementScale;
	}
	
	public void setMovementPercent(double movementPercent) {
		this.movementPercent = movementPercent;
	}
	
	public void setWalkSpeed(double walkSpeed) {
		this.walkSpeed = walkSpeed;
	}
	
	public void setRunSpeed(double runSpeed) {
		this.runSpeed = runSpeed;
	}
	
	public void setAccelScale(double accelScale) {
		this.accelScale = accelScale;
	}
	
	public void setAccelPercent(double accelPercent) {
		this.accelPercent = accelPercent;
	}
	
	public void setTurnScale(double turnScale) {
		this.turnScale = turnScale;
	}
	
	public void setSlopeModAngle(double slopeModAngle) {
		this.slopeModAngle = slopeModAngle;
	}
	
	public void setSlopeModPercent(double slopeModPercent) {
		this.slopeModPercent = slopeModPercent;
	}
	
	public void setWaterModPercent(double waterModPercent) {
		this.waterModPercent = waterModPercent;
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		this.performanceListenTarget = performanceListenTarget;
	}
	
	public void setTotalLevelXp(int totalLevelXp) {
		this.totalLevelXp = totalLevelXp;
	}
	
	public double getMovementScale() {
		return movementScale;
	}
	
	public double getMovementPercent() {
		return movementPercent;
	}
	
	public double getWalkSpeed() {
		return walkSpeed;
	}
	
	public double getRunSpeed() {
		return runSpeed;
	}
	
	public double getAccelScale() {
		return accelScale;
	}
	
	public double getAccelPercent() {
		return accelPercent;
	}
	
	public double getTurnScale() {
		return turnScale;
	}
	
	public double getSlopeModAngle() {
		return slopeModAngle;
	}
	
	public double getSlopeModPercent() {
		return slopeModPercent;
	}
	
	public double getWaterModPercent() {
		return waterModPercent;
	}
	
	public long getPerformanceListenTarget() {
		return performanceListenTarget;
	}
	
	public int getTotalLevelXp() {
		return totalLevelXp;
	}
	
	public void adjustSkillmod(String skillModName, int base, int modifier, SWGObject target) {
		synchronized (skillMods) {
			SkillMod skillMod = skillMods.get(skillModName);
			
			if (skillMod == null) {
				// They didn't have this SkillMod already.
				// Therefore, we send a full delta.
				skillMods.put(skillModName, new SkillMod(base, modifier));
				skillMods.sendDeltaMessage(target);
			} else {
				// They already had this skillmod.
				// All we need to do is adjust the base and the modifier and send an update from the
// SWGMap
				skillMod.adjustBase(base);
				skillMod.adjustModifier(modifier);
				skillMods.update(skillModName, target);
			}
		}
	}
	
	public int getSkillModValue(String skillModName) {
		synchronized (skillMods) {
			SkillMod skillMod = skillMods.get(skillModName);
			return skillMod != null ? skillMod.getValue() : 0;
		}
	}
	
	public void addAbility(SWGObject target, String ... abilityList) {
		synchronized (abilities) {
			for (String abilityName : abilityList) {
				Integer prev = abilities.get(abilityName);
				if (prev == null)
					prev = 0;
				prev += 1;
				abilities.put(abilityName, prev);
			}
			abilities.sendDeltaMessage(target);
		}
	}
	
	public void removeAbility(String abilityName) {
		synchronized (abilities) {
			Integer i = abilities.remove(abilityName);
			if (i != null && i > 1)
				abilities.put(abilityName, i-1);
		}
	}
	
	public boolean hasAbility(String abilityName) {
		synchronized (abilities) {
			return abilities.containsKey(abilityName);
		}
	}
	
	public Set<String> getAbilityNames() {
		synchronized (abilities) {
			return new HashSet<>(abilities.keySet());
		}
	}
	
	public void createBaseline4(Player target, BaselineBuilder bb) {
		bb.addFloat((float) accelPercent); // 0
		bb.addFloat((float) accelScale); // 1
		bb.addObject(hamEncumbList); // Rename to bonusAttributes? 2
		bb.addObject(skillMods); // 3
		bb.addFloat((float) movementPercent); // 4
		bb.addFloat((float) movementScale); // 5
		bb.addLong(performanceListenTarget); // 6
		bb.addFloat((float) runSpeed); // 7
		bb.addFloat((float) slopeModAngle); // 8
		bb.addFloat((float) slopeModPercent); // 9
		bb.addFloat((float) turnScale); // 10
		bb.addFloat((float) walkSpeed); // 11
		bb.addFloat((float) waterModPercent); // 12
		bb.addObject(missionCriticalObjs); // Group Missions? 13
		bb.addObject(abilities); // 14
		bb.addInt(totalLevelXp); // 15
		
		bb.incrementOperandCount(16);
	}
	
	public void parseBaseline4(NetBuffer buffer) {
		accelPercent = buffer.getFloat();
		accelScale = buffer.getFloat();
		hamEncumbList.addAll(SWGList.getSwgList(buffer, 4, 2, Integer.class));
		skillMods.putAll(SWGMap.getSwgMap(buffer, 4, 3, StringType.ASCII, SkillMod.class));
		movementPercent = buffer.getFloat();
		movementScale = buffer.getFloat();
		performanceListenTarget = buffer.getLong();
		runSpeed = buffer.getFloat();
		slopeModAngle = buffer.getFloat();
		slopeModPercent = buffer.getFloat();
		turnScale = buffer.getFloat();
		walkSpeed = buffer.getFloat();
		waterModPercent = buffer.getFloat();
		missionCriticalObjs.addAll(SWGSet.getSwgSet(buffer, 4, 13, StringType.ASCII));
		abilities.putAll(SWGMap.getSwgMap(buffer, 4, 14, StringType.ASCII, Integer.class));
		totalLevelXp = buffer.getInt();
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(0);
		stream.addFloat((float) accelPercent);
		stream.addFloat((float) accelScale);
		stream.addFloat((float) movementPercent);
		stream.addFloat((float) movementScale);
		stream.addFloat((float) runSpeed);
		stream.addFloat((float) slopeModAngle);
		stream.addFloat((float) slopeModPercent);
		stream.addFloat((float) turnScale);
		stream.addFloat((float) walkSpeed);
		stream.addFloat((float) waterModPercent);
		stream.addInt(totalLevelXp);
		synchronized (hamEncumbList) {
			stream.addList(hamEncumbList, stream::addInt);
		}
		synchronized (skillMods) {
			stream.addMap(skillMods, (e) -> {
				stream.addAscii(e.getKey());
				e.getValue().save(stream);
			});
		}
		synchronized (missionCriticalObjs) {
			stream.addList(missionCriticalObjs, stream::addAscii);
		}
		synchronized (abilities) {
			stream.addMap(abilities, (e) -> {
				stream.addAscii(e.getKey());
				stream.addInt(e.getValue());
			});
		}
	}
	
	@Override
	public void read(NetBufferStream stream) {
		stream.getByte();
		accelPercent = stream.getFloat();
		accelScale = stream.getFloat();
		movementPercent = stream.getFloat();
		movementScale = stream.getFloat();
		runSpeed = stream.getFloat();
		slopeModAngle = stream.getFloat();
		slopeModPercent = stream.getFloat();
		turnScale = stream.getFloat();
		walkSpeed = stream.getFloat();
		waterModPercent = stream.getFloat();
		totalLevelXp = stream.getInt();
		stream.getList((i) -> hamEncumbList.add(stream.getInt()));
		stream.getList((i) -> {
			SkillMod mod = new SkillMod();
			String key = stream.getAscii();
			mod.read(stream);
			skillMods.put(key, mod);
		});
		stream.getList((i) -> missionCriticalObjs.add(stream.getAscii()));
		stream.getList((i) -> abilities.put(stream.getAscii(), stream.getInt()));
	}
	
}
