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
package intents.chat;

import resources.commands.Command;
import resources.control.Intent;
import resources.objects.creature.CreatureObject;

public class ChatCommandIntent extends Intent {
	
	public static final String TYPE = "ChatCommandIntent";
	
	private CreatureObject source;
	private long target;
	private Command command;
	private String [] arguments;
	
	public ChatCommandIntent() {
		super(TYPE);
		setSource(null);
		setTarget(0);
		setCommand(null);
		setArguments(new String[0]);
	}
	
	public ChatCommandIntent(CreatureObject source, long target, Command command, String [] arguments) {
		super(TYPE);
		setSource(source);
		setTarget(target);
		setCommand(command);
		setArguments(arguments);
	}
	
	public void setSource(CreatureObject source) {
		this.source = source;
	}
	
	public void setTarget(long target) {
		this.target = target;
	}
	
	public void setCommand(Command command) {
		this.command = command;
	}
	
	public void setArguments(String [] arguments) {
		this.arguments = new String[arguments.length];
		System.arraycopy(arguments, 0, this.arguments, 0, arguments.length);
	}
	
	public CreatureObject getSource() {
		return source;
	}
	
	public long getTarget() {
		return target;
	}
	
	public Command getCommand() {
		return command;
	}
	
	public String [] getArguments() {
		// I do purposefully return this so you can modify the memory.. but in the future it may be good to change
		return arguments;
	}
	
}
