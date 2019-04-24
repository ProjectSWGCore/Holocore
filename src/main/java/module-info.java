open module holocore {
	requires java.sql;
	requires java.management;
	requires jdk.management;
	
	requires org.jetbrains.annotations;
	requires org.mongodb.driver.sync.client;
	requires org.mongodb.bson;
	requires org.mongodb.driver.core;
	requires me.joshlarson.jlcommon;
	requires me.joshlarson.jlcommon.network;
	
	requires com.projectswg.common;
	requires fast.json;
	requires commons.cli;
	requires kotlin.stdlib;
}
