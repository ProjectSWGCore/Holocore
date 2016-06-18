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
package intents.radial;

import network.packets.swg.zone.object_controller.ObjectMenuRequest;
import resources.control.Intent;
import resources.objects.SWGObject;
import resources.player.Player;

public class RadialRequestIntent extends Intent {
	
	public static final String TYPE = "RadialRequestIntent";
	
	private Player player;
	private SWGObject target;
	private ObjectMenuRequest request;
	
	public RadialRequestIntent(Player player, SWGObject target, ObjectMenuRequest request) {
		super(TYPE);
		setPlayer(player);
		setTarget(target);
		setRequest(request);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public void setRequest(ObjectMenuRequest request) {
		this.request = request;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
	public ObjectMenuRequest getRequest() {
		return request;
	}
	
}
