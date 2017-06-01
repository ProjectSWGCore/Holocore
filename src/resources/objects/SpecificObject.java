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
package resources.objects;

import services.objects.ObjectCreator;

public enum SpecificObject {
	SO_TRAVEL_TICKET			("object/tangible/travel/travel_ticket/base/shared_base_travel_ticket.iff"),
	SO_TICKET_COLLETOR			("object/tangible/travel/ticket_collector/shared_ticket_collector.iff"),
	SO_TRANSPORT_SHUTTLE		("object/creature/npc/theme_park/shared_player_shuttle.iff"),
	SO_TRANSPORT_STARPORT		("object/creature/npc/theme_park/shared_player_transport.iff"),
	SO_TRANSPORT_STARPORT_THEED	("object/creature/npc/theme_park/shared_player_transport_theed_hangar.iff"),
	SO_WORLD_WAYPOINT			("object/waypoint/shared_world_waypoint.iff");
	
	private final String template;
	
	SpecificObject(String template) {
		this.template = template;
	}
	
	public String getTemplate() {
		return template;
	}
	
	public boolean isType(SWGObject obj) {
		return obj.getTemplate().equals(template);
	}
	
	public SWGObject createType() {
		return ObjectCreator.createObjectFromTemplate(template);
	}
	
}
