/************************************************************************************
 * Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
 *                                                                                  *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
 * Our goal is to create an emulator which will provide a server for players to     *
 * continue playing a game similar to the one they used to play. We are basing      *
 * it on the final publish of the game prior to end-game events.                    *
 *                                                                                  *
 * This file is part of Holocore.                                                   *
 *                                                                                  *
 * -------------------------------------------------------------------------------- *
 *                                                                                  *
 * Holocore is free software: you can redistribute it and/or modify                 *
 * it under the terms of the GNU Affero General Public License as                   *
 * published by the Free Software Foundation, either version 3 of the               *
 * License, or (at your option) any later version.                                  *
 *                                                                                  *
 * Holocore is distributed in the hope that it will be useful,                      *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
 * GNU Affero General Public License for more details.                              *
 *                                                                                  *
 * You should have received a copy of the GNU Affero General Public License         *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
 *                                                                                  *
 ***********************************************************************************/
package resources.commands;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import utilities.ThreadUtilities;

/**
 * Each {@link CreatureObject} has their own {@code CombatQueue]. 
 * @author mads
 */
public final class CombatQueue extends AbstractQueue<Command> {
	
	private static final byte PERIOD = 1;
	
	private final QueuedCommandExecutor commandExecutor;
	private final ScheduledExecutorService executorService;
	private final Queue<Command> queue;

	public CombatQueue(QueuedCommandExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;
		executorService = Executors.newSingleThreadScheduledExecutor(ThreadUtilities.newThreadFactory("combat-queue"));
		queue = new PriorityBlockingQueue<>();	// The PriorityQueue has natural ordering. The Command class implements Comparable for this purpose.
	}
	
	public void start() {
		executorService.scheduleAtFixedRate(() -> commandExecutor.executeQueuedCommand(queue.poll()), PERIOD, PERIOD, TimeUnit.SECONDS);
	}
	
	public void stop() {
		executorService.shutdown();
	}
	
	@Override
	public Iterator<Command> iterator() {
		return queue.iterator();
	}

	@Override
	public int size() {
		return queue.size();
	}

	@Override
	public boolean offer(Command e) {
		return queue.offer(e);
	}

	@Override
	public Command poll() {
		return queue.poll();
	}

	@Override
	public Command peek() {
		return queue.peek();
	}
	
	@FunctionalInterface
	public interface QueuedCommandExecutor {
		public void executeQueuedCommand(Command queuedCommand);
	}
	
}
