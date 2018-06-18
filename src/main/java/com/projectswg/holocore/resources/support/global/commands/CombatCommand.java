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

import com.projectswg.common.data.combat.*;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class CombatCommand extends Command {
	
	private final ValidTarget validTarget;
	private final boolean forceCombat;
	private final Map<WeaponType, String[]> animations;
	private final String [] defaultAnimation;
	private final AttackType attackType;
	private final double healthCost;
	private final double actionCost;
	private final DamageType damageType;
	private final boolean ignoreDistance;
	private final boolean pvpOnly;
	private final int attackRolls;
	private final double percentAddFromWeapon;
	private final int addedDamage;
	private final String buffNameTarget;
	private final String buffNameSelf;
	private final HitType hitType;
	private final String delayAttackEggTemplate;
	private final String delayAttackParticle;
	private final double initialDelayAttackInterval;
	private final double delayAttackInterval;
	private final int delayAttackLoops;
	private final DelayAttackEggPosition eggPosition;
	private final double coneLength;
	private final HealAttrib healAttrib;
	
	private CombatCommand(CombatCommandBuilder builder) {
		super(builder);
		this.validTarget = builder.validTarget;
		this.forceCombat = builder.forceCombat;
		this.animations = builder.animations;
		this.defaultAnimation = Objects.requireNonNull(builder.defaultAnimation, "defaultAnimation");
		this.attackType = builder.attackType;
		this.healthCost = builder.healthCost;
		this.actionCost = builder.actionCost;
		this.damageType = builder.damageType;
		this.ignoreDistance = builder.ignoreDistance;
		this.pvpOnly = builder.pvpOnly;
		this.attackRolls = builder.attackRolls;
		this.percentAddFromWeapon = builder.percentAddFromWeapon;
		this.addedDamage = builder.addedDamage;
		this.buffNameTarget = builder.buffNameTarget;
		this.buffNameSelf = builder.buffNameSelf;
		this.hitType = builder.hitType;
		this.delayAttackEggTemplate = builder.delayAttackEggTemplate;
		this.delayAttackParticle = builder.delayAttackParticle;
		this.initialDelayAttackInterval = builder.initialDelayAttackInterval;
		this.delayAttackInterval = builder.delayAttackInterval;
		this.delayAttackLoops = builder.delayAttackLoops;
		this.eggPosition = builder.eggPosition;
		this.coneLength = builder.coneLength;
		this.healAttrib = builder.healAttrib;
	}
	
	@Override
	public boolean isCombatCommand() {
		return true;
	}
	
	public ValidTarget getValidTarget() {
		return validTarget;
	}
	
	public boolean isForceCombat() {
		return forceCombat;
	}
	
	@NotNull
	public String[] getAnimations(WeaponType type) {
		return animations.getOrDefault(type, new String[0]);
	}
	
	@NotNull
	public String getRandomAnimation(WeaponType type) {
		String [] animations = this.animations.get(type);
		if (animations == null || animations.length == 0)
			animations = defaultAnimation;
		if (animations == null || animations.length == 0)
			return "";
		return animations[(int) (Math.random() * animations.length)];
	}
	
	@NotNull
	public String[] getDefaultAnimation() {
		return defaultAnimation;
	}
	
	public AttackType getAttackType() {
		return attackType;
	}
	
	public double getHealthCost() {
		return healthCost;
	}
	
	public double getActionCost() {
		return actionCost;
	}
	
	public DamageType getDamageType() {
		return damageType;
	}
	
	public boolean isIgnoreDistance() {
		return ignoreDistance;
	}
	
	public boolean isPvpOnly() {
		return pvpOnly;
	}
	
	public int getAttackRolls() {
		return attackRolls;
	}
	
	public double getPercentAddFromWeapon() {
		return percentAddFromWeapon;
	}
	
	public int getAddedDamage() {
		return addedDamage;
	}
	
	public String getBuffNameTarget() {
		return buffNameTarget;
	}
	
	public String getBuffNameSelf() {
		return buffNameSelf;
	}
	
	public HitType getHitType() {
		return hitType;
	}
	
	public String getDelayAttackEggTemplate() {
		return delayAttackEggTemplate;
	}
	
	public String getDelayAttackParticle() {
		return delayAttackParticle;
	}
	
	public double getInitialDelayAttackInterval() {
		return initialDelayAttackInterval;
	}
	
	public double getDelayAttackInterval() {
		return delayAttackInterval;
	}
	
	public int getDelayAttackLoops() {
		return delayAttackLoops;
	}
	
	public DelayAttackEggPosition getEggPosition() {
		return eggPosition;
	}
	
	public double getConeLength() {
		return coneLength;
	}
	
	public HealAttrib getHealAttrib() {
		return healAttrib;
	}
	
	public static CombatCommandBuilder builder() {
		return new CombatCommandBuilder();
	}
	
	public static CombatCommandBuilder builder(Command source) {
		return new CombatCommandBuilder(source);
	}
	
	public static class CombatCommandBuilder extends CommandBuilder {
		
		private final Map<WeaponType, String[]> animations;
		
		private ValidTarget validTarget;
		private boolean forceCombat;
		private String [] defaultAnimation;
		private AttackType attackType;
		private double healthCost;
		private double actionCost;
		private DamageType damageType;
		private boolean ignoreDistance;
		private boolean pvpOnly;
		private int attackRolls;
		private double percentAddFromWeapon;
		private int addedDamage;
		private String buffNameTarget;
		private String buffNameSelf;
		private HitType hitType;
		private String delayAttackEggTemplate;
		private String delayAttackParticle;
		private double initialDelayAttackInterval;
		private double delayAttackInterval;
		private int delayAttackLoops;
		private DelayAttackEggPosition eggPosition;
		private double coneLength;
		private HealAttrib healAttrib;
		
		private CombatCommandBuilder() {
			this.animations = new HashMap<>();
		}
		
		private CombatCommandBuilder(Command command) {
			super(command);
			this.animations = new HashMap<>();
		}
		
		public CombatCommandBuilder withValidTarget(ValidTarget validTarget) {
			this.validTarget = validTarget;
			return this;
		}
		
		public CombatCommandBuilder withForceCombat(boolean forceCombat) {
			this.forceCombat = forceCombat;
			return this;
		}
		
		public CombatCommandBuilder withAnimations(WeaponType type, String[] animations) {
			this.animations.put(type, animations);
			return this;
		}
		
		public CombatCommandBuilder withDefaultAnimation(String[] defaultAnimation) {
			this.defaultAnimation = defaultAnimation;
			return this;
		}
		
		public CombatCommandBuilder withAttackType(AttackType attackType) {
			this.attackType = attackType;
			return this;
		}
		
		public CombatCommandBuilder withHealthCost(double healthCost) {
			this.healthCost = healthCost;
			return this;
		}
		
		public CombatCommandBuilder withActionCost(double actionCost) {
			this.actionCost = actionCost;
			return this;
		}
		
		public CombatCommandBuilder withDamageType(DamageType damageType) {
			this.damageType = damageType;
			return this;
		}
		
		public CombatCommandBuilder withIgnoreDistance(boolean ignoreDistance) {
			this.ignoreDistance = ignoreDistance;
			return this;
		}
		
		public CombatCommandBuilder withPvpOnly(boolean pvpOnly) {
			this.pvpOnly = pvpOnly;
			return this;
		}
		
		public CombatCommandBuilder withAttackRolls(int attackRolls) {
			this.attackRolls = attackRolls;
			return this;
		}
		
		public CombatCommandBuilder withPercentAddFromWeapon(double percentAddFromWeapon) {
			this.percentAddFromWeapon = percentAddFromWeapon;
			return this;
		}
		
		public CombatCommandBuilder withAddedDamage(int addedDamage) {
			this.addedDamage = addedDamage;
			return this;
		}
		
		public CombatCommandBuilder withBuffNameTarget(String buffNameTarget) {
			this.buffNameTarget = buffNameTarget;
			return this;
		}
		
		public CombatCommandBuilder withBuffNameSelf(String buffNameSelf) {
			this.buffNameSelf = buffNameSelf;
			return this;
		}
		
		public CombatCommandBuilder withHitType(HitType hitType) {
			this.hitType = hitType;
			return this;
		}
		
		public CombatCommandBuilder withDelayAttackEggTemplate(String delayAttackEggTemplate) {
			this.delayAttackEggTemplate = delayAttackEggTemplate;
			return this;
		}
		
		public CombatCommandBuilder withDelayAttackParticle(String delayAttackParticle) {
			this.delayAttackParticle = delayAttackParticle;
			return this;
		}
		
		public CombatCommandBuilder withInitialDelayAttackInterval(double initialDelayAttackInterval) {
			this.initialDelayAttackInterval = initialDelayAttackInterval;
			return this;
		}
		
		public CombatCommandBuilder withDelayAttackInterval(double delayAttackInterval) {
			this.delayAttackInterval = delayAttackInterval;
			return this;
		}
		
		public CombatCommandBuilder withDelayAttackLoops(int delayAttackLoops) {
			this.delayAttackLoops = delayAttackLoops;
			return this;
		}
		
		public CombatCommandBuilder withEggPosition(DelayAttackEggPosition eggPosition) {
			this.eggPosition = eggPosition;
			return this;
		}
		
		public CombatCommandBuilder withConeLength(double coneLength) {
			this.coneLength = coneLength;
			return this;
		}
		
		public CombatCommandBuilder withHealAttrib(HealAttrib healAttrib) {
			this.healAttrib = healAttrib;
			return this;
		}
		
		public CombatCommand build() {
			return new CombatCommand(this);
		}
		
	}
}
