package com.projectswg.holocore.resources.support.objects.radial.collection;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.gameplay.player.collections.GrantClickyCollectionIntent;
import com.projectswg.holocore.resources.gameplay.player.collections.CollectionItem;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public class WorldItemRadial implements RadialHandlerInterface {
	
	private final CollectionItem details;
	
	public WorldItemRadial(CollectionItem details) {
		this.details = details;
	}
	
	public void getOptions(List<RadialOption> options, Player player, SWGObject target) {
		RadialOption use = null;
		for (RadialOption option : options) {
			if (option.getOptionType() == RadialItem.ITEM_USE.getId()) {
				use = option;
				break;
			}
		}
		if (use == null) {
			use = new RadialOption(RadialItem.ITEM_USE);
			options.add(0, use);
		}
		use.setOverriddenText("@collection:consume_item");
	}
	
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		if (selection != RadialItem.ITEM_USE)
			return;
		
		new GrantClickyCollectionIntent(player.getCreatureObject(), target, details).broadcast();
	}
	
}
