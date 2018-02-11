/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.intents.radial;

import java.util.List;

import com.projectswg.common.control.Intent;
import com.projectswg.common.data.radial.RadialOption;

import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.player.Player;

public class RadialResponseIntent extends Intent {
	
	private Player player;
	private SWGObject target;
	private List<RadialOption> options;
	private int counter;
	
	public RadialResponseIntent(Player player, SWGObject target, List<RadialOption> options, int counter) {
		setPlayer(player);
		setTarget(target);
		setOptions(options);
		setCounter(counter);
	}
	
	public void setPlayer(Player player) {
		this.player = player;
	}
	
	public void setTarget(SWGObject target) {
		this.target = target;
	}
	
	public void setOptions(List<RadialOption> options) {
		this.options = options;
	}
	
	public void setCounter(int counter) {
		this.counter = counter;
	}
	
	public Player getPlayer() {
		return player;
	}
	
	public SWGObject getTarget() {
		return target;
	}
	
	public List<RadialOption> getOptions() {
		return options;
	}
	
	public int getCounter() {
		return counter;
	}
	
}
