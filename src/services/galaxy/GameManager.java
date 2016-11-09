/************************************************************************************
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
package services.galaxy;

import resources.control.Manager;
import services.commands.BuffService;
import services.collections.CollectionService;
import services.collections.CollectionBadgeManager;
import services.combat.CombatManager;
import services.commands.CommandService;
import services.commands.EntertainmentService;
import services.experience.ExperienceManager;
import services.faction.FactionService;
import services.galaxy.terminals.TerminalService;
import services.sui.SuiService;
import services.group.GroupService;

public class GameManager extends Manager {

	private final CommandService commandService;
	private final ConnectionService connectionService;
	private final SuiService suiService;
	private final CollectionService collectionService;
	private final CollectionBadgeManager collectionBadgeManager;
	private final EnvironmentService weatherService;
	private final TerminalService terminalManager;
	private final FactionService factionService;
	private final GroupService groupService;
	private final SkillModService skillModService;
	private final EntertainmentService entertainmentService;
	private final CombatManager combatManager;
	private final ExperienceManager experienceManager;
	private final BuffService buffService;

	public GameManager() {
		commandService = new CommandService();
		connectionService = new ConnectionService();
		suiService = new SuiService();
		collectionService = new CollectionService();
		collectionBadgeManager = new CollectionBadgeManager();
		weatherService = new EnvironmentService();
		terminalManager = new TerminalService();
		factionService = new FactionService();
		groupService = new GroupService();
		skillModService = new SkillModService();
		entertainmentService = new EntertainmentService();
		combatManager = new CombatManager();
		experienceManager = new ExperienceManager();
		buffService = new BuffService();

		addChildService(commandService);
		addChildService(connectionService);
		addChildService(suiService);
		addChildService(collectionService);
		addChildService(collectionBadgeManager);
		addChildService(weatherService);
		addChildService(terminalManager);
		addChildService(factionService);
		addChildService(groupService);
		addChildService(skillModService);
		addChildService(entertainmentService);
		addChildService(combatManager);
		addChildService(experienceManager);
		addChildService(buffService);
	}
}
