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

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.holocore.intents.gameplay.jedi.TuneCrystalIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

/**
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Lightsaber crystal tuning</li>
 *     <li>Applying the owner attribute to crystals that are created and don't already have it</li>
 * </ul>
 */
public class LightsaberCrystalService extends Service {
	
	private static final String CRYSTAL_OWNER = "@obj_attr_n:crystal_owner";
	private static final String UNTUNED = "\\#D1F56F UNTUNED \\#FFFFFF ";
	
	@IntentHandler
	private void handleTuneCrystalIntent(TuneCrystalIntent intent) {
		CreatureObject tuner = intent.getTuner();
		SWGObject crystal = intent.getCrystal();
		
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
			crystal.addAttribute(CRYSTAL_OWNER, tuner.getObjectName());
			crystal.setObjectName( "\\#00FF00" + crystal.getObjectName() + " (tuned)");
			
			// TODO if power crystal or pearl (look for Quality? attribute on object), then apply randomized(?) min/max attributes as well
			
			SystemMessageIntent.broadcastPersonal(owner, "@jedi_spam:crystal_tune_success");
		}));
		
		suiMessageBox.display(owner);
	}
	
	@IntentHandler
	private void handleObjectCreated(ObjectCreatedIntent intent) {
		SWGObject object = intent.getObject();
		
		if (object.getGameObjectType() != GameObjectType.GOT_COMPONENT_SABER_CRYSTAL || isTuned(object)) {
			// We don't apply the untuned attribute to something that's not a lightsaber crystal or already has the attribute
			return;
		}
		
		object.addAttribute(CRYSTAL_OWNER, UNTUNED);
	}
	
	private final boolean isTuned(SWGObject crystal) {
		if (!crystal.hasAttribute(CRYSTAL_OWNER)) {
			return false;
		}
		
		return !UNTUNED.equals(crystal.getAttribute(CRYSTAL_OWNER));
	}
}
