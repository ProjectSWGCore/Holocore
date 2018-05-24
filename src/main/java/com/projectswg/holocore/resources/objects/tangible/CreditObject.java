/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.objects.tangible;

import com.projectswg.holocore.intents.object.ContainerTransferIntent;
import com.projectswg.holocore.intents.object.DestroyObjectIntent;
import com.projectswg.holocore.resources.containers.ContainerResult;
import com.projectswg.holocore.resources.objects.SWGObject;
import com.projectswg.holocore.resources.objects.creature.CreatureObject;
import me.joshlarson.jlcommon.log.Log;
import me.joshlarson.jlcommon.utilities.Arguments;
import org.jetbrains.annotations.NotNull;

public class CreditObject extends TangibleObject {
	private long amount;

	public CreditObject(long objectId) {
		super(objectId);
	}
	
	/**
	 * Moves this object to the passed container if the requester has the MOVE permission for the container
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	@Override
	public ContainerResult moveToContainer(@NotNull SWGObject requester, SWGObject container) {
		if (!(requester instanceof CreatureObject && ((CreatureObject) requester).isPlayer()))
			return super.moveToContainer(requester, container);
		
		assert amount > 0 : "amount must be set";
		
		SWGObject parent = getParent();
		
		if (parent == container) // One could be null, and this is specifically an instance-based check
			return ContainerResult.SUCCESS;
		
		ContainerResult result = moveToAccountChecks(requester);
		if (result != ContainerResult.SUCCESS)
			return result;
		
		((CreatureObject) requester).addToCash(amount);
		
//		Set<Player> oldObservers = getObserversAndParent();
//		if (parent != null)
//			parent.removeObject(this);
//		AwarenessUtilities.callForOldObserver(oldObservers, Collections.emptySet(), this::destroyObject);
		new ContainerTransferIntent(this, parent, null).broadcast();
		new DestroyObjectIntent(this).broadcast();
		
		return ContainerResult.SUCCESS;
	}
	
	protected ContainerResult moveToAccountChecks(SWGObject requester) {
		if (requester == null)
			return ContainerResult.SUCCESS;
		
		if (!getContainerPermissions().canMove(requester, this)) {
			Log.w("No permission 'MOVE' for requestor %s with object %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		
		return ContainerResult.SUCCESS;
	}
	
	public long getAmount() {
		return amount;
	}
	
	public void setAmount(long amount) {
		Arguments.validate(amount > 0, "Amount must be greater than 0");
		this.amount = amount;
		setObjectName(amount + " cr");
	}
}
