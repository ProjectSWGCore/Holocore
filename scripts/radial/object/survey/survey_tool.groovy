import intents.crafting.survey.StartSurveyToolIntent
import resources.objects.SWGObject
import resources.player.Player
import com.projectswg.common.data.radial.RadialItem
import com.projectswg.common.data.radial.RadialOption
import com.projectswg.common.debug.Log;

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	options.add(new RadialOption(RadialItem.ITEM_USE))
	options.add(new RadialOption(RadialItem.EXAMINE))
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	new StartSurveyToolIntent(player.getCreatureObject(), target).broadcast();
}
