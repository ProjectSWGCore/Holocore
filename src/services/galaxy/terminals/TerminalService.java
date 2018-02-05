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
package services.galaxy.terminals;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.projectswg.common.control.Service;
import com.projectswg.common.data.info.RelationalServerData;
import com.projectswg.common.data.info.RelationalServerFactory;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.common.debug.Log;

import intents.radial.RadialRequestIntent;
import intents.radial.RadialResponseIntent;
import intents.radial.RadialSelectionIntent;
import resources.radial.Radials;

public class TerminalService extends Service {
	
	private static final String GET_ALL_TEMPLATES_SQL = "SELECT iff FROM radials";
	private static final String GET_SCRIPT_FOR_IFF_SQL = "SELECT script FROM radials WHERE iff = ?";
	
	private final Set<String> templates;
	private final RelationalServerData iffDatabase;
	private final PreparedStatement getAllTemplatesStatement;
	private final PreparedStatement getScriptForIffStatement;
	
	public TerminalService() {
		templates = new HashSet<>();
		iffDatabase = RelationalServerFactory.getServerData("radial/radials.db", "radials");
		if (iffDatabase == null)
			throw new main.ProjectSWG.CoreException("Unable to load sdb files for StaticService");

		getAllTemplatesStatement = iffDatabase.prepareStatement(GET_ALL_TEMPLATES_SQL);
		getScriptForIffStatement = iffDatabase.prepareStatement(GET_SCRIPT_FOR_IFF_SQL);
		
		registerForIntent(RadialRequestIntent.class, this::handleRadialRequestIntent);
		registerForIntent(RadialSelectionIntent.class, this::handleRadialSelectionIntent);
	}
	
	@Override
	public boolean initialize() {
		synchronized (getAllTemplatesStatement) {
			// Cool and fancy Java thing to auto-cleanup resources
			try (ResultSet set = getAllTemplatesStatement.executeQuery()) {
				templates.clear();
				while (set.next()) {
					templates.add(set.getString("iff"));
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		return super.initialize();
	}
	
	private void handleRadialRequestIntent(RadialRequestIntent rri){
		String script = lookupScript(rri.getTarget().getTemplate());
		if (script == null)
			return;
		List<RadialOption> options = new ArrayList<>(rri.getRequest().getOptions());
		options.addAll(Radials.getRadialOptions(script, rri.getPlayer(), rri.getTarget()));
		new RadialResponseIntent(rri.getPlayer(), rri.getTarget(), options, rri.getRequest().getCounter()).broadcast();
	}
	
	private void handleRadialSelectionIntent(RadialSelectionIntent rsi){
		String script = lookupScript(rsi.getTarget().getTemplate());
		if (script == null)
			return;
		Radials.handleSelection(script, rsi.getPlayer(), rsi.getTarget(), rsi.getSelection());
	}
	
	private String lookupScript(String iff) {
		if (!templates.contains(iff))
			return null;
		synchronized (getScriptForIffStatement) {
			try {
				getScriptForIffStatement.setString(1, iff);
				try (ResultSet set = getScriptForIffStatement.executeQuery()) {
					if (set.next())
						return set.getString("script");
					
					Log.e("Cannot find script for template: " + iff);
				}
			} catch (SQLException e) {
				Log.e(e);
			}
		}
		return null;
	}
	
}
