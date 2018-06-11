package com.projectswg.holocore.resources.support.objects.radial.object.survey;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyToolIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.object.UsableObjectRadial;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

public class ObjectSurveyToolRadial extends UsableObjectRadial {
	
	public ObjectSurveyToolRadial() {
		
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (selection != RadialItem.ITEM_USE) {
			return;
		}
		
		new StartSurveyToolIntent(player.getCreatureObject(), target).broadcast();
	}
	
}
