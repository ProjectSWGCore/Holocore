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

import com.projectswg.common.data.encodables.mongo.MongoData;
import com.projectswg.common.data.encodables.mongo.MongoPersistable;
import com.projectswg.common.data.encodables.tangible.SkillMod;
import com.projectswg.common.encoding.StringType;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.NetBufferStream;
import com.projectswg.common.persistable.Persistable;
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.objects.swg.creature.attributes.Attributes;
import com.projectswg.holocore.resources.support.objects.swg.creature.attributes.AttributesMutable;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * CREO 4
 */
@SuppressWarnings("ClassWithTooManyFields") // Required by SWG
class CreatureObjectClientServerNP implements Persistable, MongoPersistable {
	
	private final CreatureObject obj;
	
	/** CREO4-00 */ private float								accelPercent			= 1;
	/** CREO4-01 */ private float								accelScale				= 1;
	/** CREO4-02 */ private AttributesMutable					bonusAttributes;
	/** CREO4-03 */ private SWGMap<String, SkillMod>			skillMods				= new SWGMap<>(4, 3);
	/** CREO4-04 */ private	float								movementPercent			= 1;
	/** CREO4-05 */ private float								movementScale			= 1;
	/** CREO4-06 */ private long								performanceListenTarget	= 0;
	/** CREO4-07 */ private float								runSpeed				= 7.3f;
	/** CREO4-08 */ private float								slopeModAngle			= 1;
	/** CREO4-09 */ private float								slopeModPercent			= 1;
	/** CREO4-10 */ private float								turnScale				= 1;
	/** CREO4-11 */ private float								walkSpeed				= 1.549f;
	/** CREO4-12 */ private float								waterModPercent			= 0.75f;
	/** CREO4-13 */ private SWGSet<GroupMissionCriticalObject>	missionCriticalObjects	= new SWGSet<>(4, 13);
	/** CREO4-14 */ private SWGMap<String, Integer>				commands				= new SWGMap<>(4, 14, StringType.ASCII);
	/** CREO4-15 */ private int									totalLevelXp			= 0;
	
	public CreatureObjectClientServerNP(@NotNull CreatureObject obj) {
		this.obj = obj;
		this.bonusAttributes = new AttributesMutable(obj, 4, 2);
	}
	
	public float getAccelPercent() {
		return accelPercent;
	}
	
	public void setAccelPercent(float accelPercent) {
		this.accelPercent = accelPercent;
		sendDelta(0, accelPercent);
	}
	
	public float getAccelScale() {
		return accelScale;
	}
	
	public void setAccelScale(float accelScale) {
		this.accelScale = accelScale;
		sendDelta(1, accelScale);
	}
	
	@NotNull
	public Attributes getBonusAttributes() {
		return bonusAttributes.getImmutable();
	}
	
	// TODO: Add Bonus Attribute Setters
	
	public void adjustSkillmod(@NotNull String skillModName, int base, int modifier) {
		SkillMod skillMod = skillMods.get(skillModName);
		
		if (skillMod == null) {
			// They didn't have this SkillMod already.
			skillMods.put(skillModName, new SkillMod(base, modifier));
		} else {
			// They already had this skillmod.
			// All we need to do is adjust the base and the modifier and send an update
			skillMod.adjustBase(base);
			skillMod.adjustModifier(modifier);
			skillMods.update(skillModName);
		}
		skillMods.sendDeltaMessage(obj);
	}
	
	public int getSkillModValue(@NotNull String skillModName) {
		SkillMod skillMod = skillMods.get(skillModName);
		return skillMod != null ? skillMod.getValue() : 0;
	}
	
	public float getMovementPercent() {
		return movementPercent;
	}
	
	public void setMovementPercent(float movementPercent) {
		this.movementPercent = movementPercent;
		sendDelta(4, movementPercent);
	}
	
	public float getMovementScale() {
		return movementScale;
	}
	
	public void setMovementScale(float movementScale) {
		this.movementScale = movementScale;
		sendDelta(5, movementScale);
	}
	
	public long getPerformanceListenTarget() {
		return performanceListenTarget;
	}
	
	public void setPerformanceListenTarget(long performanceListenTarget) {
		this.performanceListenTarget = performanceListenTarget;
		sendDelta(6, performanceListenTarget);
	}
	
