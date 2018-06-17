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
package com.projectswg.holocore.resources.support.global.commands;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.combat.TargetType;

import java.util.Locale;

public class Command {
	
	private final String name;
	private final int crc;
	
	private DefaultPriority defaultPriority;
	
	private ICmdCallback javaCallback;
	// fail
	private double defaultTime;
	private String characterAbility;
	// posture booleans x63
	// temp script
	private int target;
	private TargetType targetType;
	// string id
	// visible - unsure what it's used for (stealth maybe?)
	private boolean callOnTarget;
	// command group
	// disabled
	private double maxRange;
	private int godLevel;
	// display group
	private boolean combatCommand;
	private boolean addToCombatQueue;
	private int validWeapon;
	private int invalidWeapon;
	private String cooldownGroup;
	private double warmupTime;
	private double executeTime;
	private double cooldownTime;
	private String cooldownGroup2;
	private double cooldownTime2;
	// toolbarOnly
	// fromServerOnly
	private boolean autoAddToToolbar;
	
	public Command(String name) {
		assert name.equals(name.toLowerCase(Locale.US));
		this.name = name;
		this.crc = CRC.getCrc(name);
	}
	
	public String getName() {
		return name;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public DefaultPriority getDefaultPriority() {
		return defaultPriority;
	}
	
	public void setDefaultPriority(DefaultPriority defaultPriority) {
		this.defaultPriority = defaultPriority;
	}
	
	public ICmdCallback getJavaCallback() {
		return javaCallback;
	}
	
	public void setJavaCallback(ICmdCallback javaCallback) {
		this.javaCallback = javaCallback;
	}
	
	public double getDefaultTime() {
		return defaultTime;
	}
	
	public void setDefaultTime(double defaultTime) {
		this.defaultTime = defaultTime;
	}
	
	public String getCharacterAbility() {
		return characterAbility;
	}
	
	public void setCharacterAbility(String characterAbility) {
		this.characterAbility = characterAbility;
	}
	
	public int getTarget() {
		return target;
	}
	
	public void setTarget(int target) {
		this.target = target;
	}
	
	public TargetType getTargetType() {
		return targetType;
	}
	
	public void setTargetType(TargetType targetType) {
		this.targetType = targetType;
	}
	
	public boolean isCallOnTarget() {
		return callOnTarget;
	}
	
	public void setCallOnTarget(boolean callOnTarget) {
		this.callOnTarget = callOnTarget;
	}
	
	public double getMaxRange() {
		return maxRange;
	}
	
	public void setMaxRange(double maxRange) {
		this.maxRange = maxRange;
	}
	
	public int getGodLevel() {
		return godLevel;
	}
	
	public void setGodLevel(int godLevel) {
		this.godLevel = godLevel;
	}
	
	public boolean isCombatCommand() {
		return combatCommand;
	}
	
	public void setCombatCommand(boolean combatCommand) {
		this.combatCommand = combatCommand;
	}
	
	public boolean isAddToCombatQueue() {
		return addToCombatQueue;
	}
	
	public void setAddToCombatQueue(boolean addToCombatQueue) {
		this.addToCombatQueue = addToCombatQueue;
	}
	
	public int getValidWeapon() {
		return validWeapon;
	}
	
	public void setValidWeapon(int validWeapon) {
		this.validWeapon = validWeapon;
	}
	
	public int getInvalidWeapon() {
		return invalidWeapon;
	}
	
	public void setInvalidWeapon(int invalidWeapon) {
		this.invalidWeapon = invalidWeapon;
	}
	
	public String getCooldownGroup() {
		return cooldownGroup;
	}
	
	public void setCooldownGroup(String cooldownGroup) {
		this.cooldownGroup = cooldownGroup;
	}
	
	public double getWarmupTime() {
		return warmupTime;
	}
	
	public void setWarmupTime(double warmupTime) {
		this.warmupTime = warmupTime;
	}
	
	public double getExecuteTime() {
		return executeTime;
	}
	
	public void setExecuteTime(double executeTime) {
		this.executeTime = executeTime;
	}
	
	public double getCooldownTime() {
		return cooldownTime;
	}
	
	public void setCooldownTime(double cooldownTime) {
		this.cooldownTime = cooldownTime;
	}
	
	public String getCooldownGroup2() {
		return cooldownGroup2;
	}
	
	public void setCooldownGroup2(String cooldownGroup2) {
		this.cooldownGroup2 = cooldownGroup2;
	}
	
	public double getCooldownTime2() {
		return cooldownTime2;
	}
	
	public void setCooldownTime2(double cooldownTime2) {
		this.cooldownTime2 = cooldownTime2;
	}
	
	public boolean isAutoAddToToolbar() {
		return autoAddToToolbar;
	}
	
	public void setAutoAddToToolbar(boolean autoAddToToolbar) {
		this.autoAddToToolbar = autoAddToToolbar;
	}
	
	@Override
	public String toString() {
		return name + ":" + crc;
	}
}
