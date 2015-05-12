package main;

import network.encryption.TestEncryption;
import network.encryption.TestFragmented;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import resources.TestSortedLinkedList;
import resources.objects.quadtree.TestQuadTree;
import resources.server_info.TestObjectDatabase;

@RunWith(Suite.class)
@SuiteClasses({
	TestEncryption.class,
	TestFragmented.class,
	TestQuadTree.class,
	TestObjectDatabase.class,
	TestSortedLinkedList.class
})
public class TestAll {
	
}
