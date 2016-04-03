/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
			danceMap.put((String) performanceTable.getCell(i, 0), (int) performanceTable.getCell(i, 5));	// performanceName, danceVisualId
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
		dancer.setPerformanceCounter(0);	// TODO is this correct?
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
