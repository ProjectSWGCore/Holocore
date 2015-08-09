package resources.radial;

import java.util.ArrayList;
import java.util.List;

import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Scripts;

public class Radials {
	
	private static final String SCRIPT_PREFIX = "radial/";
	
	public static List<RadialOption> getRadialOptions(String script, Player player, SWGObject target, Object ... args) {
		List<RadialOption> options = new ArrayList<>();
		Scripts.invoke(SCRIPT_PREFIX + script, "getOptions", options, player, target, args);
		return options;
	}
	
	public static void handleSelection(String script, Player player, SWGObject target, RadialItem selection, Object ... args) {
		Scripts.invoke(SCRIPT_PREFIX + script, "handleSelection", player, target, selection, args);
	}
	
}
