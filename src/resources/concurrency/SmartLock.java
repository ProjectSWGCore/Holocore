/***********************************************************************************
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
package resources.concurrency;

import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SmartLock {
	
	private final Lock lock;
	private final Condition condition;
	
	public SmartLock() {
		this.lock = new ReentrantLock(true);
		this.condition = lock.newCondition();
	}
	
	public void lock() {
		lock.lock();
	}
	
	public void lockInterruptibly() throws InterruptedException {
		lock.lockInterruptibly();
	}
	
	public boolean tryLock() {
		return lock.tryLock();
	}
	
	public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
		return lock.tryLock(time, unit);
	}
	
	public void unlock() {
		lock.unlock();
	}
	
	public void await() throws InterruptedException {
		lock();
		try {
			condition.await();
		} finally {
			unlock();
		}
	}
	
	public void awaitUninterruptibly() {
		lock();
		try {
			condition.awaitUninterruptibly();
		} finally {
			unlock();
		}
	}
	
	public long awaitNanos(long nanosTimeout) throws InterruptedException {
		lock();
		try {
			return condition.awaitNanos(nanosTimeout);
		} finally {
			unlock();
		}
	}
	
	public boolean await(long time, TimeUnit unit) throws InterruptedException {
		lock();
		try {
			return condition.await(time, unit);
		} finally {
			unlock();
		}
	}
	
	public boolean awaitUntil(Date deadline) throws InterruptedException {
		lock();
		try {
			return condition.awaitUntil(deadline);
		} finally {
			unlock();
		}
	}
	
	public void signal() {
		lock();
		try {
			condition.signal();
		} finally {
			unlock();
		}
	}
	
	public void signalAll() {
		lock();
		try {
			condition.signalAll();
		} finally {
			unlock();
		}
	}
	
}
