package com.projectswg.holocore.resources.support.global.commands.callbacks.combat;

import com.projectswg.common.data.encodables.tangible.PvpFaction;
import com.projectswg.holocore.intents.gameplay.gcw.faction.FactionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.Intent;

public final class CmdPVP implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		CreatureObject creature = player.getCreatureObject();
		
		// Ziggy was using this to test until recruiters are enabled.
		
		if (!args.isEmpty()) {
			if (args.contains("imperial")) {
				new FactionIntent(creature, PvpFaction.IMPERIAL).broadcast();
			} else if (args.contains("rebel")) {
				new FactionIntent(creature, PvpFaction.REBEL).broadcast();
			} else {
				new FactionIntent(creature, PvpFaction.NEUTRAL).broadcast();
			}
		} else if (creature.getPvpFaction() != PvpFaction.NEUTRAL) {
			new FactionIntent(creature, FactionIntent.FactionIntentType.SWITCHUPDATE).broadcast();
		} else {
			SystemMessageIntent.broadcastPersonal(player, "@faction_recruiter:not_aligned");
		}
	}
	
}
