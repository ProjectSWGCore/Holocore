package utilities;

import java.util.concurrent.ThreadFactory;

import org.python.google.common.util.concurrent.ThreadFactoryBuilder;

public class ThreadUtilities {
	
	public static ThreadFactory newThreadFactory(String pattern) {
		return new ThreadFactoryBuilder().setNameFormat(pattern).build();
	}
	
}
