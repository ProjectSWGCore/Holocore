package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.GrantSkillIntent.IntentType;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerRoleLoader.RoleInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.Collection;

public class ExperienceRoleService extends Service {
	
	public ExperienceRoleService() {
		
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof ChangeRoleIconChoice) {
			ChangeRoleIconChoice iconChoice = (ChangeRoleIconChoice) packet;
			changeRoleIcon(gpi.getPlayer().getCreatureObject(), iconChoice.getIconChoice());
		}
	}
	
	@IntentHandler
	private void handleGrantSkillIntent(GrantSkillIntent gsi) {
		if (gsi.getIntentType() != IntentType.GIVEN)
			return;
		
		Collection<RoleInfo> roles = DataLoader.Companion.playerRoles().getRolesBySkill(gsi.getSkillName());
		if (roles.isEmpty()) {
			return;
		}
		
		PlayerObject playerObject = gsi.getTarget().getPlayerObject();
		assert playerObject != null;
		
		RoleInfo role = roles.iterator().next();
		playerObject.setProfessionIcon(role.getIndex());
	}
	
	private void changeRoleIcon(CreatureObject creature, int chosenIcon) {
		Collection<RoleInfo> roles = DataLoader.Companion.playerRoles().getRolesByIndex(chosenIcon);
		if (roles.isEmpty()) {
			Log.w("%s tried to use undefined role icon %d", creature, chosenIcon);
			return;
		}
		PlayerObject playerObject = creature.getPlayerObject();
		assert playerObject != null;
		
		boolean creatureQualifiedForRoleIcon = roles.stream().anyMatch(role -> creature.hasSkill(role.getQualifyingSkill()));
		
		if (creatureQualifiedForRoleIcon) {
			playerObject.setProfessionIcon(chosenIcon);
		}
	}
	
}
