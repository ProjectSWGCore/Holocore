package services.commands;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import intents.BuffIntent;
import intents.SkillModIntent;
import intents.object.ObjectCreatedIntent;
import resources.Buff;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.creature.CreatureObject;
import utilities.Scripts;

public class BuffService extends Service {
	
	// 100 metres is the max distance you can be away from the source of the buff before it's removed.
//	private static final byte GROUP_BUFF_RANGE = 100;	
	
	private final DelayQueue<BuffDelayed> buffRemoval;
	private final ExecutorService executor;
	private boolean stopBuffRemover;
	private final Map<String, BuffData> dataMap;
	
	public BuffService() {
		registerForIntent(BuffIntent.TYPE);
		registerForIntent(ObjectCreatedIntent.TYPE);
		
		buffRemoval = new DelayQueue<>();
		executor = Executors.newSingleThreadScheduledExecutor();
		dataMap = new HashMap<>();
	}
	
	@Override
	public boolean initialize() {
		// TODO read buff datatable into dataMap??
		
		executor.execute(new BuffRemover());
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		switch(i.getType()) {
			case BuffIntent.TYPE:
				BuffIntent bi = (BuffIntent) i;
				
				if(bi.isRemove()) {
					handleBuffRemove(bi);
				} else {
					handleBuffAdd(bi);
				}
				
				break;
			case ObjectCreatedIntent.TYPE: handleObjectCreation((ObjectCreatedIntent) i); break;
		}
	}
	
	@Override
	public boolean terminate() {
		stopBuffRemover = true;
		executor.shutdown();
		
		return super.terminate();
	}
	
	private void handleObjectCreation(ObjectCreatedIntent oci) {
		if(oci.getObject() instanceof CreatureObject) {
			// If this is a creature, we'll want to check if they have any buffs that we should start managing.
		}
	}
	
	private void handleBuffAdd(BuffIntent bi) {
		CreatureObject receiver = bi.getReceiver();
		CreatureObject buffer = bi.getBuffer();
		int duration = 600;
		Buff buff = new Buff(buffer.getObjectId(), receiver.getPlayerObject().getPlayTime(), duration, 1337f);
		String buffName = bi.getBuffName();
		
		receiver.addBuff(buffName, buff);
		
		if(duration >= 0) {
			// If this buff doesn't last forever, we'll schedule it for removal in the future
			buffRemoval.put(new BuffDelayed(buff, buffName, receiver));
		}
	}
	
	private void handleBuffRemove(BuffIntent bi) {
		removeBuff(bi.getReceiver(), bi.getBuffName(), false);
	}
	
	private void removeBuff(CreatureObject creature, String buffName, boolean expired) {
		// Get the BuffData for this buff name.
		BuffData buffData = dataMap.get(buffName);
		Buff buff = creature.getBuffByName(buffName);
		
		// Check if this buff can be stacked
		if(buffData.maxStackCount > 1 && !expired) {
			// Check if this buff has been stacked
			if(buff.getStackCount() > 1) {
				// Get the adjustment from the 
				int adjustment = Scripts.invoke(buffName, "onStackChange", creature, buffName);
				
				// If it has, reduce the stack count and reset the duration.
				creature.adjustBuffStackCount(buffName, adjustment);
			}
		} else {
			// If this is a group buff, skillmods and buffs need to be removed from the group as well.
			CreatureObject[] affectedCreatures = null;
			
			// Remove the buff from the creature(s)
			creature.removeBuff(buffName);
			
			// TODO remove skillmods
			new SkillModIntent("skillModName", 0, -10, creature);
		}
	}
	
	private class BuffRemover implements Runnable {
		@Override
		public void run() {
			while(!stopBuffRemover) {
				try {
					BuffDelayed buffToRemove = buffRemoval.take();	// Block here until a buff expires
					
					removeBuff(buffToRemove.owner, buffToRemove.buffName, true);
				} catch (InterruptedException e) {
					
				}
			}
		}
	}
	
	private class BuffDelayed implements Delayed {
		
		private final Buff buff;
		private final String buffName;
		private final CreatureObject owner;
		
		private BuffDelayed(Buff buff, String buffName, CreatureObject owner) {
			this.buff = buff;
			this.buffName = buffName;
			this.owner = owner;
		}
		
		@Override
		public int compareTo(Delayed o) {
			return Long.compare(buff.getEndTime(), ((BuffDelayed) o).buff.getEndTime());
		}

		@Override
		public long getDelay(TimeUnit timeUnit) {
			return timeUnit.convert(buff.getEndTime() - System.currentTimeMillis(), TimeUnit.MILLISECONDS);
		}
		
	}
	
	/**
	 * @author Mads
	 * Each instance of this class holds the base information
	 * for a specific buff name. Thus, we only store shared data that each {@code Buff} instance
	 * would otherwise have been holding.
	 * 
	 * Example: Instead of each {@code Buff} instance storing the max amount of times you can
	 * stack it, a shared class stores that information.
	 * 
	 * With many {@code Buff} instances in play, this will result in memory
	 * usage reduction.
	 */
	private class BuffData {
		private final long maxStackCount;
//		private final String effect1Name;
//		private final int effect1Value;
//		private final String effect2Name;
//		private final int effect2Value;
//		private final String effect3Name;
//		private final int effect3Value;
//		private final String effect4Name;
//		private final int effect4Value;
//		private final String effect5Name;
//		private final int effect5Value;
		
		private BuffData(long maxStackCount) {
			this.maxStackCount = maxStackCount;
		}
		
	}
	
}
