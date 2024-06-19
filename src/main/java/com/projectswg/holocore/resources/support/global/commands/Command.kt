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
package com.projectswg.holocore.resources.support.global.commands

import com.projectswg.common.data.CRC
import com.projectswg.common.data.combat.TargetType
import com.projectswg.holocore.resources.support.data.server_info.loader.ValidWeapon

class Command private constructor(builder: CommandBuilder) {
	val name: String = requireNotNull(builder.name)
	val crc: Int = CRC.getCrc(name)

	val cppCallback: String = builder.cppCallback
	val scriptCallback: String = builder.scriptCallback
	val defaultPriority: DefaultPriority = builder.defaultPriority
	val defaultTime: Double = builder.defaultTime
	val characterAbility: String = builder.characterAbility
	val target: Int = builder.target
	val targetType: TargetType = builder.targetType
	val isCallOnTarget: Boolean = builder.callOnTarget
	val maxRange: Double = builder.maxRange
	val godLevel: Int = builder.godLevel
	val isAddToCombatQueue: Boolean = builder.addToCombatQueue
	val validWeapon: ValidWeapon = builder.validWeapon
	val invalidWeapon: Int = builder.invalidWeapon
	val cooldownGroup: String = builder.cooldownGroup
	val warmupTime: Double = builder.warmupTime
	val executeTime: Double = builder.executeTime
	val cooldownTime: Double = builder.cooldownTime
	val cooldownGroup2: String = builder.cooldownGroup2
	val cooldownTime2: Double = builder.cooldownTime2
	val isAutoAddToToolbar: Boolean = builder.autoAddToToolbar
	private val disallowedLocomotions: Set<Locomotion> = builder.disallowedLocomotions
	private val disallowedStates: Set<State> = builder.disallowedStates

	init {
		assert(name == name.lowercase())
	}

	fun getDisallowedLocomotions(): Set<Locomotion> {
		return disallowedLocomotions
	}

	fun getDisallowedStates(): Set<State> {
		return disallowedStates
	}

	override fun toString(): String {
		return "$name:$crc"
	}

	override fun equals(other: Any?): Boolean {
		return other is Command && name == other.name
	}

	override fun hashCode(): Int {
		return crc
	}

	class CommandBuilder {
		var name: String? = null
		var cppCallback: String = ""
		var scriptCallback: String = ""
		var defaultPriority: DefaultPriority = DefaultPriority.NORMAL
		var defaultTime: Double = 0.0
		var characterAbility: String = ""
		var target: Int = 0
		var targetType: TargetType = TargetType.NONE
		var callOnTarget: Boolean = false
		var maxRange: Double = 0.0
		var godLevel: Int = 0
		var addToCombatQueue: Boolean = false
		var validWeapon: ValidWeapon = ValidWeapon.ALL
		var invalidWeapon: Int = 0
		var cooldownGroup: String = ""
		var warmupTime: Double = 0.0
		var executeTime: Double = 0.0
		var cooldownTime: Double = 0.0
		var cooldownGroup2: String = ""
		var cooldownTime2: Double = 0.0
		var autoAddToToolbar: Boolean = false
		var disallowedLocomotions: MutableSet<Locomotion> = HashSet()
		var disallowedStates: MutableSet<State> = HashSet()

		fun withName(name: String?): CommandBuilder {
			this.name = name
			return this
		}

		fun withCppCallback(cppCallback: String): CommandBuilder {
			this.cppCallback = cppCallback
			return this
		}

		fun withScriptCallback(scriptCallback: String): CommandBuilder {
			this.scriptCallback = scriptCallback
			return this
		}

		fun withDefaultPriority(defaultPriority: DefaultPriority): CommandBuilder {
			this.defaultPriority = defaultPriority
			return this
		}

		fun withDefaultTime(defaultTime: Double): CommandBuilder {
			this.defaultTime = defaultTime
			return this
		}

		fun withCharacterAbility(characterAbility: String): CommandBuilder {
			this.characterAbility = characterAbility
			return this
		}

		fun withTarget(target: Int): CommandBuilder {
			this.target = target
			return this
		}

		fun withTargetType(targetType: TargetType): CommandBuilder {
			this.targetType = targetType
			return this
		}

		fun withCallOnTarget(callOnTarget: Boolean): CommandBuilder {
			this.callOnTarget = callOnTarget
			return this
		}

		fun withMaxRange(maxRange: Double): CommandBuilder {
			this.maxRange = maxRange
			return this
		}

		fun withGodLevel(godLevel: Int): CommandBuilder {
			this.godLevel = godLevel
			return this
		}

		fun withAddToCombatQueue(addToCombatQueue: Boolean): CommandBuilder {
			this.addToCombatQueue = addToCombatQueue
			return this
		}

		fun withValidWeapon(validWeapon: ValidWeapon): CommandBuilder {
			this.validWeapon = validWeapon
			return this
		}

		fun withInvalidWeapon(invalidWeapon: Int): CommandBuilder {
			this.invalidWeapon = invalidWeapon
			return this
		}

		fun withCooldownGroup(cooldownGroup: String): CommandBuilder {
			this.cooldownGroup = cooldownGroup
			return this
		}

		fun withWarmupTime(warmupTime: Double): CommandBuilder {
			this.warmupTime = warmupTime
			return this
		}

		fun withExecuteTime(executeTime: Double): CommandBuilder {
			this.executeTime = executeTime
			return this
		}

		fun withCooldownTime(cooldownTime: Double): CommandBuilder {
			this.cooldownTime = cooldownTime
			return this
		}

		fun withCooldownGroup2(cooldownGroup2: String): CommandBuilder {
			this.cooldownGroup2 = cooldownGroup2
			return this
		}

		fun withCooldownTime2(cooldownTime2: Double): CommandBuilder {
			this.cooldownTime2 = cooldownTime2
			return this
		}

		fun withAutoAddToToolbar(autoAddToToolbar: Boolean): CommandBuilder {
			this.autoAddToToolbar = autoAddToToolbar
			return this
		}

		fun withDisallowedLocomotion(locomotion: Locomotion): CommandBuilder {
			disallowedLocomotions.add(locomotion)
			return this
		}

		fun withDisallowedState(state: State): CommandBuilder {
			disallowedStates.add(state)
			return this
		}

		fun build(): Command {
			return Command(this)
		}
	}

	companion object {
		fun builder(): CommandBuilder {
			return CommandBuilder()
		}
	}
}
