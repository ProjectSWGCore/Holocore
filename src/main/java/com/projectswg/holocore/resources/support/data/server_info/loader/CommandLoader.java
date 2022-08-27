package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.combat.*;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.commands.*;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CommandLoader extends DataLoader {
	
	private final Map<String, Command> commandNameMap;
	private final Map<String, List<Command>> commandCppCallbackMap;
	private final Map<String, List<Command>> commandScriptCallbackMap;
	private final Map<Integer, Command> commandCrcMap;
	
	CommandLoader() {
		this.commandNameMap = new HashMap<>();
		this.commandCppCallbackMap = new HashMap<>();
		this.commandScriptCallbackMap = new HashMap<>();
		this.commandCrcMap = new HashMap<>();
	}
	
	public boolean isCommand(String command) {
		return commandNameMap.containsKey(command);
	}
	
	public boolean isCommand(int crc) {
		return commandCrcMap.containsKey(crc);
	}
	
	public Command getCommand(String command) {
		return commandNameMap.get(command.toLowerCase(Locale.US));
	}
	
	public List<Command> getCommandsByCppCallback(String cppCallback) {
		return commandCppCallbackMap.get(cppCallback.toLowerCase(Locale.US));
	}
	
	public List<Command> getCommandsByScriptCallback(String scriptCallback) {
		return commandScriptCallbackMap.get(scriptCallback.toLowerCase(Locale.US));
	}
	public Command getCommand(int crc) {
		return commandCrcMap.get(crc);
	}
	
	@Override
	public void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/command/commands.msdb"))) {
			while (set.next()) {
				Command.CommandBuilder commandBuilder = Command.builder()
						.withName(set.getText("commandName").toLowerCase(Locale.US))
						.withCppCallback(set.getText("cppHook"))
						.withScriptCallback(set.getText("scriptHook"))
						.withDefaultPriority(DefaultPriority.getDefaultPriority(set.getText("defaultPriority")))
						.withDefaultTime(set.getReal("defaultTime"))
						.withCharacterAbility(set.getText("characterAbility"))
						.withGodLevel((int) set.getInt("godLevel"))
						.withCooldownGroup(set.getText("cooldownGroup"))
						.withCooldownGroup2(set.getText("cooldownGroup2"))
						.withCooldownTime(set.getReal("cooldownTime"))
						.withCooldownTime2(set.getReal("cooldownTime2"))
						.withWarmupTime(set.getReal("warmupTime"))
						.withExecuteTime(set.getReal("executeTime"))
						.withTargetType(TargetType.getTargetType(set.getText("targetType")))
						.withValidWeapon(ValidWeapon.Companion.getByNum((int) set.getInt("validWeapon")));
				
				@NotNull Locomotion[] locomotions = Locomotion.values();
				
				for (Locomotion locomotion : locomotions) {
					if (!set.getBoolean(locomotion.getCommandSdbColumnName())) {
						commandBuilder.withDisallowedLocomotion(locomotion);
					}
				}
				
				State[] states = State.values();
				
				for (State state : states) {
					if (!set.getBoolean(state.getCommandSdbColumnName())) {
						commandBuilder.withDisallowedState(state);
					}
				}
				
				Command command = commandBuilder
						.build();
				if (commandNameMap.containsKey(command.getName())) {
					Log.w("Duplicate command name [ignoring]: %s", command.getName());
					continue;
				}
				if (commandCrcMap.containsKey(command.getCrc())) {
					Log.w("Duplicate command crc [ignoring]: %d [%s]", command.getCrc(), command.getName());
					continue;
				}
				commandNameMap.put(command.getName(), command);
				commandCppCallbackMap.computeIfAbsent(command.getCppCallback().toLowerCase(Locale.US), c -> new ArrayList<>()).add(command);
				commandScriptCallbackMap.computeIfAbsent(command.getScriptCallback().toLowerCase(Locale.US), c -> new ArrayList<>()).add(command);
				commandCrcMap.put(command.getCrc(), command);
			}
		}
	}

}
