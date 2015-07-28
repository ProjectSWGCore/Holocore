package utilities;

import java.io.FileReader;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Scripts {

	private static final String SCRIPTS = "scripts/";
	private static final String EXTENSION = ".js";
	private static final ScriptEngine ENGINE = new ScriptEngineManager().getEngineByName("nashorn");
	private static final Invocable INVOCABLE = (Invocable) ENGINE;
	
	// Prevents instantiation.
	private Scripts() {}
	
	/**
	 * @param script name of the script, relative to the scripts folder.
	 * @param function name of the specific function within the script.
	 * @param args to pass to the function.
	 * @return whatever the function returns. If the function doesn't have a return statement, this method returns {@code null}.
	 * If an exception occurs, {@code null} is returned.
	 */
	@SuppressWarnings("unchecked")
	public static <T> T invoke(String script, String function, Object... args) {
		try {
			ENGINE.eval(new FileReader(SCRIPTS + script + EXTENSION));
			return (T) INVOCABLE.invokeFunction(function, args);
		} catch(Throwable t) {
			t.printStackTrace();
			return null;
		}
	}
	
}
