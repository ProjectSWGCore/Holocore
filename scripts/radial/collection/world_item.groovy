import resources.objects.SWGObject
import resources.player.Player
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	options.add(new RadialOption(RadialItem.ITEM_USE))
	options.get(0).setOverriddenText("@collection:consume_item")
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	switch (selection) {
		case RadialItem.ITEM_USE:
			//Log.d("bazaar.groovy", "Bazaar Selection: ITEM_USE")
			break
	}
}
