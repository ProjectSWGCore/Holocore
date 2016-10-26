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

import intents.SkillModIntent;
import intents.experience.SkillBoxGrantedIntent;
import intents.network.GalacticPacketIntent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import network.packets.Packet;
import network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Manager;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.server_info.Log;

/**
 *
 * @author Mads
 */
public final class SkillManager extends Manager {
	
	// Maps icon index to qualifying skill.
	private final Map<Integer, Set<String>> roleIconMap;
	private final Map<String, SkillData> skillDataMap;
	
	public SkillManager() {
		roleIconMap = new HashMap<>();
		skillDataMap = new HashMap<>();
		
		addChildService(new ExpertiseService());
		
		registerForIntent(SkillBoxGrantedIntent.TYPE);
		registerForIntent(GalacticPacketIntent.TYPE);
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case SkillBoxGrantedIntent.TYPE:
				if (i instanceof SkillBoxGrantedIntent)
					handleSkillAddIntent((SkillBoxGrantedIntent) i);
				break;
			case GalacticPacketIntent.TYPE:
				if (i instanceof GalacticPacketIntent)
					handleGalacticPacket((GalacticPacketIntent) i);
				break;
		}
	}
	
	@Override
	public boolean initialize() {
		DatatableData roleIconTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/role/role.iff", false);
		
		for (int i = 0; i < roleIconTable.getRowCount(); i++) {
			int iconIndex = (int) roleIconTable.getCell(i, 0);
			String qualifyingSkill = (String) roleIconTable.getCell(i, 2);
			
			Set<String> qualifyingSkills = roleIconMap.get(iconIndex);
			
			if(qualifyingSkills == null) {
				qualifyingSkills = new HashSet<>();
				roleIconMap.put(iconIndex, qualifyingSkills);
			}
			
			qualifyingSkills.add(qualifyingSkill);
		}
		
		loadSkills();
		
		return super.initialize();
	}
	
	private void loadSkills() {
		DatatableData skillsTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/skill/skills.iff", false);
		
		for (int i = 0; i < skillsTable.getRowCount(); i++) {
			String skillName = (String) skillsTable.getCell(i, 0);
			String [] skillModsStrings = splitCsv((String) skillsTable.getCell(i, 22));
			Map<String, Integer> skillMods = new HashMap<>();
			for (String skillModString : skillModsStrings) {
				String [] values = skillModString.split("=", 2);
				skillMods.put(values[0], Integer.parseInt(values[1]));
			}
			
			SkillData skillData = new SkillData(
					splitCsv((String) skillsTable.getCell(i, 11)),	// required skills
					(String) skillsTable.getCell(i, 12),			// xp type
					(int) skillsTable.getCell(i, 13),				// xp cost
					splitCsv((String) skillsTable.getCell(i, 21)),	// commands
					skillMods,
					splitCsv((String) skillsTable.getCell(i, 23))	// schematics
			);
			
			skillDataMap.put(skillName, skillData);
		}
	}
	
	private String [] splitCsv(String str) {
		if (str.isEmpty())
			return new String[0];
		else if (str.indexOf(',') == -1)
			return new String[]{str};
		return str.split(",");
	}
	
	private void handleSkillAddIntent(SkillBoxGrantedIntent intent) {
		String skillName = intent.getSkillName();
		CreatureObject target = intent.getTarget();
		SkillData skillData = skillDataMap.get(skillName);
		String[] requiredSkills = skillData.getRequiredSkills();
		
		for(String requiredSkill : requiredSkills) {
			if(!target.hasSkill(requiredSkill)) {
				Log.w(this, "%s lacks required skill %s before being granted skill %s", target, requiredSkill, skillName);
				return;
			}
		}
		
		target.addSkill(skillName);
		
		// TODO expertise abilities?
		
		for(String commandName : skillData.getCommands()) {
			target.addAbility(commandName);
		}
		
		skillData.getSkillMods().forEach((skillModName, skillModValue) -> new SkillModIntent(skillModName, 0, skillModValue, target).broadcast());
		
		// Ziggy: These are disabled for now, since we don't have the structs in place for it.
//		for(String schematic : skillData.schematics) {
			// Add schematic to PlayerObject
//		}
		
		Log.i(this, "%s was given skill %s", target, skillName);
	}
	
	private void handleGalacticPacket(GalacticPacketIntent gpi) {
		Packet packet = gpi.getPacket();
		if (packet instanceof ChangeRoleIconChoice) {
			ChangeRoleIconChoice iconChoice = (ChangeRoleIconChoice) packet;
			
			int chosenIcon = iconChoice.getIconChoice();
			SWGObject object = gpi.getObjectManager().getObjectById(iconChoice.getObjectId());
			
			if(object instanceof CreatureObject) {
				changeRoleIcon((CreatureObject) object, chosenIcon);
			} else {
				Log.e(this, "Could not alter role icon for object %s because it's not a CreatureObject", object);
			}
		}
	}
	
	private void changeRoleIcon(CreatureObject creature, int chosenIcon) {
		Set<String> qualifyingSkills = roleIconMap.get(chosenIcon);
		
		if (qualifyingSkills != null) {
			for(String qualifyingSkill : qualifyingSkills) {
				if (creature.hasSkill(qualifyingSkill)) {
					PlayerObject playerObject = creature.getPlayerObject();

					if(playerObject != null) {
						playerObject.setProfessionIcon(chosenIcon);
						break;
					} else {
						Log.e(this, "Could not alter role icon for PlayerObject of %s because it has none attached" , creature);
					}
				}
			}
		} else {
			Log.w(this, "%s tried to use undefined role icon %d", creature, chosenIcon);
		}
	}
	
	@SuppressWarnings("unused")
	private static class SkillData {
		private String[] requiredSkills;
		private String xpType;
		private int xpCost;
		private String[] commands;
		private Map<String, Integer> skillMods;
		private String[] schematics;

		public SkillData(String[] requiredSkills, String xpType, int xpCost, String[] commands, Map<String, Integer> skillMods, String[] schematics) {
			this.requiredSkills = requiredSkills;
			this.xpType = xpType;
			this.xpCost = xpCost;
			this.commands = commands;
			this.skillMods = skillMods;
			this.schematics = schematics;
		}

		public String[] getRequiredSkills() { return requiredSkills; }
		public String getXpType() { return xpType; }
		public int getXpCost() { return xpCost; }
		public String[] getCommands() { return commands; }
		public Map<String, Integer> getSkillMods() { return skillMods; }
		public String[] getSchematics() { return schematics; }
	}
	
}
