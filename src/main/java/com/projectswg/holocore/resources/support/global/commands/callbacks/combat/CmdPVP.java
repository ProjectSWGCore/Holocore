package com.projectswg.holocore.resources.support.global.commands.callbacks.combat;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.ServerData;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class CmdPVP implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		
		// Ziggy was using this to test until recruiters are enabled.
		
		if (!args.isEmpty()) {
			if (args.contains("imperial")) {
				new FactionIntent(creature, ServerData.INSTANCE.getFactions().getFaction("imperial")).broadcast();
			} else if (args.contains("rebel")) {
				new FactionIntent(creature, ServerData.INSTANCE.getFactions().getFaction("rebel")).broadcast();
			} else {
				new FactionIntent(creature, ServerData.INSTANCE.getFactions().getFaction("neutral")).broadcast();
			}
		} else if (creature.getPvpFaction() != PvpFaction.NEUTRAL) {
			new FactionIntent(creature, FactionIntent.FactionIntentType.SWITCHUPDATE).broadcast();
		} else {
			SystemMessageIntent.broadcastPersonal(player, "@faction_recruiter:not_aligned");
		}
	}
	
}
