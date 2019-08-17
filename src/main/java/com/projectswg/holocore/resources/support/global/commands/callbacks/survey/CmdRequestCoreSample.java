package com.projectswg.holocore.resources.support.global.commands.callbacks.survey;

import com.projectswg.holocore.intents.gameplay.crafting.survey.StartSamplingIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.GalacticResource;
import com.projectswg.holocore.resources.gameplay.crafting.resource.galactic.storage.GalacticResourceContainer;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdRequestCoreSample implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		GalacticResource resource = GalacticResourceContainer.getContainer().getGalacticResourceByName(args);
		if (resource == null) {
			SystemMessageIntent.broadcastPersonal(player, "@survey:sample_select_type");
			return;
		}
		new StartSamplingIntent(player.getCreatureObject(), resource).broadcast();
	}
	
}
