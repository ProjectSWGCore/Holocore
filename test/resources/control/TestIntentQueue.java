package resources.control;

import java.util.Arrays;

import intents.LoginEventIntent;
import intents.PlayerEventIntent;
import intents.object.ObjectCreatedIntent;
import intents.object.ObjectTeleportIntent;
import intents.object.UpdateObjectAwareness;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class TestIntentQueue {
	
	@Test
	public void testQueue() {
		Intent [] intents = new Intent[5];
		intents[0] = new PlayerEventIntent(null, null);
		intents[1] = new ObjectTeleportIntent(null, null);
		intents[2] = new UpdateObjectAwareness(null, false);
		intents[3] = new ObjectCreatedIntent(null);
		intents[4] = new LoginEventIntent(0, null);
		IntentQueue queue = new IntentQueue();
		long start = System.nanoTime();
		queue.addAll(Arrays.asList(intents));
		long end = System.nanoTime();
		System.out.println((end-start)/1E6/5 + "ms");
		int iterI = 0;
		Assert.assertEquals(queue.size(), 5);
		Assert.assertFalse(queue.isEmpty());
		for (Intent i : queue) {
			Assert.assertEquals("Failed for intent #"+iterI, intents[iterI], i);
			iterI++;
		}
		Assert.assertEquals(queue.size(), 5);
		Assert.assertFalse(queue.isEmpty());
		for (int i = 0; i < intents.length; i++) {
			Intent intent = queue.poll();
			Assert.assertEquals(5 - i - 1, queue.size());
			Assert.assertEquals("Failed for intent #"+i, intents[i], intent);
		}
		Assert.assertEquals(queue.size(), 0);
		Assert.assertTrue(queue.isEmpty());
	}
	
}
