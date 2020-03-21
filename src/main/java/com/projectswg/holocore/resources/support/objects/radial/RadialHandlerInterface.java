package com.projectswg.holocore.resources.support.objects.radial;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;

import java.util.Collection;

public interface RadialHandlerInterface {
	void getOptions(Collection<RadialOption> options, Player player, SWGObject target);
	void handleSelection(Player player, SWGObject target, RadialItem selection);
}
