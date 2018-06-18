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
	
	private final String callback;
	private final DefaultPriority defaultPriority;
	private final double defaultTime;
	private final String characterAbility;
	private final int target;
	private final TargetType targetType;
	private final boolean callOnTarget;
	private final double maxRange;
	private final int godLevel;
	private final boolean addToCombatQueue;
	private final int validWeapon;
	private final int invalidWeapon;
	private final String cooldownGroup;
	private final double warmupTime;
	private final double executeTime;
	private final double cooldownTime;
	private final String cooldownGroup2;
	private final double cooldownTime2;
	private final boolean autoAddToToolbar;
	
	protected Command(CommandBuilder builder) {
		this.name = builder.name;
		this.crc = CRC.getCrc(name);
		assert name.equals(name.toLowerCase(Locale.US));
		
		this.callback = builder.callback;
		this.defaultPriority = builder.defaultPriority;
		this.defaultTime = builder.defaultTime;
		this.characterAbility = builder.characterAbility;
		this.target = builder.target;
		this.targetType = builder.targetType;
		this.callOnTarget = builder.callOnTarget;
		this.maxRange = builder.maxRange;
		this.godLevel = builder.godLevel;
		this.addToCombatQueue = builder.addToCombatQueue;
		this.validWeapon = builder.validWeapon;
		this.invalidWeapon = builder.invalidWeapon;
		this.cooldownGroup = builder.cooldownGroup;
		this.warmupTime = builder.warmupTime;
		this.executeTime = builder.executeTime;
		this.cooldownTime = builder.cooldownTime;
		this.cooldownGroup2 = builder.cooldownGroup2;
		this.cooldownTime2 = builder.cooldownTime2;
		this.autoAddToToolbar = builder.autoAddToToolbar;
	}
	
	public String getName() {
		return name;
	}
	
	public int getCrc() {
		return crc;
	}
	
	public String getCallback() {
		return callback;
	}
	
	public DefaultPriority getDefaultPriority() {
		return defaultPriority;
	}
	
	public double getDefaultTime() {
		return defaultTime;
	}
	
	public String getCharacterAbility() {
		return characterAbility;
	}
	
	public int getTarget() {
		return target;
	}
	
	public TargetType getTargetType() {
		return targetType;
	}
	
	public boolean isCallOnTarget() {
		return callOnTarget;
	}
	
	public double getMaxRange() {
		return maxRange;
	}
	
	public int getGodLevel() {
		return godLevel;
	}
	
	public boolean isCombatCommand() {
		return false;
	}
	
	public boolean isAddToCombatQueue() {
		return addToCombatQueue;
	}
	
	public int getValidWeapon() {
		return validWeapon;
	}
	
	public int getInvalidWeapon() {
		return invalidWeapon;
	}
	
	public String getCooldownGroup() {
		return cooldownGroup;
	}
	
	public double getWarmupTime() {
		return warmupTime;
	}
	
	public double getExecuteTime() {
		return executeTime;
	}
	
	public double getCooldownTime() {
		return cooldownTime;
	}
	
	public String getCooldownGroup2() {
		return cooldownGroup2;
	}
	
	public double getCooldownTime2() {
		return cooldownTime2;
	}
	
	public boolean isAutoAddToToolbar() {
		return autoAddToToolbar;
	}
	
	@Override
	public String toString() {
		return name + ":" + crc;
	}
	
	@Override
	public boolean equals(Object o) {
		return o instanceof Command && name.equals(((Command) o).name);
	}
	
	@Override
	public int hashCode() {
		return crc;
	}
	
	public static CommandBuilder builder() {
		return new CommandBuilder();
	}
	
	public static class CommandBuilder {
		
		private String name;
		private String callback;
		private DefaultPriority defaultPriority;
		private double defaultTime;
		private String characterAbility;
		private int target;
		private TargetType targetType;
		private boolean callOnTarget;
		private double maxRange;
		private int godLevel;
		private boolean addToCombatQueue;
		private int validWeapon;
		private int invalidWeapon;
		private String cooldownGroup;
		private double warmupTime;
		private double executeTime;
		private double cooldownTime;
		private String cooldownGroup2;
		private double cooldownTime2;
		private boolean autoAddToToolbar;
		
		protected CommandBuilder() {}
		
		protected CommandBuilder(Command command) {
			this.name = command.name;
			this.callback = command.callback;
			this.defaultPriority = command.defaultPriority;
			this.defaultTime = command.defaultTime;
			this.characterAbility = command.characterAbility;
			this.target = command.target;
			this.targetType = command.targetType;
			this.callOnTarget = command.callOnTarget;
			this.maxRange = command.maxRange;
			this.godLevel = command.godLevel;
			this.addToCombatQueue = command.addToCombatQueue;
			this.validWeapon = command.validWeapon;
			this.invalidWeapon = command.invalidWeapon;
			this.cooldownGroup = command.cooldownGroup;
			this.warmupTime = command.warmupTime;
			this.executeTime = command.executeTime;
			this.cooldownTime = command.cooldownTime;
			this.cooldownGroup2 = command.cooldownGroup2;
			this.cooldownTime2 = command.cooldownTime2;
			this.autoAddToToolbar = command.autoAddToToolbar;
		}
		
		public CommandBuilder withName(String name) {
			this.name = name;
			return this;
		}
		
		public CommandBuilder withCallback(String callback) {
			this.callback = callback;
			return this;
		}
		
		public CommandBuilder withDefaultPriority(DefaultPriority defaultPriority) {
			this.defaultPriority = defaultPriority;
			return this;
		}
		
		public CommandBuilder withDefaultTime(double defaultTime) {
			this.defaultTime = defaultTime;
			return this;
		}
		
		public CommandBuilder withCharacterAbility(String characterAbility) {
			this.characterAbility = characterAbility;
			return this;
		}
		
		public CommandBuilder withTarget(int target) {
			this.target = target;
			return this;
		}
		
		public CommandBuilder withTargetType(TargetType targetType) {
			this.targetType = targetType;
			return this;
		}
		
		public CommandBuilder withCallOnTarget(boolean callOnTarget) {
			this.callOnTarget = callOnTarget;
			return this;
		}
		
		public CommandBuilder withMaxRange(double maxRange) {
			this.maxRange = maxRange;
			return this;
		}
		
		public CommandBuilder withGodLevel(int godLevel) {
			this.godLevel = godLevel;
			return this;
		}
		
		public CommandBuilder withAddToCombatQueue(boolean addToCombatQueue) {
			this.addToCombatQueue = addToCombatQueue;
			return this;
		}
		
		public CommandBuilder withValidWeapon(int validWeapon) {
			this.validWeapon = validWeapon;
			return this;
		}
		
		public CommandBuilder withInvalidWeapon(int invalidWeapon) {
			this.invalidWeapon = invalidWeapon;
			return this;
		}
		
		public CommandBuilder withCooldownGroup(String cooldownGroup) {
			this.cooldownGroup = cooldownGroup;
			return this;
		}
		
		public CommandBuilder withWarmupTime(double warmupTime) {
			this.warmupTime = warmupTime;
			return this;
		}
		
		public CommandBuilder withExecuteTime(double executeTime) {
			this.executeTime = executeTime;
			return this;
		}
		
		public CommandBuilder withCooldownTime(double cooldownTime) {
			this.cooldownTime = cooldownTime;
			return this;
		}
		
		public CommandBuilder withCooldownGroup2(String cooldownGroup2) {
			this.cooldownGroup2 = cooldownGroup2;
			return this;
		}
		
		public CommandBuilder withCooldownTime2(double cooldownTime2) {
			this.cooldownTime2 = cooldownTime2;
			return this;
		}
		
		public CommandBuilder withAutoAddToToolbar(boolean autoAddToToolbar) {
			this.autoAddToToolbar = autoAddToToolbar;
			return this;
		}
		
		public Command build() {
			return new Command(this);
		}
		
	}
	
}
