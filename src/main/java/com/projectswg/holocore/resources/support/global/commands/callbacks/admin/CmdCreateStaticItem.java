package com.projectswg.holocore.resources.support.global.commands.callbacks.admin;

import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.resources.support.global.commands.ICmdCallback;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.services.support.objects.items.StaticItemService;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CmdCreateStaticItem implements ICmdCallback {
	
	@Override
	public void execute(@NotNull Player player, SWGObject target, @NotNull String args) {
		CreatureObject creature = player.getCreatureObject();
		TangibleObject inventory = (TangibleObject) creature.getSlottedObject("inventory");
		
		new CreateStaticItemIntent(creature, inventory, new CreateStaticItemCallback(player), args).broadcast();
	}
	
	private static class CreateStaticItemCallback implements StaticItemService.ObjectCreationHandler {
		
		@NotNull
		private final Player player;
		
		public CreateStaticItemCallback(@NotNull Player player) {this.player = player;}
		
		@Override
		public void success(List<SWGObject> createdObjects) {
			new SystemMessageIntent(player, "@system_msg:give_item_success").broadcast();
		}
		
		@Override
		public void containerFull() {
			new SystemMessageIntent(player, "@system_msg:give_item_failure").broadcast();
		}
		
		@Override
		public boolean isIgnoreVolume() {
			return true;    // This is an admin command - coontainer restrictions is for peasants!
		}
	}
}
