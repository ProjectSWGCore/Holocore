package resources;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import resources.collections.SWGListTest;
import resources.objects.quadtree.TestQuadTree;
import resources.server_info.TestObjectDatabase;
import resources.services.TestConfig;

@RunWith(Suite.class)
@SuiteClasses({
	SWGListTest.class,
	TestSortedLinkedList.class,
	TestWeatherType.class,
	TestQuadTree.class,
	TestObjectDatabase.class,
	TestConfig.class,
	TestQuaternion.class
})
public class TestResources {
	
}
