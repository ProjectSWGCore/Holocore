package com.projectswg.holocore.intents.support.global.command;

import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExecuteCommandIntent extends Intent {
	
	private final CreatureObject source;
	private final SWGObject target;
	private final String arguments;
	private final Command command;
	
	public ExecuteCommandIntent(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull String arguments, @NotNull Command command) {
		this.source = source;
		this.target = target;
		this.arguments = arguments;
		this.command = command;
	}
	
	@NotNull
	public CreatureObject getSource() {
		return source;
	}
	
	@Nullable
	public SWGObject getTarget() {
		return target;
	}
	
	@NotNull
	public String getArguments() {
		return arguments;
	}
	
	@NotNull
	public Command getCommand() {
		return command;
	}
	
	public static void broadcast(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull String arguments, @NotNull Command command) {
		new ExecuteCommandIntent(source, target, arguments, command).broadcast();
	}
	
}
