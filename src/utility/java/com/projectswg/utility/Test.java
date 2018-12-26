package com.projectswg.utility;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.ObjectData;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.log.log_wrapper.ConsoleLogWrapper;

import java.io.File;

public class Test {
	
	public static void main(String[] args) {
		Log.addWrapper(new ConsoleLogWrapper());
//		long start = System.nanoTime();
//		DataLoader.buildouts();
//		long end = System.nanoTime();
//		Log.i("Took: %.3fms", (end - start) / 1E6);
		ObjectData objectData = (ObjectData) ClientFactory.getInfoFromFile(new File("clientdata/object/mobile/vehicle/shared_speederbike_swoop.iff"));
		System.out.println(objectData.getAttributes().toString().replace(", ", System.lineSeparator()));
	}
	
}
