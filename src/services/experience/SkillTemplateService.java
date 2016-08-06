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

import intents.experience.LevelChangedIntent;
import intents.experience.SkillBoxGrantedIntent;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import network.packets.swg.zone.PlayClientEffectObjectMessage;
import network.packets.swg.zone.PlayMusicMessage;
import network.packets.swg.zone.object_controller.ShowFlyText;
import network.packets.swg.zone.object_controller.ShowFlyText.Scale;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.common.RGB;
import resources.control.Intent;
import resources.control.Service;
import resources.encodables.OutOfBandPackage;
import resources.encodables.StringId;
import resources.objects.creature.CreatureObject;
import resources.objects.player.PlayerObject;
import resources.player.Player;
import resources.server_info.Log;

/**
 * This is a service that listens for {@link LevelChangedIntent} and grants
 * everything linked to a skillbox.
 * @author Mads
 */
public final class SkillTemplateService extends Service {
	
	private final Map<String, String[]> skillTemplates;
	
	SkillTemplateService() {
		skillTemplates = new HashMap<>();
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
		
		for (int level = oldLevel + 1; level <= newLevel; level++) {
			// Skills are only awarded every third or fourth level
			if ((level == 4 || level == 7 || level == 10) || ((level > 10) && (((level - 10) % 4) == 0))) {
				PlayerObject playerObject = creatureObject.getPlayerObject();
				String profession = playerObject.getProfession();
				String[] templates = skillTemplates.get(profession);

				if (templates == null) {
					Log.w(this, "%s tried to level up to %d with invalid profession %s", creatureObject, level, profession);
				} else {
					int skillIndex = ((level <= 10) ? ((level - 1) / 3) : ((((level - 10) / 4)) + 3));

					String skillName = templates[skillIndex];
					new SkillBoxGrantedIntent(skillName, creatureObject).broadcast();
					playerObject.setProfWheelPosition(skillName);
					
					// TODO roadmap reward items
					
					creatureObject.sendObserversAndSelf(new PlayClientEffectObjectMessage("clienteffect/skill_granted.cef", "", objectId));
					sendPacket(player, new ShowFlyText(objectId, new StringId("cbt_spam", "skill_up"), Scale.LARGEST, new RGB(Color.GREEN)));
					sendPacket(player, new PlayMusicMessage(0, "sound/music_acq_bountyhunter.snd", 1, false));
				}
			} else {
				creatureObject.sendObserversAndSelf(new PlayClientEffectObjectMessage("clienteffect/level_granted.cef", "", objectId));
				sendPacket(player, new ShowFlyText(objectId, new StringId("cbt_spam", "level_up"), Scale.LARGEST, new RGB(Color.BLUE)));
				Log.d(this, "Level %d has no skillbox - %s is rewarded nothing", level, creatureObject);
			}
		}
	}
}
