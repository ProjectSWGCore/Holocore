package com.projectswg.holocore.services.gameplay.player;

import com.projectswg.holocore.services.gameplay.player.badge.BadgeManager;
import com.projectswg.holocore.services.gameplay.player.character.PlayerCharacterSheetService;
import com.projectswg.holocore.services.gameplay.player.experience.ExperienceManager;
import com.projectswg.holocore.services.gameplay.player.group.GroupManager;
import com.projectswg.holocore.services.gameplay.player.guild.GuildService;
import com.projectswg.holocore.services.gameplay.player.quest.QuestService;
import me.joshlarson.jlcommon.control.Manager;
import me.joshlarson.jlcommon.control.ManagerStructure;

@ManagerStructure(children = {
		BadgeManager.class,
		ExperienceManager.class,
		GroupManager.class,
		GuildService.class,
		QuestService.class,
		PlayerCharacterSheetService.class
})
public class PlayerManager extends Manager {
	
	public PlayerManager() {
		
	}
	
}
