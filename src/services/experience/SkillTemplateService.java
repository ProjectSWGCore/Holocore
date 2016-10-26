/***********************************************************************************
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
package services.experience;

import intents.GrantBadgeIntent;
import intents.experience.LevelChangedIntent;
import intents.experience.SkillBoxGrantedIntent;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import intents.object.CreateStaticItemIntent;
import intents.object.ObjectCreatedIntent;
import network.packets.swg.zone.PlayClientEffectObjectMessage;
import network.packets.swg.zone.PlayMusicMessage;
import network.packets.swg.zone.object_controller.ShowFlyText;
import network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import resources.Race;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.common.RGB;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.rewards.RoadmapReward;
import resources.server_info.Log;
import services.objects.ObjectCreator;

/**
 * This is a service that listens for {@link LevelChangedIntent} and grants
 * everything linked to a skillbox.
 * @author Mads
 */
public final class SkillTemplateService extends Service {
	
	private final Map<String, String[]> skillTemplates;
	private Map<String, RoadmapReward> rewards;
	private DatatableData rewardsTable;

	public SkillTemplateService() {
		skillTemplates = new HashMap<>();
		rewards = new HashMap<>();
		rewardsTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/roadmap/item_rewards.iff");
		registerForIntent(LevelChangedIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		DatatableData skillTemplateTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/skill_template/skill_template.iff");

		for (int row = 0; row < skillTemplateTable.getRowCount(); row++) {
			String profession = (String) skillTemplateTable.getCell(row, 0);
			String[] templates = ((String) skillTemplateTable.getCell(row, 4)).split(",");
			
			skillTemplates.put(profession, templates);
		}

		loadRewardItemsIff();

		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case LevelChangedIntent.TYPE: handleLevelChangedIntent((LevelChangedIntent) i); break;
		}
	}
	
	private void handleLevelChangedIntent(LevelChangedIntent i) {
		short oldLevel = i.getPreviousLevel();
		short newLevel = i.getNewLevel();
		CreatureObject creatureObject = i.getCreatureObject();
		Player player = creatureObject.getOwner();
		long objectId = creatureObject.getObjectId();
		boolean skillUp = false;
		
		for (int level = oldLevel + 1; level <= newLevel; level++) {
			// Skills are only awarded every third or fourth level
			if ((level == 4 || level == 7 || level == 10) || ((level > 10) && (((level - 10) % 4) == 0))) {
				PlayerObject playerObject = creatureObject.getPlayerObject();
				String profession = playerObject.getProfession();
				String[] templates = skillTemplates.get(profession);

				if (templates == null) {
					Log.w(this, "%s tried to level up to %d with invalid profession %s", creatureObject, level, profession);
					return;
				}
				
				int skillIndex = (level <= 10) ? ((level - 1) / 3) : (((level - 10) / 4) + 3);

				String skillName = templates[skillIndex];
				new SkillBoxGrantedIntent(skillName, creatureObject).broadcast();
				playerObject.setProfWheelPosition(skillName);

				// Grants a mastery collection badge, IF they qualify.
				grantMasteryBadge(creatureObject, profession, skillName);
				giveRewardItems(creatureObject, skillName);
				
				skillUp = true;
			}
		}
		
		if (skillUp) {
			creatureObject.sendObserversAndSelf(new PlayClientEffectObjectMessage("clienteffect/skill_granted.cef", "", objectId));
			sendPacket(player, new ShowFlyText(objectId, new StringId("cbt_spam", "skill_up"), Scale.LARGEST, new RGB(Color.GREEN)));
			sendPacket(player, new PlayMusicMessage(0, "sound/music_acq_bountyhunter.snd", 1, false));
		} else {
			creatureObject.sendObserversAndSelf(new PlayClientEffectObjectMessage("clienteffect/level_granted.cef", "", objectId));
			sendPacket(player, new ShowFlyText(objectId, new StringId("cbt_spam", "level_up"), Scale.LARGEST, new RGB(Color.BLUE)));
		}
	}

