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
package com.projectswg.holocore.services.gameplay.player.experience.skills.skillmod;

import com.projectswg.holocore.intents.gameplay.player.experience.skills.SkillModIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.Map;

public class SkillModService extends Service {
	
	public SkillModService() {
		
	}
	
	@IntentHandler
	private void handleContainerTransferIntent(ContainerTransferIntent cti){
		if (cti.getObject().getOwner() == null)
		    return;
		
		CreatureObject creature = cti.getObject().getOwner().getCreatureObject();
	
		for (Map.Entry<String, String> attributes : cti.getObject().getAttributes().entrySet()){
			if(attributes.getKey().startsWith("cat_skill_mod_bonus")){
				String[] splitModName = attributes.getKey().split(":",2);
				String modName = splitModName[1];
				int modValue = Integer.parseInt(attributes.getValue());

				if(cti.getContainer().getObjectId() == creature.getObjectId()){
					adjustSkillmod(creature, modName, 0, modValue);
				}else if(cti.getOldContainer() != null){
					if(cti.getOldContainer().getObjectId() == creature.getObjectId()){
						adjustSkillmod(creature, modName, 0, -modValue);
					}
				}				
			}
		}
	}

	@IntentHandler
	private void handleSkillModIntent(SkillModIntent smi) {
		for (CreatureObject creature : smi.getAffectedCreatures()) {
			String skillModName = smi.getSkillModName();
			
			int adjustBase = smi.getAdjustBase();
			int adjustModifier = smi.getAdjustModifier();
			
			adjustSkillmod(creature, skillModName, adjustBase, adjustModifier);
		}
	}
	
	private void adjustSkillmod(CreatureObject creature, String skillModName, int adjustBase, int adjustModifier) {
		creature.adjustSkillmod(skillModName, adjustBase, adjustModifier);
	}
	
}
