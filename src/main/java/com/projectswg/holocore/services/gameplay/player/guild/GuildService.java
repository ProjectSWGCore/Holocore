package com.projectswg.holocore.services.gameplay.player.guild;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.PlayerEvent;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.awareness.AwarenessType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.guild.GuildObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;

import java.util.List;

/**
 * Responsibilities:
 * <ol>
 *     <li>Ensures existence of a singleton {@link GuildObject}</li>
 *     <li>Ensures every player is made aware of the {@link GuildObject}</li>
 * </ol>
 */
public class GuildService extends Service {
	
	private GuildObject guildObject;
	
	public GuildService() {
	
	}
	
	@Override
	public boolean initialize() {
		if (guildObject == null) {
			// A guild object doesn't already exist. Let's create one.
			guildObject = (GuildObject) ObjectCreator.createObjectFromTemplate("object/guild/shared_guild_object.iff");
			ObjectCreatedIntent.broadcast(guildObject);
		}
		
		return super.start();
	}
	
	@IntentHandler
	private void handleObjectCreated(ObjectCreatedIntent intent) {
		SWGObject genericObject = intent.getObject();
		
		if (genericObject.getGameObjectType() == GameObjectType.GOT_GUILD) {
			guildObject = (GuildObject) genericObject;
		}
	}
	
	/**
	 * Every player should be aware of the GuildObject in order for guilds and, oddly enough, GCW regions.to work
	 */
	@IntentHandler
	private void handlePlayerEvent(PlayerEventIntent intent) {
		if (PlayerEvent.PE_ZONE_IN_SERVER == intent.getEvent()) {
			CreatureObject creature = intent.getPlayer().getCreatureObject();
			
			creature.setAware(AwarenessType.GUILD, List.of(guildObject));
		}
	}
}
