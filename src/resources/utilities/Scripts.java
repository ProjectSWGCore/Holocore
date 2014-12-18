package resources.utilities;

import java.io.File;

import org.python.core.Py;
import org.python.util.PythonInterpreter;

public final class Scripts {
	private static final String SCRIPTS_PATH = "scripts/";
	
	// TODO: Variable arguments?
	
	public static void execute(String script, String method) {
		if (!scriptExists(script))
			return;
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(SCRIPTS_PATH + script);
		interp.get(method).__call__();
	}
	
	public static void execute(String script, String method, Object arg1) {
		if (!scriptExists(script))
			return;
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(SCRIPTS_PATH + script);
		interp.get(method).__call__(Py.java2py(arg1));
	}
	
	public static void execute(String script, String method, Object arg1, Object arg2) {
		if (!scriptExists(script))
			return;
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(SCRIPTS_PATH + script);
		interp.get(method).__call__(Py.java2py(arg1), Py.java2py(arg2));
	}
	
	public static void execute(String script, String method, Object arg1, Object arg2, Object arg3) {
		if (!scriptExists(script))
			return;
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(SCRIPTS_PATH + script);
		interp.get(method).__call__(Py.java2py(arg1), Py.java2py(arg2), Py.java2py(arg3));
	}
	
	public static void execute(String script, String method, Object arg1, Object arg2, Object arg3, Object arg4) {
		if (!scriptExists(script))
			return;
		
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(SCRIPTS_PATH + script);
		interp.get(method).__call__(Py.java2py(arg1), Py.java2py(arg2), Py.java2py(arg3), Py.java2py(arg4));
	}
	
	private static boolean scriptExists(String file) {
		return new File(SCRIPTS_PATH + file).exists();
	}
}
