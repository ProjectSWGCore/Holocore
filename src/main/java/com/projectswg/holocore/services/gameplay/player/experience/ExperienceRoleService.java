package com.projectswg.holocore.services.gameplay.player.experience;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.zone.object_controller.ChangeRoleIconChoice;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ExperienceRoleService extends Service {
	
	private final Map<Integer, Set<String>> roleIconMap;
	
	public ExperienceRoleService() {
		roleIconMap = new HashMap<>();
	}
	
	@Override
	public boolean initialize() {
		DatatableData roleIconTable = (DatatableData) ClientFactory.getInfoFromFile("datatables/role/role.iff");
		
		for (int i = 0; i < roleIconTable.getRowCount(); i++) {
			int iconIndex = (int) roleIconTable.getCell(i, 0);
			String qualifyingSkill = (String) roleIconTable.getCell(i, 2);
			
			Set<String> qualifyingSkills = roleIconMap.computeIfAbsent(iconIndex, k -> new HashSet<>());
			
			qualifyingSkills.add(qualifyingSkill);
		}
		return true;
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent gpi) {
		SWGPacket packet = gpi.getPacket();
		if (packet instanceof ChangeRoleIconChoice) {
			ChangeRoleIconChoice iconChoice = (ChangeRoleIconChoice) packet;
			changeRoleIcon(gpi.getPlayer().getCreatureObject(), iconChoice.getIconChoice());
		}
	}
	
	private void changeRoleIcon(CreatureObject creature, int chosenIcon) {
		Set<String> qualifyingSkills = roleIconMap.get(chosenIcon);
		if (qualifyingSkills == null) {
			Log.w("%s tried to use undefined role icon %d", creature, chosenIcon);
			return;
		}
		PlayerObject playerObject = creature.getPlayerObject();
		assert playerObject != null;
		
		for (String qualifyingSkill : qualifyingSkills) {
			if (creature.hasSkill(qualifyingSkill)) {
				playerObject.setProfessionIcon(chosenIcon);
				return;
			}
		}
		Log.e("%s could not be given role icon %d - does not have qualifying skill! Qualifying: %s", creature, chosenIcon, qualifyingSkills);
	}
	
}
