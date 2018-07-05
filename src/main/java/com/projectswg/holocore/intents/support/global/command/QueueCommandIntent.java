package com.projectswg.holocore.intents.support.global.command;

import com.projectswg.holocore.resources.support.global.commands.Command;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class QueueCommandIntent extends Intent {
	
	private final CreatureObject source;
	private final SWGObject target;
	private final String arguments;
	private final Command command;
	private final int counter;
	
	public QueueCommandIntent(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull String arguments, @NotNull Command command, int counter) {
		this.source = source;
		this.target = target;
		this.arguments = arguments;
		this.command = command;
		this.counter = counter;
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
	
	public int getCounter() {
		return counter;
	}
	
	public static void broadcast(@NotNull CreatureObject source, @Nullable SWGObject target, @NotNull String arguments, @NotNull Command command, int counter) {
		new QueueCommandIntent(source, target, arguments, command, counter).broadcast();
	}
	
}
