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
package resources.commands;

public class Command {
	
	private int crc;
	
	private String name;
	// defaultPriority
	private String scriptCallback;
	// fail
	private ICmdCallback javaCallback;
	// fail
	private float defaultTime;
	private String characterAbility;
	// posture booleans x63
	// temp script
	private int target;
	private int targetType;
	// string id
	// visible - unsure what it's used for (stealth maybe?)
	private boolean callOnTarget;
	// command group
	// disabled
	private int maxRange;
	private int godLevel;
	// display group
	// add to combat queue
	private int validWeapon;
	private int invalidWeapon;
	private String cooldownGroup;
	private int warmupTime;
	private int executeTime;
	private int cooldownTime;
	private String cooldownGroup2;
	private int cooldownTime2;
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
	public String getScriptCallback() { return scriptCallback; }
	public void setScriptCallback(String scriptCallback) { this.scriptCallback = scriptCallback; }
	public ICmdCallback getJavaCallback() { return javaCallback; }
	public void setJavaCallback(ICmdCallback javaCallback) { this.javaCallback = javaCallback; }
	public boolean hasJavaCallback() { return javaCallback != null; }
	public float getDefaultTime() { return defaultTime; }
	public void setDefaultTime(float defaultTime) { this.defaultTime = defaultTime; }
	public String getCharacterAbility() { return characterAbility; }
	public void setCharacterAbility(String characterAbility) { this.characterAbility = characterAbility; }
	public int getTarget() { return target; }
	public void setTarget(int target) { this.target = target; }
	public int getTargetType() { return targetType; }
	public void setTargetType(int targetType) { this.targetType = targetType; }
	public boolean isCallOnTarget() { return callOnTarget; }
	public void setCallOnTarget(boolean callOnTarget) { this.callOnTarget = callOnTarget; }
	public int getMaxRange() { return maxRange; }
	public void setMaxRange(int maxRange) { this.maxRange = maxRange; }
	public int getGodLevel() { return godLevel; }
	public void setGodLevel(int godLevel) { this.godLevel = godLevel; }
	public int getValidWeapon() { return validWeapon; }
	public void setValidWeapon(int validWeapon) { this.validWeapon = validWeapon; }
	public int getInvalidWeapon() { return invalidWeapon; }
	public void setInvalidWeapon(int invalidWeapon) { this.invalidWeapon = invalidWeapon; }
	public String getCooldownGroup() { return cooldownGroup; }
	public void setCooldownGroup(String cooldownGroup) { this.cooldownGroup = cooldownGroup; }
	public int getWarmupTime() { return warmupTime; }
	public void setWarmupTime(int warmupTime) { this.warmupTime = warmupTime; }
	public int getExecuteTime() { return executeTime; }
	public void setExecuteTime(int executeTime) { this.executeTime = executeTime; }
	public int getCooldownTime() { return cooldownTime; }
	public void setCooldownTime(int cooldownTime) { this.cooldownTime = cooldownTime; }
	public String getCooldownGroup2() { return cooldownGroup2; }
	public void setCooldownGroup2(String cooldownGroup2) { this.cooldownGroup2 = cooldownGroup2; }
	public int getCooldownTime2() { return cooldownTime2; }
	public void setCooldownTime2(int cooldownTime2) { this.cooldownTime2 = cooldownTime2; }
	public boolean isAutoAddToToolbar() { return autoAddToToolbar; }
	public void setAutoAddToToolbar(boolean autoAddToToolbar) { this.autoAddToToolbar = autoAddToToolbar; }
	
	@Override
	public String toString() {
		return name + ":" + crc;
	}
}
