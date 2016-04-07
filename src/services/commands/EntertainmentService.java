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
package services.commands;

import intents.DanceIntent;
import intents.chat.ChatBroadcastIntent;
import java.util.HashMap;
import java.util.Map;
import resources.Posture;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.DatatableData;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;

/**
 *
 * @author Mads
 */
public class EntertainmentService extends Service {

	private final Map<String, Integer> danceMap;	// dance performanceNames mapped to danceId

	public EntertainmentService() {
		danceMap = new HashMap<>();
		registerForIntent(DanceIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		DatatableData performanceTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/performance/performance.iff");

		for (int i = 0; i < performanceTable.getRowCount(); i++) {
			String requiredDance = (String) performanceTable.getCell(i, 4);

			if (!requiredDance.isEmpty()) {
				danceMap.put((String) performanceTable.getCell(i, 0), (int) performanceTable.getCell(i, 5));	// performanceName, danceVisualId
			}
		}

		return super.initialize();
	}

	@Override
	public void onIntentReceived(Intent i) {
		switch (i.getType()) {
			case DanceIntent.TYPE:
				handleDanceIntent((DanceIntent) i);
				break;
		}
	}

	private void handleDanceIntent(DanceIntent i) {
		CreatureObject dancer = i.getCreatureObject();
		String danceName = i.getDanceName();

		if (i.isStartDance()) {
			// This intent wants the creature to start dancing
			if (dancer.isPerforming()) {
				new ChatBroadcastIntent(dancer.getOwner(), "@performance:already_performing_self").broadcast();
			} else if (danceMap.containsKey(danceName)) {
				// The dance name is valid.
				if (dancer.hasAbility("startDance+" + danceName)) {
					startDancing(dancer, "dance_" + danceMap.get(danceName));
				} else {
					// This creature doesn't have the ability to perform this dance.
					new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_lack_skill_self").broadcast();
				}
			} else {
				// This dance name is invalid
				new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_unknown_self").broadcast();
			}
		} else {
			// This intent wants the creature to stop dancing
			stopDancing(dancer);
		}
	}

	private void startDancing(CreatureObject dancer, String danceId) {
		dancer.setAnimation(danceId);
		dancer.setPerformanceId(0);	// 0 - anything else will make it look like we're playing music
		dancer.setPerforming(true);
		dancer.setPosture(Posture.SKILL_ANIMATING);
		new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_start_self").broadcast();
	}

	private void stopDancing(CreatureObject dancer) {
		if (dancer.isPerforming()) {
			dancer.setPerforming(false);
			dancer.setPosture(Posture.UPRIGHT);
			dancer.setPerformanceCounter(0);
			dancer.setAnimation("");
			new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_stop_self").broadcast();
		} else {
			new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_not_performing").broadcast();
		}
	}

}
