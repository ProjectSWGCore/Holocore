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
package com.projectswg.holocore.resources.commands;

import com.projectswg.common.data.combat.TargetType;

public class Command {
	
	private int crc;
	
	private String name;
	private DefaultPriority defaultPriority;
	private String scriptHook;

	// fail
	private String cppHook;
	private ICmdCallback javaCallback;
	// fail
	private float defaultTime;
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
	private float maxRange;
	private int godLevel;
	// display group
	private boolean combatCommand;
	private boolean addToCombatQueue;
	private int validWeapon;
	private int invalidWeapon;
	private String cooldownGroup;
	private float warmupTime;
	private float executeTime;
	private float cooldownTime;
	private String cooldownGroup2;
	private float cooldownTime2;
	// toolbarOnly
	// fromServerOnly
	private boolean autoAddToToolbar;
	
	public Command(String name) {
		this.name = name;
	}
	
	public int getCrc() { return crc; }
	public void setCrc(int crc) { this.crc = crc; }
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	public DefaultPriority getDefaultPriority() { return defaultPriority; }
	public void setDefaultPriority(DefaultPriority defaultPriority) { this.defaultPriority = defaultPriority; }
	public String getScriptHook() { return scriptHook; }
	public void setScriptHook(String scriptHook) { this.scriptHook = scriptHook; }
	public String getCppHook() { return cppHook; }
	public void setCppHook(String cppHook) { this.cppHook = cppHook; }
	public ICmdCallback getJavaCallback() { return javaCallback; }
	public void setJavaCallback(ICmdCallback javaCallback) { this.javaCallback = javaCallback; }
	public boolean hasJavaCallback() { return javaCallback != null; }
	public float getDefaultTime() { return defaultTime; }
	public void setDefaultTime(float defaultTime) { this.defaultTime = defaultTime; }
	public String getCharacterAbility() { return characterAbility; }
	public void setCharacterAbility(String characterAbility) { this.characterAbility = characterAbility; }
	public int getTarget() { return target; }
	public void setTarget(int target) { this.target = target; }
	public TargetType getTargetType() { return targetType; }
	public void setTargetType(TargetType targetType) { this.targetType = targetType; }
	public boolean isCallOnTarget() { return callOnTarget; }
	public void setCallOnTarget(boolean callOnTarget) { this.callOnTarget = callOnTarget; }
	public float getMaxRange() { return maxRange; }
	public void setMaxRange(float maxRange) { this.maxRange = maxRange; }
	public int getGodLevel() { return godLevel; }
	public void setGodLevel(int godLevel) { this.godLevel = godLevel; }
	public boolean isCombatCommand() { return combatCommand; }
	public void setCombatCommand(boolean combatCommand) { this.combatCommand = combatCommand; }
	public boolean isAddToCombatQueue() { return addToCombatQueue; }
	public void setAddToCombatQueue(boolean addToCombatQueue) { this.addToCombatQueue = addToCombatQueue; }
	public int getValidWeapon() { return validWeapon; }
	public void setValidWeapon(int validWeapon) { this.validWeapon = validWeapon; }
	public int getInvalidWeapon() { return invalidWeapon; }
	public void setInvalidWeapon(int invalidWeapon) { this.invalidWeapon = invalidWeapon; }
	public String getCooldownGroup() { return cooldownGroup; }
	public void setCooldownGroup(String cooldownGroup) { this.cooldownGroup = cooldownGroup; }
	public float getWarmupTime() { return warmupTime; }
	public void setWarmupTime(float warmupTime) { this.warmupTime = warmupTime; }
	public float getExecuteTime() { return executeTime; }
	public void setExecuteTime(float executeTime) { this.executeTime = executeTime; }
	public float getCooldownTime() { return cooldownTime; }
	public void setCooldownTime(float cooldownTime) { this.cooldownTime = cooldownTime; }
	public String getCooldownGroup2() { return cooldownGroup2; }
	public void setCooldownGroup2(String cooldownGroup2) { this.cooldownGroup2 = cooldownGroup2; }
	public float getCooldownTime2() { return cooldownTime2; }
	public void setCooldownTime2(float cooldownTime2) { this.cooldownTime2 = cooldownTime2; }
	public boolean isAutoAddToToolbar() { return autoAddToToolbar; }
	public void setAutoAddToToolbar(boolean autoAddToToolbar) { this.autoAddToToolbar = autoAddToToolbar; }

	public String getDefaultScriptCallback(){ return (scriptHook == null || scriptHook.isEmpty()) ? cppHook : scriptHook; }
	
	@Override
	public String toString() {
		return name + ":" + crc;
	}
}
