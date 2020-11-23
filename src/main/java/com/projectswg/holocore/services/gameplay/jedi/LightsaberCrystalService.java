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
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * <h3>Responsibilities</h3>
 * <ul>
 *     <li>Lightsaber crystal tuning</li>
 *     <li>Applying the owner attribute to crystals that are created and don't already have it</li>
 * </ul>
 */
public final class LightsaberCrystalService extends Service {

	// Object attribute keys
	public static final String CRYSTAL_OWNER = "@obj_attr_n:crystal_owner";
	public static final String QUALITY = "@obj_attr_n:quality";
	private static final String MIN_DAMAGE = "@obj_attr_n:mindamage";
	private static final String MAX_DAMAGE = "@obj_attr_n:maxdamage";

	// Object attribute values
	private static final String POOR_VALUE = "@jedi_spam:crystal_quality_0";
	private static final String FAIR_VALUE = "@jedi_spam:crystal_quality_1";
	private static final String GOOD_VALUE = "@jedi_spam:crystal_quality_2";
	private static final String QUALITY_VALUE = "@jedi_spam:crystal_quality_3";
	private static final String SELECT_VALUE = "@jedi_spam:crystal_quality_4";
	private static final String PREMIUM_VALUE = "@jedi_spam:crystal_quality_5";
	private static final String FLAWLESS_VALUE = "@jedi_spam:crystal_quality_6";
	private static final String PERFECT_VALUE = "@jedi_spam:crystal_quality_7";

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
			
			if (isPowerCrystal(crystal)) {
				generateDamageModifiers(crystal);
			}

			SystemMessageIntent.broadcastPersonal(owner, "@jedi_spam:crystal_tune_success");
		}));
		
		suiMessageBox.display(owner);
	}
	
	private boolean isTuned(SWGObject crystal) {
		if (!crystal.hasAttribute(CRYSTAL_OWNER)) {
			return false;
		}
		
		return !UNTUNED.equals(crystal.getAttribute(CRYSTAL_OWNER));
	}

	private boolean isPowerCrystal(SWGObject crystal) {
		return crystal.hasAttribute(QUALITY);
	}

	private void generateDamageModifiers(SWGObject crystal) {
		int baseDamage = getBaseDamage(crystal);
		ThreadLocalRandom random = ThreadLocalRandom.current();
		int addedDamage = random.nextInt(0, 4);	// Randomly add 0, 1, 2 or 3 damage points
		int minDamage = baseDamage + addedDamage;
		int maxDamage = minDamage + 1;

		crystal.addAttribute(MIN_DAMAGE, String.valueOf(minDamage));
		crystal.addAttribute(MAX_DAMAGE, String.valueOf(maxDamage));
	}

	private int getBaseDamage(SWGObject crystal) {
		String attribute = crystal.getAttribute(QUALITY);

		switch (attribute) {
			case POOR_VALUE: return 1;
			case FAIR_VALUE: return 3;
			case GOOD_VALUE: return 6;
			case QUALITY_VALUE: return 11;
			case SELECT_VALUE: return 13;
			case PREMIUM_VALUE: return 15;
			case FLAWLESS_VALUE: return 19;
			case PERFECT_VALUE: return 22;
			default: throw new UnsupportedOperationException("Unknown attribute " + attribute);
		}
	}

}
