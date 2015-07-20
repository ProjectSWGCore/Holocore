package main;

import network.encryption.TestEncryption;
import network.encryption.TestFragmented;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import resources.TestResources;

@RunWith(Suite.class)
@SuiteClasses({
	TestEncryption.class,
	TestFragmented.class,
	TestResources.class
})
public class TestAll {
	
}
