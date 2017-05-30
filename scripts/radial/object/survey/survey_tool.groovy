import intents.crafting.survey.StartSurveyingIntent
import resources.objects.SWGObject
import resources.player.Player
import resources.radial.RadialItem
import resources.radial.RadialOption
import com.projectswg.common.debug.Log;

static def getOptions(List<RadialOption> options, Player player, SWGObject target, Object... args) {
	options.add(new RadialOption(RadialItem.ITEM_USE))
	options.add(new RadialOption(RadialItem.EXAMINE))
}

static def handleSelection(Player player, SWGObject target, RadialItem selection, Object... args) {
	new StartSurveyingIntent(player.getCreatureObject(), target).broadcast();
}
