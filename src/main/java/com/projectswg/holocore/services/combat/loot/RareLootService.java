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
package com.projectswg.holocore.services.combat.loot;

import com.projectswg.common.network.packets.swg.zone.PlayClientEffectObjectMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.ShowLootBox;
import com.projectswg.holocore.intents.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.object.ObjectCreatedIntent;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureDifficulty;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import com.projectswg.holocore.services.objects.ObjectCreator;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Random;

public class RareLootService extends Service {
	
	// TODO these two could be config options
	private static final short MAX_LEVEL_DIFFERENCE = 6;    // +-6 difference is allowed between killer and corpse
	private static final int DROP_CHANCE = 1;    // One in a hundred eligible kills will drop a chest
	
	private final Random random;
	
	public RareLootService() {
		random = new Random();
		
		// TODO handle chest opening
	}
	
	@Override
	public boolean initialize() {
		// TODO load dataset
		
		return super.initialize();
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		// TODO only the player that delivered the killing blow is considered for this
		CreatureObject corpse = cki.getCorpse();
		CreatureObject killer = cki.getKiller();
		
		if (!isPlayerEligible(killer.isPlayer(), corpse.isPlayer())) {
			return;
		}
		
		if (!isLevelEligible(corpse.getLevel(), killer.getLevel())) {
			return;
		}
		
		int roll = random.nextInt(100) + 1;    // Rolls from 0 to 99, then we add 1 and it becomes 1 to 100
		
		if (!isDrop(roll)) {
			Log.d("No RLS drop from %s with roll %d", corpse, roll);
			return;
		}
		
		String template = templateForDifficulty(corpse.getDifficulty());
		SWGObject chest = ObjectCreator.createObjectFromTemplate(template);
		SWGObject inventory = killer.getSlottedObject("inventory");
		
		chest.setStf("loot_n", chestIdForTemplate(template) + "_n");
		chest.setDetailStf("loot_n", chestIdForTemplate(template) + "_d");    // Not located in loot_d, for whatever reason...
		
		chest.moveToContainer(inventory);
		sendSuccessPackets(chest, corpse, killer);
	}
	
	boolean isPlayerEligible(boolean killerPlayer, boolean corpsePlayer) {
		return killerPlayer && !corpsePlayer;
	}
	
	boolean isLevelEligible(int corpseLevel, int killerLevel) {
		// Ensure a positive levelDifference
		int highestLevel = Math.max(corpseLevel, killerLevel);
		int lowestLevel = Math.min(corpseLevel, killerLevel);
		int levelDifference = highestLevel - lowestLevel;
		
		return levelDifference <= MAX_LEVEL_DIFFERENCE;
	}
	
	boolean isDrop(int roll) {
		return roll <= DROP_CHANCE;
	}
	
	String templateForDifficulty(CreatureDifficulty difficulty) {
		switch (difficulty) {
			case NORMAL:
				return "object/tangible/item/shared_rare_loot_chest_1.iff";
			case ELITE:
				return "object/tangible/item/shared_rare_loot_chest_2.iff";
			case BOSS:
				return "object/tangible/item/shared_rare_loot_chest_3.iff";
			default:
				throw new IllegalArgumentException("Unknown CreatureDifficulty: " + difficulty);
		}
	}
	
	String chestIdForTemplate(String template) {
		return template.replace("object/tangible/item/shared_", "").replace(".iff", "");
	}
	
	private void sendSuccessPackets(SWGObject chest, CreatureObject corpse, CreatureObject killer) {
		ObjectCreatedIntent.broadcast(chest);
		
		PlayClientEffectObjectMessage effect = new PlayClientEffectObjectMessage("appearance/pt_rare_chest.prt", "", corpse.getObjectId(), "");
		PlayMusicMessage sound = new PlayMusicMessage(0, "sound/rare_loot_chest.snd", 1, false);
		ShowLootBox box = new ShowLootBox(killer.getObjectId(), new long[] { chest.getObjectId() });
		
		killer.getOwner().sendPacket(effect, sound, box);
	}
	
}
