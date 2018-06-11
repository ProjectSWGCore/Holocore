package com.projectswg.holocore.resources.support.global.commands.callbacks.survey;

import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyingIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public final class CmdRequestSurvey implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		GalacticResource resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args);
		if (resource == null) {
			SystemMessageIntent.broadcastPersonal(player, "Unknown resource: " + args);
			return;
		}
		if (player.getCreatureObject().hasAbility("admin")) {
			List<GalacticResourceSpawn> spawns = GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, player.getCreatureObject().getTerrain());
			for (GalacticResourceSpawn spawn : spawns) {
				SystemMessageIntent.broadcastPersonal(player, "Spawn: " + spawn);
			}
		}
		new StartSurveyingIntent(player.getCreatureObject(), resource).broadcast();
	}
	
}
