package resources.objects.tangible;

import java.util.Set;

import com.projectswg.common.debug.Assert;
import com.projectswg.common.debug.Log;
import com.projectswg.common.network.packets.swg.zone.UpdateContainmentMessage;

import intents.object.ContainerTransferIntent;
import resources.containers.ContainerResult;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.player.Player;
import utilities.AwarenessUtilities;

public class CreditObject extends TangibleObject
{
	private long amount;

	public CreditObject(long objectId)
	{
		super(objectId);
	}
	
	/**
	 * Moves this object to the passed container if the requester has the MOVE permission for the container
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return {@link ContainerResult}
	 */
	@Override
	public ContainerResult moveToContainer(SWGObject requester, SWGObject container) {
		if (!(requester instanceof CreatureObject && ((CreatureObject) requester).isPlayer()))
			return super.moveToContainer(requester, container);
		
		Assert.test(amount > 0, "Amount must be set!");
		
		if (parent == container) // One could be null, and this is specifically an instance-based check
			return ContainerResult.SUCCESS;
		
		ContainerResult result = moveToAccountChecks(requester);
		if (result != ContainerResult.SUCCESS)
			return result;
		
		Set<Player> oldObservers = getObserversAndParent();
		removeFromParent();
		
		((CreatureObject) requester).addToCash(amount);
		
		Set<Player> newObservers = getObserversAndParent();
		
		UpdateContainmentMessage update = new UpdateContainmentMessage(getObjectId(), 0, getSlotArrangement());
		AwarenessUtilities.callForSameObserver(oldObservers, newObservers, (observer) -> observer.sendPacket(update));
		AwarenessUtilities.callForOldObserver(oldObservers, newObservers, (observer) -> destroyObject(observer));
		
		new ContainerTransferIntent(this, parent, null).broadcast();
		
		return ContainerResult.SUCCESS;
	}
	
	/**
	 * Checks if an object can be moved to the container by the requester
	 * @param requester Object that is requesting to move the object, used for permission checking
	 * @param container Where this object should be moved to
	 * @return
	 */
	protected ContainerResult moveToAccountChecks(SWGObject requester) {
		if (requester == null)
			return ContainerResult.SUCCESS;
		
		if (!permissions.canMove(requester, this)) {
			Log.w("No permission 'MOVE' for requestor %s with object %s", requester, this);
			return ContainerResult.NO_PERMISSION;
		}
		
		return ContainerResult.SUCCESS;
	}
	
	public long getAmount() {
		return amount;
	}
	
	public void setAmount(long amount) {
		Assert.test(amount > 0, "Amount must be greater than 0!");
		this.amount = amount;
		setObjectName(amount + " cr");
	}
}
