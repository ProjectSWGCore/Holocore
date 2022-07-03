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
package com.projectswg.holocore.services.gameplay.jedi;

import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

/**
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Lightsaber crystal tuning</li>
 *     <li>Applying the owner attribute to crystals that are created and don't already have it</li>
 * </ul>
 */
public final class LightsaberCrystalService extends Service {
	
	@IntentHandler
	private void handleTuneCrystalIntent(TuneCrystalIntent intent) {
		CreatureObject tuner = intent.getTuner();
		TangibleObject crystal = intent.getCrystal();
		
		if (isTuned(crystal)) {
			return;
		}
		
		Player owner = tuner.getOwner();
		
		if (owner == null) {
			return;
		}
		
		SuiMessageBox suiMessageBox = new SuiMessageBox(SuiButtons.YES_NO, "@jedi_spam:confirm_tune_title", "@jedi_spam:confirm_tune_prompt");
		
		suiMessageBox.addOkButtonCallback("tune", ((event, parameters) -> {
			crystal.setServerAttribute(ServerAttribute.LINK_OBJECT_ID, tuner.getObjectId());	// In case the name of the character ever changes
			crystal.setObjectName( "\\#00FF00" + crystal.getObjectName() + " (tuned)");

			SystemMessageIntent.broadcastPersonal(owner, "@jedi_spam:crystal_tune_success");
		}));
		
		suiMessageBox.display(owner);
	}
	
	private boolean isTuned(TangibleObject crystal) {
		return crystal.getServerAttribute(ServerAttribute.LINK_OBJECT_ID) != null;
	}

}
