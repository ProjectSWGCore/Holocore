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
package resources.radial;

import com.projectswg.common.debug.Log;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Scripts;

import java.util.ArrayList;
import java.util.List;

public class Radials {
	
	private static final String SCRIPT_PREFIX = "radial/";
	
	public static List<RadialOption> getRadialOptions(String script, Player player, SWGObject target, Object ... args) {
		List<RadialOption> options = new ArrayList<>();
		try {
			Scripts.invoke(SCRIPT_PREFIX + script, "getOptions", options, player, target, args);
		} catch (ResourceException | ScriptException e) {
			Log.w("Couldn't retrieve radial options from %s for object %s because the script couldn't be found", SCRIPT_PREFIX + script, target);
		}
		return options;
	}
	
	public static void handleSelection(String script, Player player, SWGObject target, RadialItem selection, Object ... args) {
		try {
			Scripts.invoke(SCRIPT_PREFIX + script, "handleSelection", player, target, selection, args);
		} catch (ResourceException | ScriptException e) {
			Log.w("Can't handle selection %s on object %s because the script couldn't be found");
		}
	}
	
}
