package com.projectswg.holocore.resources.support.global.commands.callbacks.generic;

import com.projectswg.holocore.intents.gameplay.player.experience.skills.SurrenderSkillIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

public final class CmdSurrenderSkill implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		SurrenderSkillIntent.broadcast(player.getCreatureObject(), args);
	}
	
}
