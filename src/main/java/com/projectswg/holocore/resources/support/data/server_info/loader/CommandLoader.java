package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.combat.TargetType;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader.SdbResultSet;
import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.global.commands.DefaultPriority;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class CommandLoader extends DataLoader {
	
	private final Map<String, Command> commands;
	
	CommandLoader() {
		this.commands = new HashMap<>();
	}
	
	public Command getCommand(String command) {
		return commands.get(command);
	}
	
	@Override
	protected void load() throws IOException {
		try (SdbResultSet set = SdbLoader.load(new File("serverdata/command/commands.msdb"))) {
			while (set.next()) {
				Command command = new Command(set.getText("name").toLowerCase(Locale.US));
				command.setDefaultPriority(DefaultPriority.getDefaultPriority(set.getText("defaultPriority")));
				command.setDefaultTime(set.getReal("defaultTime"));
				command.setCharacterAbility(set.getText("characterAbility"));
				command.setCombatCommand(false);
				command.setGodLevel((int) set.getInt("godLevel"));
				command.setCooldownGroup(set.getText("cooldownGroup"));
				command.setCooldownGroup2(set.getText("cooldownGroup2"));
				command.setCooldownTime(set.getReal("cooldownTime"));
				command.setCooldownTime2(set.getReal("cooldownTime2"));
				command.setTargetType(TargetType.getTargetType(set.getText("targetType")));
				command.setAddToCombatQueue(set.getBoolean("addToCombatQueue"));
			}
		}
	}
}
