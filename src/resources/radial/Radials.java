package resources.radial;

import java.util.ArrayList;
import java.util.List;

import utilities.Scripts;

public class Radials {
	
	private static final String SCRIPT_PREFIX = "radial/";
	
	public static List<RadialOption> getRadialOptions(String script, Object ... args) {
		List<RadialOption> options = new ArrayList<>();
		Scripts.invoke(SCRIPT_PREFIX + script, "getOptions", options, args);
		return options;
	}
	
	public static void handleSelection(String script, Object ... args) {
		
	}
	
}
