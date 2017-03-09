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

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import intents.GrantBadgeIntent;
import intents.experience.GrantSkillIntent;
import intents.experience.LevelChangedIntent;
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
import resources.control.Service;
import resources.encodables.StringId;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.rewards.RoadmapReward;
import resources.server_info.Log;
import services.objects.ObjectCreator;
import services.objects.StaticItemService;

/**
 * This is a service that listens for {@link LevelChangedIntent} and grants
 * everything linked to a skillbox.
 * @author Mads
 */
public final class SkillTemplateService extends Service {
	
	private final Map<String, String[]> skillTemplates;
	private final Map<String, String[]> badgeNames;
	private Map<String, RoadmapReward> rewards;

	public SkillTemplateService() {
		skillTemplates = new HashMap<>();
		
		badgeNames = new HashMap<>();
		badgeNames.put("bounty_hunter_1a", 	new String[]{"new_prof_bountyhunter_master"});
		badgeNames.put("commando_1a", 		new String[]{"new_prof_commando_master"});
		badgeNames.put("entertainer_1a",	new String[]{"new_prof_social_entertainer_master"});
		badgeNames.put("force_sensitive_1a",new String[]{"new_prof_jedi_master"});
		badgeNames.put("medic_1a", 			new String[]{"new_prof_medic_master"});
		badgeNames.put("officer_1a", 		new String[]{"new_prof_officer_master"});
		badgeNames.put("smuggler_1a", 		new String[]{"new_prof_smuggler_master"});
		badgeNames.put("spy_1a", 			new String[]{"new_prof_spy_master"});
		// All traders become Master Merchants and Master Artisans
		badgeNames.put("trader_0a", 		new String[]{"new_prof_crafting_merchant_master", "new_prof_crafting_artisan_master", "new_prof_crafting_chef_master", "new_prof_crafting_tailor_master"});
		badgeNames.put("trader_0b", 		new String[]{"new_prof_crafting_merchant_master", "new_prof_crafting_artisan_master", "new_prof_crafting_architect_master"});
		badgeNames.put("trader_0c", 		new String[]{"new_prof_crafting_merchant_master", "new_prof_crafting_artisan_master", "new_prof_crafting_armorsmith_master", "new_prof_crafting_weaponsmith_master"});
		badgeNames.put("trader_0d", 		new String[]{"new_prof_crafting_merchant_master", "new_prof_crafting_artisan_master", "new_prof_crafting_droidengineer_master"});
		
		registerForIntent(LevelChangedIntent.class, lci -> handleLevelChangedIntent(lci));
	}

	@Override
	public boolean initialize() {
		DatatableData skillTemplateTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/skill_template/skill_template.iff");

		for (int row = 0; row < skillTemplateTable.getRowCount(); row++) {
			String profession = (String) skillTemplateTable.getCell(row, 0);
			String[] templates = ((String) skillTemplateTable.getCell(row, 4)).split(",");
			
			skillTemplates.put(profession, templates);
		}

		DatatableData rewardsTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/roadmap/item_rewards.iff");
		rewards = loadRewardItemsIff(rewardsTable);

		return super.initialize();
	}

	private void handleLevelChangedIntent(LevelChangedIntent lci) {
		short oldLevel = lci.getPreviousLevel();
		short newLevel = lci.getNewLevel();
		CreatureObject creatureObject = lci.getCreatureObject();
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
					Log.w("%s tried to level up to %d with invalid profession %s", creatureObject, level, profession);
					return;
				}
				
				int skillIndex = (level <= 10) ? ((level - 1) / 3) : (((level - 10) / 4) + 3);

				String skillName = templates[skillIndex];
				new GrantSkillIntent(GrantSkillIntent.IntentType.GRANT, skillName, creatureObject, true).broadcast();
				playerObject.setProfWheelPosition(skillName);

				// Grants a mastery collection badge, IF they qualify.
				grantMasteryBadge(creatureObject, profession, skillName);
				giveRewardItems(creatureObject, skillName);
				
				skillUp = true;
			}
		}
		
		String effectFile;
		String flyText;
		RGB flyTextColor;
		
		if (skillUp)
		{
			effectFile = "clienteffect/skill_granted.cef";
			flyText = "skill_up";
			flyTextColor = new RGB(Color.GREEN);
		}
		else
		{
			effectFile = "clienteffect/level_granted.cef";
			flyText = "level_up";
			flyTextColor = new RGB(Color.BLUE);
		}
		
		creatureObject.sendObserversAndSelf(new PlayClientEffectObjectMessage(effectFile, "", objectId));
		player.sendPacket(new ShowFlyText(objectId, new StringId("cbt_spam", flyText), Scale.LARGEST, flyTextColor));
		
		if (skillUp) player.sendPacket(new PlayMusicMessage(0, "sound/music_acq_bountyhunter.snd", 1, false));
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

			Collection<String> staticItems = new ArrayList<>();
			
			for (String item : items) {
				if (item.endsWith(".iff")) {
					SWGObject nonStaticItem = ObjectCreator.createObjectFromTemplate(ClientFactory.formatToSharedFile(item));

					if (nonStaticItem != null) {
						nonStaticItem.moveToContainer(inventory);
					}
					new ObjectCreatedIntent(nonStaticItem).broadcast();
				} else {
					staticItems.add(item);
				}
			}
			
			// No reason to broadcast this intent if we don't need new static items anyways
			if (!staticItems.isEmpty())
				new CreateStaticItemIntent(creatureObject, inventory, new StaticItemService.LootBoxHandler(creatureObject), staticItems.toArray(new String[staticItems.size()])).broadcast();
		}
	}

	private HashMap<String, RoadmapReward> loadRewardItemsIff(DatatableData rewardsTable) {
		HashMap<String, RoadmapReward> rewards = new HashMap<>();
		
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
		
		return rewards;
	}
	
	private void grantMasteryBadge(CreatureObject creature, String profession, String skillName) {
		Log.d("grantMasteryBadge - skillName: %s", skillName);
		if (skillName.endsWith("_phase4_master")) {
			if (badgeNames.containsKey(profession)) {
				String[] badgeList = badgeNames.get(profession);
				for (String badgeName : badgeList)
				{
					new GrantBadgeIntent(creature, badgeName).broadcast();
					Log.d("granting badge %s to %s", badgeName, creature);
				}
			}
			else
			{
				Log.e("%s could not be granted a mastery badge because their profession %s is unrecognised", creature, profession);
			}
		}
	}
	
}
