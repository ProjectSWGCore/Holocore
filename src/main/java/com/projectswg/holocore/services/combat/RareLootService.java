/** **********************************************************************************
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
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>. * *
 **********************************************************************************
 */
package com.projectswg.holocore.services.combat;

import com.projectswg.common.control.Service;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.services.objects.ObjectCreator;

import intents.combat.CorpseLootedIntent;
import intents.combat.CreatureKilledIntent;

public final class RareLootService extends Service {

	// TODO these two could be config options
	private static final short ALLOWED_LEVEL_DIFFERENCE = 6;	// +-6 difference is allowed between killer and corpse
	private static final int DROP_CHANCE = 1;	// One in a hundred eligible kills will drop a chest

	private final Random random;

	public RareLootService() {
		random = new Random();

		registerForIntent(CreatureKilledIntent.class, this::handleCreatureKilled);
		// TODO handle chest opening
	}

	@Override
	public boolean initialize() {
		// TODO load dataset

		return super.initialize();
	}

	private void handleCreatureKilled(CreatureKilledIntent cki) {
		// TODO only the player that delivered the killing blow is considered for this
		CreatureObject corpse = cki.getCorpse();
		CreatureObject killer = cki.getKiller();

		if(!isChestEligible(corpse.isPlayer(), killer.isPlayer())) {
			return;
		}

		if(!isLevelEligible(corpse.getCombatLevel(), killer.getCombatLevel())) {
			return;
		}

		int roll = random.nextInt(100 + 1) + 1;	// Rolls from 1 to 100%

		if(roll > DROP_CHANCE) {
			// If roll is outside the drop chance, then do nothing
			return;
		}

		String template = templateForDifficulty(corpse.getCreatureDifficulty());
		SWGObject chest = ObjectCreator.createObjectFromTemplate(template);
		SWGObject inventory = killer.getSlottedObject("inventory");

		switch (chest.moveToContainer(inventory)) {
			case SUCCESS:
				new ObjectCreatedIntent(chest).broadcast();

				PlayClientEffectObjectMessage effect = new PlayClientEffectObjectMessage("appearance/pt_rare_chest.prt", "", corpse.getObjectId(), "");
				PlayMusicMessage sound = new PlayMusicMessage(0, "sound/rare_loot_chest.snd", 1, false);
				ShowLootBox box = new ShowLootBox(killer.getObjectId(), chest.getObjectId());

				killer.sendPacket(effect, sound, box);
				break;
		}
	}

	boolean isChestEligible(boolean killerPlayer, boolean corpsePlayer) {
		return killerPlayer && !corpsePlayer;
	}

	boolean isLevelEligible(short corpseLevel, short killerLevel) {
		short levelDifference = corpseLevel - killerLevel;

		return	levelDifference <= ALLOWED_LEVEL_DIFFERENCE ||
						levelDifference >= -ALLOWED_LEVEL_DIFFERENCE;
	}

	String templateForDifficulty(CreatureDifficulty difficulty) {
		switch(corpse.getCreatureDifficulty()) {
			case NORMAL:	return "object/tangible/item/shared_rare_loot_chest_1.iff";
			case ELITE:		return "object/tangible/item/shared_rare_loot_chest_2.iff";
			case BOSS:		return "object/tangible/item/shared_rare_loot_chest_3.iff";
		}
	}


}
