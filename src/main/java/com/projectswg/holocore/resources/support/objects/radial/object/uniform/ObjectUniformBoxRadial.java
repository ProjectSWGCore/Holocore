package com.projectswg.holocore.resources.support.objects.radial.object.uniform;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.intents.support.objects.items.OpenUniformBoxIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.radial.RadialHandlerInterface;
import com.projectswg.holocore.resources.support.objects.radial.object.SWGObjectRadial;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.List;

public class ObjectUniformBoxRadial extends SWGObjectRadial {
	
	public ObjectUniformBoxRadial() {
		
	}
	
	@Override
	public void handleSelection(Player player, SWGObject target, RadialItem selection) {
		DestroyObjectIntent.broadcast(target);
		OpenUniformBoxIntent.broadcast(player);
	}
	
}
