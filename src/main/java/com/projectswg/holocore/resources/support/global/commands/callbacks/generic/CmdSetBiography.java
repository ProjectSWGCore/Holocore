package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.common.network.packets.swg.zone.object_controller.BiographyUpdate;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSetBiography implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		if (args.length() > 1025) {
			return;
		}
		
		CreatureObject creatureObject = player.getCreatureObject();
		
		creatureObject.getPlayerObject().setBiography(args);
		
		player.sendPacket(new BiographyUpdate(creatureObject.getObjectId(), creatureObject.getObjectId(), args));
	}
	
}
