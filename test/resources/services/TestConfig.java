package resources.services;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import resources.server_info.Config;

@RunWith(JUnit4.class)
public class TestConfig {
	
	private static final File file = new File("test_config.cfg");
	
	static {
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}
		file.deleteOnExit();
	}
	
	@Test
	public void testGetSet() {
		Config c = new Config(file);
		c.setProperty("TEST-VALUE", true);
		c.save();
		c.load();
		Assert.assertTrue(c.getBoolean("TEST-VALUE", false));
		file.delete();
	}
	
}
