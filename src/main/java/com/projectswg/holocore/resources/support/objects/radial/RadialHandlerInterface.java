package com.projectswg.holocore.resources.support.objects.radial;

import com.projectswg.common.data.radial.RadialItem;
import com.projectswg.common.data.radial.RadialOption;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public interface RadialHandlerInterface {
	void getOptions(@NotNull Collection<RadialOption> options, @NotNull Player player, @NotNull SWGObject target);
	void handleSelection(@NotNull Player player, @NotNull SWGObject target, @NotNull RadialItem selection);
}