	public float getRunSpeed() {
		return runSpeed;
	}
	
	public void setRunSpeed(float runSpeed) {
		this.runSpeed = runSpeed;
		sendDelta(7, runSpeed);
	}
	
	public float getSlopeModAngle() {
		return slopeModAngle;
	}
	
	public void setSlopeModAngle(float slopeModAngle) {
		this.slopeModAngle = slopeModAngle;
		sendDelta(8, slopeModAngle);
	}
	
	public float getSlopeModPercent() {
		return slopeModPercent;
	}
	
	public void setSlopeModPercent(float slopeModPercent) {
		this.slopeModPercent = slopeModPercent;
		sendDelta(9, slopeModPercent);
	}
	
	public float getTurnScale() {
		return turnScale;
	}
	
	public void setTurnScale(float turnScale) {
		this.turnScale = turnScale;
		sendDelta(10, turnScale);
	}
	
	public float getWalkSpeed() {
		return walkSpeed;
	}
	
	public void setWalkSpeed(float walkSpeed) {
		this.walkSpeed = walkSpeed;
		sendDelta(11, walkSpeed);
	}
	
	public float getWaterModPercent() {
		return waterModPercent;
	}
	
	public void setWaterModPercent(float waterModPercent) {
		this.waterModPercent = waterModPercent;
		sendDelta(12, waterModPercent);
	}
	
	@NotNull
	public Set<GroupMissionCriticalObject> getMissionCriticalObjects() {
		return Collections.unmodifiableSet(missionCriticalObjects);
	}
	
	public void setMissionCriticalObjects(@NotNull Set<GroupMissionCriticalObject> missionCriticalObjects) {
		this.missionCriticalObjects.clear();
		this.missionCriticalObjects.addAll(missionCriticalObjects);
		this.missionCriticalObjects.sendDeltaMessage(obj);
	}
	
	@NotNull
	public Set<String> getCommands() {
		return Collections.unmodifiableSet(commands.keySet());
	}
	
	public void addCommand(@NotNull String command) {
		// TODO: Replace with compute
		commands.put(command, commands.getOrDefault(command, 0)+1);
		commands.sendDeltaMessage(obj);
	}
	
	public void addCommands(@NotNull String ... commands) {
		for (String command : commands)
			this.commands.put(command, this.commands.getOrDefault(command, 0)+1);
		this.commands.sendDeltaMessage(obj);
	}
	
	public void removeCommand(@NotNull String command) {
		// TODO: Replace with compute
		int nVal = commands.getOrDefault(command, 0)-1;
		if (nVal <= 0)
			commands.remove(command);
		else
			commands.put(command, nVal);
		commands.sendDeltaMessage(obj);
	}
	
	public boolean hasCommand(@NotNull String command) {
		return commands.containsKey(command);
	}
	
	public int getTotalLevelXp() {
		return totalLevelXp;
	}
	
	public void setTotalLevelXp(int totalLevelXp) {
		this.totalLevelXp = totalLevelXp;
		sendDelta(15, totalLevelXp);
	}
	
	public void createBaseline4(BaselineBuilder bb) {
		bb.addFloat(accelPercent); // 0
		bb.addFloat(accelScale); // 1
		bb.addObject(bonusAttributes); // 2
		bb.addObject(skillMods); // 3
		bb.addFloat(movementPercent); // 4
		bb.addFloat(movementScale); // 5
		bb.addLong(performanceListenTarget); // 6
		bb.addFloat(runSpeed); // 7
		bb.addFloat(slopeModAngle); // 8
		bb.addFloat(slopeModPercent); // 9
		bb.addFloat(turnScale); // 10
		bb.addFloat(walkSpeed); // 11
		bb.addFloat(waterModPercent); // 12
		bb.addObject(missionCriticalObjects); // 13
		bb.addObject(commands); // 14
		bb.addInt(totalLevelXp); // 15
		
		bb.incrementOperandCount(16);
	}
	
	public void parseBaseline4(NetBuffer buffer) {
		skillMods.clear();
		missionCriticalObjects.clear();
		commands.clear();
		
		accelPercent = buffer.getFloat();
		accelScale = buffer.getFloat();
		bonusAttributes.decode(buffer);
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
		missionCriticalObjects.addAll(SWGSet.getSwgSet(buffer, 4, 13, GroupMissionCriticalObject.class));
		commands.putAll(SWGMap.getSwgMap(buffer, 4, 14, StringType.ASCII, Integer.class));
		totalLevelXp = buffer.getInt();
	}
	
