package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent;
import com.projectswg.holocore.intents.gameplay.player.experience.skills.GrantSkillIntent.IntentType;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.PlayerRoleLoader.RoleInfo;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

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
		
		RoleInfo qualifyingSkills = DataLoader.playerRoles().getRoleBySkill(gsi.getSkillName());
		if (qualifyingSkills == null)
			return;
		
		PlayerObject playerObject = gsi.getTarget().getPlayerObject();
		assert playerObject != null;
		
		playerObject.setProfessionIcon(qualifyingSkills.getIndex());
	}
	
	private void changeRoleIcon(CreatureObject creature, int chosenIcon) {
		RoleInfo qualifyingSkills = DataLoader.playerRoles().getRoleByIndex(chosenIcon);
		if (qualifyingSkills == null) {
			Log.w("%s tried to use undefined role icon %d", creature, chosenIcon);
			return;
		}
		PlayerObject playerObject = creature.getPlayerObject();
		assert playerObject != null;
		
		if (creature.hasSkill(qualifyingSkills.getQualifyingSkill()))
			playerObject.setProfessionIcon(qualifyingSkills.getIndex());
	}
	
}
