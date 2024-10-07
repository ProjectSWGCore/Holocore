open module holocore {
	requires java.logging;
	requires java.management;
	requires jdk.management;
	
	requires org.jetbrains.annotations;
	requires org.mongodb.driver.sync.client;
	requires org.mongodb.bson;
	requires org.mongodb.driver.core;
	requires me.joshlarson.jlcommon;
	requires me.joshlarson.jlcommon.network;
	requires me.joshlarson.jlcommon.argparse;
	requires me.joshlarson.websocket;
	
	requires com.projectswg.common;
	requires fast.json;
	requires kotlin.stdlib;
	requires kotlin.reflect;
}