	private void giveRewardItems(CreatureObject creatureObject, String skillName) {
		RoadmapReward reward = rewards.get(skillName);
		Race characterRace = creatureObject.getRace();
		String species = characterRace.getSpecies().toUpperCase();
		SWGObject inventory = creatureObject.getSlottedObject("inventory");
		String[] items;

		if (reward.hasItems() || reward.isUniversalReward()) {
			if (reward.isUniversalReward())
				items = reward.getDefaultRewardItems();
			else if (species.equals("ITHORIAN"))
				items = reward.getIthorianRewardItems();
			else if (species.equals("WOOKIEE"))
				items = reward.getWookieeRewardItems();
			else
				items = reward.getDefaultRewardItems();

			for (String item : items) {
				if (item.endsWith(".iff")) {
					SWGObject nonStaticItem = ObjectCreator.createObjectFromTemplate(ClientFactory.formatToSharedFile(item));

					if (nonStaticItem != null) {
						nonStaticItem.moveToContainer(inventory);
					}
					new ObjectCreatedIntent(nonStaticItem).broadcast();
				} else
					new CreateStaticItemIntent(creatureObject, inventory, item).broadcast();
			}
		}
	}

	private void loadRewardItemsIff() {
		for (int row = 0; row < rewardsTable.getRowCount(); row++) {
			String roadmapTemplate = rewardsTable.getCell(row, 0).toString();
			String roadmapSkillName = rewardsTable.getCell(row, 1).toString();
			String appearanceName = rewardsTable.getCell(row, 2).toString();
			String stringId = rewardsTable.getCell(row, 3).toString();
			String itemDefault = rewardsTable.getCell(row, 4).toString();
			String itemWookiee = rewardsTable.getCell(row, 5).toString();
			String itemIthorian = rewardsTable.getCell(row, 6).toString();

			rewards.put(roadmapSkillName, new RoadmapReward(roadmapTemplate, roadmapSkillName, appearanceName, stringId, itemDefault, itemWookiee, itemIthorian));
		}
	}
	
	private void grantMasteryBadge(CreatureObject creature, String profession, String skillName) {
		if (skillName.endsWith("_phase4_master")) {
			if (profession.startsWith("trader_0")) {
				// All traders become Master Merchants and Master Artisans
				new GrantBadgeIntent(creature, "new_prof_crafting_merchant_master").broadcast();
				new GrantBadgeIntent(creature, "new_prof_crafting_artisan_master").broadcast();
			}

			switch (profession) {
				case "bounty_hunter_1a":
					new GrantBadgeIntent(creature, "new_prof_bountyhunter_master").broadcast();
					break;
				case "commando_1a":
					new GrantBadgeIntent(creature, "new_prof_commando_master").broadcast();
					break;
				case "entertainer_1a":
					new GrantBadgeIntent(creature, "new_prof_social_entertainer_master").broadcast();
					break;
				case "force_sensitive_1a":
					new GrantBadgeIntent(creature, "new_prof_jedi_master").broadcast();
					break;
				case "medic_1a":
					new GrantBadgeIntent(creature, "new_prof_medic_master").broadcast();
					break;
				case "officer_1a":
					new GrantBadgeIntent(creature, "new_prof_officer_master").broadcast();
					break;
				case "smuggler_1a":
					new GrantBadgeIntent(creature, "new_prof_smuggler_master").broadcast();
					break;
				case "spy_1a":
					new GrantBadgeIntent(creature, "new_prof_spy_master").broadcast();
					break;
				case "trader_0a":
					new GrantBadgeIntent(creature, "new_prof_crafting_chef_master").broadcast();
					new GrantBadgeIntent(creature, "new_prof_crafting_tailor_master").broadcast();
					break;
				case "trader_0b":
					new GrantBadgeIntent(creature, "new_prof_crafting_architect_master").broadcast();
					break;
				case "trader_0c":
					new GrantBadgeIntent(creature, "new_prof_crafting_armorsmith_master").broadcast();
					new GrantBadgeIntent(creature, "new_prof_crafting_weaponsmith_master").broadcast();
					break;
				case "trader_0d":
					new GrantBadgeIntent(creature, "new_prof_crafting_droidengineer_master").broadcast();
					break;
				default:
					Log.e(this, "%s could not be granted a mastery badge because their profession %s is unrecognised", creature, profession);
					break;
			}
		}
	}
	
}