	@Override
	public void saveMongo(MongoData data) {
		data.putDouble("accelPercent", accelPercent);
		data.putDouble("accelScale", accelScale);
		data.putDocument("bonusAttributes", bonusAttributes);
		data.putMap("skillMods", skillMods);
		data.putDouble("movementPercent", movementPercent);
		data.putDouble("movementScale", movementScale);
		// performance listen target
		data.putDouble("runSpeed", runSpeed);
		data.putDouble("slopeModAngle", slopeModAngle);
		data.putDouble("slopeModPercent", slopeModPercent);
		data.putDouble("turnScale", turnScale);
		data.putDouble("walkSpeed", walkSpeed);
		data.putDouble("waterModPercent", waterModPercent);
		data.putMap("commands", commands);
		data.putArray("missionCriticalObjects", missionCriticalObjects);
		data.putInteger("totalLevelXp", totalLevelXp);
	}
	
	@Override
	public void readMongo(MongoData data) {
		commands.clear();
		skillMods.clear();
		missionCriticalObjects.clear();
		
		accelPercent = data.getFloat("accelPercent", accelPercent);
		accelScale = data.getFloat("accelScale", accelScale);
		data.getDocument("bonusAttributes", bonusAttributes);
		skillMods.putAll(data.getMap("skillMods", SkillMod.class, SkillMod::new));
		movementPercent = data.getFloat("movementPercent", movementPercent);
		movementScale = data.getFloat("movementScale", accelScale);
		// performance listen target
		slopeModPercent = data.getFloat("slopeModPercent", slopeModPercent);
		slopeModAngle = data.getFloat("slopeModAngle", accelScale);
		waterModPercent = data.getFloat("waterModPercent", waterModPercent);
		runSpeed = data.getFloat("runSpeed", runSpeed);
		walkSpeed = data.getFloat("walkSpeed", walkSpeed);
		turnScale = data.getFloat("turnScale", turnScale);
		commands.putAll(data.getMap("commands", Integer.class));
		missionCriticalObjects.addAll(data.getArray("missionCriticalObjects", GroupMissionCriticalObject.class));
		totalLevelXp = data.getInteger("totalLevelXp", totalLevelXp);
	}
	
	@Override
	public void save(NetBufferStream stream) {
		stream.addByte(1);
		stream.addFloat(accelPercent);
		stream.addFloat(accelScale);
		stream.addFloat(movementPercent);
		stream.addFloat(movementScale);
		stream.addFloat(runSpeed);
		stream.addFloat(slopeModAngle);
		stream.addFloat(slopeModPercent);
		stream.addFloat(turnScale);
		stream.addFloat(walkSpeed);
		stream.addFloat(waterModPercent);
		stream.addInt(totalLevelXp);
		bonusAttributes.save(stream);
		stream.addMap(skillMods, (e) -> {
			stream.addAscii(e.getKey());
			e.getValue().save(stream);
		});
		stream.addMap(commands, (e) -> {
			stream.addAscii(e.getKey());
			stream.addInt(e.getValue());
		});
	}
	
	@Override
	public void read(NetBufferStream stream) {
		byte ver = stream.getByte();
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
		if (ver == 0) {
			int [] array = new int[6];
			stream.getList((i) -> array[i] = stream.getInt());
			bonusAttributes.setHealth(array[0]);
			bonusAttributes.setHealthRegen(array[1]);
			bonusAttributes.setAction(array[2]);
			bonusAttributes.setActionRegen(array[3]);
			bonusAttributes.setMind(array[4]);
			bonusAttributes.setMindRegen(array[5]);
		} else {
			bonusAttributes.read(stream);
		}
		stream.getList((i) -> {
			SkillMod mod = new SkillMod();
			String key = stream.getAscii();
			mod.read(stream);
			skillMods.put(key, mod);
		});
		if (ver == 0)
			stream.getList(i -> stream.getAscii());
		stream.getList((i) -> commands.put(stream.getAscii(), stream.getInt()));
	}
	
	private void sendDelta(int update, Object o) {
		obj.sendDelta(4, update, o);
	}
	
}
