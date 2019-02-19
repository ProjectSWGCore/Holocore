package com.projectswg.holocore.resources.support.global.commands.callbacks.survey;

import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSurveyingIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResourceSpawn;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class CmdRequestSurvey implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (!(target instanceof TangibleObject))
			return;
		
		GalacticResource resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args);
		if (resource == null) {
			SystemMessageIntent.broadcastPersonal(player, "Unknown resource: " + args);
			return;
		}
		if (player.getCreatureObject().hasCommand("admin")) {
			List<GalacticResourceSpawn> spawns = new ArrayList<>(GalacticResourceContainer.getContainer().getTerrainResourceSpawns(resource, player.getCreatureObject().getTerrain()));
			spawns.sort(Comparator.comparingDouble(c -> player.getCreatureObject().getLocation().flatDistanceTo(c.getX(), c.getZ())));
			for (GalacticResourceSpawn spawn : spawns) {
				SystemMessageIntent.broadcastPersonal(player, "Spawn: " + spawn);
			}
		}
		StartSurveyingIntent.broadcast(player.getCreatureObject(), resource);
	}
	
}
