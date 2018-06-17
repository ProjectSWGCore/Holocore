package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;

public final class CmdMoney implements ICmdCallback {
	
	@Override
	public void execute(Player player, SWGObject target, String args) {
		String [] argSplit = args.split(" ");
		if (argSplit.length < 2) {
			SystemMessageIntent.broadcastPersonal(player, "Invalid Arguments: " + args);
			return;
		}
		CreatureObject creature = player.getCreatureObject();
		switch (argSplit[0]) {
			case "bank":
				creature.setBankBalance(creature.getBankBalance() + Long.valueOf(argSplit[1]));
				break;
			case "cash":
				creature.setCashBalance(creature.getCashBalance() + Long.valueOf(argSplit[1]));
				break;
			default:
				SystemMessageIntent.broadcastPersonal(player, "Unknown Destination: " + argSplit[0]);
				break;
		}
	}
	
}
