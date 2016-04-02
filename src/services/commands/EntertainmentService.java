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
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;

/**
 *
 * @author Mads
 */
public class EntertainmentService extends Service {
	
	private final Map<String, Integer> danceMap;
	
	public EntertainmentService() {
		danceMap = new HashMap<>();
		registerForIntent(DanceIntent.TYPE);
	}

	@Override
	public boolean initialize() {
		danceMap.put("basic", 1);	// TODO load data from datatables/performance/performance.iff
		
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case DanceIntent.TYPE: handleDanceIntent((DanceIntent) i); break;
		}
	}
	
	private void handleDanceIntent(DanceIntent i) {
		CreatureObject dancer = i.getCreatureObject();
		String danceName = i.getDanceName();
		
		if(danceMap.containsKey(danceName)) {
			// The dance name is valid.
			// TODO check if the dancer is allowed to perform this dance.
			startDancing(dancer, "dance_" + danceMap.get(danceName));
		} else {
			// This dance name is invalid
			new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_unknown_self").broadcast();
			System.out.println("dance " + danceName + " is an invalid dance");	// DEBUG
		}
	}
	
	private void startDancing(CreatureObject dancer, String danceId) {
		System.out.println(dancer + " is now performing dance id: " + danceId);
		dancer.setAnimation(danceId);
		dancer.setPerformanceId(0);	// 0 - anything else will make it look like we're playing music
		dancer.setPerformanceCounter(dancer.getPerformanceCounter() + 1);	// TODO is this correct?
		dancer.setPerforming(true);
		dancer.setPosture(Posture.SKILL_ANIMATING);
		new ChatBroadcastIntent(dancer.getOwner(), "@performance:dance_start_self").broadcast();
	}
	
}
