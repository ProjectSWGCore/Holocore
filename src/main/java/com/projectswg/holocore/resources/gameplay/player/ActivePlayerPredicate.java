package com.projectswg.holocore.resources.gameplay.player;

import com.projectswg.common.data.encodables.tangible.Posture;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.player.PlayerFlags;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;

import java.util.function.Predicate;

public class ActivePlayerPredicate implements Predicate<Player> {
	@Override
	public boolean test(Player player) {
		CreatureObject creatureObject = player.getCreatureObject();
		PlayerObject playerObject = creatureObject.getPlayerObject();
		boolean afk = playerObject.isFlagSet(PlayerFlags.AFK);
		boolean offline = playerObject.isFlagSet(PlayerFlags.LD);
		boolean incapacitated = creatureObject.getPosture() == Posture.INCAPACITATED;
		boolean dead = creatureObject.getPosture() == Posture.DEAD;
		boolean cloaked = !creatureObject.isVisible();
		
		return !afk && !offline && !incapacitated && !dead && !cloaked;
	}
}
