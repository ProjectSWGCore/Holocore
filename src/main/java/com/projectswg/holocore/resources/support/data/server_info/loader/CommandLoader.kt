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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.combat.*
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import com.projectswg.holocore.resources.support.data.server_info.loader.ValidWeapon.Companion.getByNum
import com.projectswg.holocore.resources.support.global.commands.*
import me.joshlarson.jlcommon.log.Log
import java.io.File
import java.io.IOException

class CommandLoader internal constructor() : DataLoader() {
	private val commandNameMap: MutableMap<String, Command> = HashMap()
	private val commandCppCallbackMap: MutableMap<String, MutableList<Command>> = HashMap()
	private val commandScriptCallbackMap: MutableMap<String, MutableList<Command>> = HashMap()
	private val commandCrcMap: MutableMap<Int, Command> = HashMap()

	fun isCommand(command: String): Boolean {
		return commandNameMap.containsKey(command)
	}

	fun isCommand(crc: Int): Boolean {
		return commandCrcMap.containsKey(crc)
	}

	fun getCommand(command: String): Command? {
		return commandNameMap[command.lowercase()]
	}

	fun getCommandsByCppCallback(cppCallback: String): List<Command> {
		return commandCppCallbackMap[cppCallback.lowercase()]!!
	}

	fun getCommandsByScriptCallback(scriptCallback: String): List<Command> {
		return commandScriptCallbackMap[scriptCallback.lowercase()]!!
	}

	fun getCommand(crc: Int): Command? {
		return commandCrcMap[crc]
	}

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/command/commands.msdb")).use { set ->
			while (set.next()) {
				val commandBuilder = Command.builder().withName(set.getText("commandName").lowercase()).withCppCallback(set.getText("cppHook")).withScriptCallback(set.getText("scriptHook")).withDefaultPriority(DefaultPriority.getDefaultPriority(set.getText("defaultPriority"))).withDefaultTime(set.getReal("defaultTime")).withCharacterAbility(set.getText("characterAbility")).withGodLevel(set.getInt("godLevel").toInt()).withCooldownGroup(set.getText("cooldownGroup")).withCooldownGroup2(set.getText("cooldownGroup2")).withCooldownTime(set.getReal("cooldownTime")).withCooldownTime2(set.getReal("cooldownTime2")).withWarmupTime(set.getReal("warmupTime")).withExecuteTime(set.getReal("executeTime")).withTargetType(TargetType.getByNum(set.getInt("targetType").toInt())).withValidWeapon(getByNum(set.getInt("validWeapon").toInt()))

				val locomotions = Locomotion.entries.toTypedArray()

				for (locomotion in locomotions) {
					if (!set.getBoolean(locomotion.commandSdbColumnName)) {
						commandBuilder.withDisallowedLocomotion(locomotion)
					}
				}

				val states = State.entries.toTypedArray()

				for (state in states) {
					if (!set.getBoolean(state.commandSdbColumnName)) {
						commandBuilder.withDisallowedState(state)
					}
				}

				val command = commandBuilder.build()
				if (commandNameMap.containsKey(command.name)) {
					Log.w("Duplicate command name [ignoring]: %s", command.name)
					continue
				}
				if (commandCrcMap.containsKey(command.crc)) {
					Log.w("Duplicate command crc [ignoring]: %d [%s]", command.crc, command.name)
					continue
				}
				commandNameMap[command.name] = command
				commandCppCallbackMap.computeIfAbsent(command.cppCallback.lowercase()) { ArrayList() }.add(command)
				commandScriptCallbackMap.computeIfAbsent(command.scriptCallback.lowercase()) { ArrayList() }.add(command)
				commandCrcMap[command.crc] = command
			}
		}
	}
}
