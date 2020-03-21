package com.projectswg.holocore.resources.support.objects.radial.collection;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.player.collections.GrantClickyCollectionIntent;
import com.projectswg.holocore.resources.gameplay.player.collections.CollectionItem;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.Collection;

public class WorldItemRadial implements RadialHandlerInterface {
	
	private final CollectionItem details;
	
	public WorldItemRadial(CollectionItem details) {
		this.details = details;
	}
	
	public void getOptions(Collection<RadialOption> options, Player player, SWGObject target) {
		options.add(RadialOption.create(RadialItem.ITEM_USE, "@collection:consume_item"));
	}
	
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		new GrantClickyCollectionIntent(player.getCreatureObject(), target, details).broadcast();
	}
	
}
