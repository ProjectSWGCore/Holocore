/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is an emulation project for Star Wars Galaxies founded on            *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create one or more emulators which will provide servers for      *
 * players to continue playing a game similar to the one they used to play.        *
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
import com.projectswg.holocore.resources.support.data.collections.SWGMap;
import com.projectswg.holocore.resources.support.data.collections.SWGSet;
import com.projectswg.holocore.resources.support.global.network.BaselineBuilder;
import com.projectswg.holocore.resources.support.objects.swg.creature.attributes.AttributesMutable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * CREO 4
 */
@SuppressWarnings("ClassWithTooManyFields") // Required by SWG
class CreatureObjectClientServerNP implements MongoPersistable {
	
	private static final float DEFAULT_RUNSPEED = 5.376f;
	private static final float DEFAULT_WALKSPEED = 1.00625f;
	private static final int DEFAULT_ACCELSCALE = 1;
	private static final int DEFAULT_TURNSCALE = 1;
	private static final int DEFAULT_MOVEMENTSCALE = 1;
	private final CreatureObject obj;
	private final Lock skillModLock = new ReentrantLock();
	private final MovementModifierContainer movementModifierContainer = new MovementModifierContainer();
	
	/** CREO4-00 */ private float								accelPercent			= 1;
	/** CREO4-01 */ private float								accelScale				= DEFAULT_ACCELSCALE;
	/** CREO4-02 */ private final AttributesMutable					bonusAttributes;
	/** CREO4-03 */ private final SWGMap<String, SkillMod>			skillMods				= new SWGMap<>(4, 3, StringType.ASCII);
	/** CREO4-04 */ private	float								movementPercent			= 1;
	/** CREO4-05 */ private float								movementScale			= DEFAULT_MOVEMENTSCALE;
	/** CREO4-06 */ private long								performanceListenTarget	= 0;
	/** CREO4-07 */ private float								runSpeed				= DEFAULT_RUNSPEED;
	/** CREO4-08 */ private float								slopeModAngle			= 1;
	/** CREO4-09 */ private float								slopeModPercent			= 0;
	/** CREO4-10 */ private float								turnScale				= DEFAULT_TURNSCALE;
	/** CREO4-11 */ private float								walkSpeed				= DEFAULT_WALKSPEED;
	/** CREO4-12 */ private float								waterModPercent			= 0.75f;
	/** CREO4-13 */ private final SWGSet<GroupMissionCriticalObject>	missionCriticalObjects	= new SWGSet<>(4, 13);
	/** CREO4-14 */ private final SWGMap<String, Integer>				commands				= new SWGMap<>(4, 14, StringType.ASCII);
	
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

	public void adjustSkillmod(@NotNull String skillModName, int base, int modifier) {
		if (base == 0 && modifier == 0) {
			return;
		}
		
		skillModLock.lock();
		try {
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
		} finally {
			skillModLock.unlock();
		}
	}
	
	public Map<String, Integer> getSkillMods() {
		Map<String, Integer> skillMods = new HashMap<>(this.skillMods.size());
		skillModLock.lock();
		try {
			for (Map.Entry<String, SkillMod> entry : this.skillMods.entrySet())
				skillMods.put(entry.getKey(), entry.getValue().getValue());
		} finally {
			skillModLock.unlock();
		}
		return skillMods;
	}
	
	public int getSkillModValue(@NotNull String skillModName) {
		SkillMod skillMod = skillMods.get(skillModName);
		return skillMod != null ? skillMod.getValue() : 0;
	}

	public float getMovementPercent() {
		return movementPercent;
	}

	public void setMovementPercent(float movementPercent) {
		assert(movementPercent >= 0 && movementPercent <= 1);	// movementPercent should only be used for snares and roots
		
		this.movementPercent = movementPercent;
		sendDelta(4, movementPercent);
	}

	public float getMovementScale() {
		return movementScale;
	}

	public void setMovementScale(MovementModifierIdentifier movementModifierIdentifier, float movementScale, boolean fromMount) {
		assert(movementScale >= 1);	// Should only be used for speed boosts
		float fastestMovementModifier = movementModifierContainer.putModifier(movementModifierIdentifier, movementScale, fromMount);
		
		this.movementScale = fastestMovementModifier;
		sendDelta(5, fastestMovementModifier);
	}
	
	public void removeMovementScale(MovementModifierIdentifier movementModifierIdentifier) {
		float fastestMovementModifier = movementModifierContainer.removeModifier(movementModifierIdentifier);
		
		this.movementScale = fastestMovementModifier;
		sendDelta(5, fastestMovementModifier);
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

	public void addCommands(@NotNull List<String> commands) {
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

	public void removeCommands(@NotNull List<String> commands) {
		// TODO: Replace with compute
		for (String command : commands) {
			int nVal = this.commands.getOrDefault(command, 0)-1;
			if (nVal <= 0)
				this.commands.remove(command);
			else
				this.commands.put(command, nVal);
		}

		this.commands.sendDeltaMessage(obj);
	}

	public boolean hasCommand(@NotNull String command) {
		return commands.containsKey(command);
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

		bb.incrementOperandCount(15);
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
		data.putDocument("movementModifiers", movementModifierContainer);
	}

	@Override
	public void readMongo(MongoData data) {
		commands.clear();
		skillMods.clear();
		missionCriticalObjects.clear();

		accelPercent = data.getFloat("accelPercent", accelPercent);
		accelScale = data.getFloat("accelScale", accelScale);
		data.getDocument("bonusAttributes", bonusAttributes);
		skillMods.putAll(data.getMap("skillMods", String.class, SkillMod.class));
		movementPercent = data.getFloat("movementPercent", movementPercent);
		movementScale = data.getFloat("movementScale", accelScale);
		// performance listen target
		slopeModPercent = data.getFloat("slopeModPercent", slopeModPercent);
		slopeModAngle = data.getFloat("slopeModAngle", accelScale);
		waterModPercent = data.getFloat("waterModPercent", waterModPercent);
		runSpeed = data.getFloat("runSpeed", runSpeed);
		walkSpeed = data.getFloat("walkSpeed", walkSpeed);
		turnScale = data.getFloat("turnScale", turnScale);
		commands.putAll(data.getMap("commands", String.class, Integer.class));
		missionCriticalObjects.addAll(data.getArray("missionCriticalObjects", GroupMissionCriticalObject.class));
		movementModifierContainer.readMongo(data.getDocument("movementModifiers"));
	}

	private void sendDelta(int update, Object o) {
		obj.sendDelta(4, update, o);
	}
	
	public void resetMovement() {
		setWalkSpeed(DEFAULT_WALKSPEED);
		setRunSpeed(DEFAULT_RUNSPEED);
		setAccelScale(DEFAULT_ACCELSCALE);
		setTurnScale(DEFAULT_TURNSCALE);
		setMovementScale(MovementModifierIdentifier.BASE, DEFAULT_MOVEMENTSCALE, false);
	}
}
