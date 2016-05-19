package services.faction;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import network.packets.swg.zone.UpdatePvpStatusMessage;
import network.packets.swg.zone.chat.ChatSystemMessage;
import network.packets.swg.zone.chat.ChatSystemMessage.SystemChatType;
import intents.FactionIntent;
import resources.PvpFaction;
import resources.PvpFlag;
import resources.PvpStatus;
import resources.control.Intent;
import resources.control.Service;
import resources.objects.SWGObject;
import resources.objects.creature.CreatureObject;
import resources.objects.tangible.TangibleObject;
import resources.player.Player;

public final class FactionService extends Service {

	private ScheduledExecutorService executor;
	
	public FactionService() {
		registerForIntent(FactionIntent.TYPE);
	}
	
	@Override
	public boolean initialize() {
		executor = Executors.newSingleThreadScheduledExecutor();
		return super.initialize();
	}
	
	@Override
	public void onIntentReceived(Intent i) {
		if(i instanceof FactionIntent) {
			FactionIntent fi = (FactionIntent) i;
			
			switch(fi.getUpdateType()) {
				case FACTIONUPDATE:
					handleTypeChange(fi);
					break;
				case STATUSUPDATE:
					handleStatusChange(fi);
					break;
				case FLAGUPDATE:
					handleFlagChange(fi.getTarget());
					break;
			}
		}
	}
	
	@Override
	public boolean terminate() {
		boolean success = true;
		try {
			if (executor != null) {
				executor.shutdownNow();
				success = executor.awaitTermination(5, TimeUnit.SECONDS);
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			success = false;
		}
		return super.terminate() && success;
	}
	
	private void sendSystemMessage(TangibleObject target, String message) {
		target.getOwner().sendPacket(new ChatSystemMessage(SystemChatType.SCREEN_AND_CHAT, message));
	}
	
	private String getBeginMessage(PvpStatus currentStatus, PvpStatus targetStatus) {
		String message = "@faction_recruiter:";
		
		if(currentStatus == PvpStatus.ONLEAVE && targetStatus == PvpStatus.COMBATANT)
			message += "on_leave_to_covert";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			message += "covert_to_overt";
		else if(currentStatus == PvpStatus.SPECIALFORCES && targetStatus == PvpStatus.COMBATANT)
			message += "overt_to_covert";
		
		return message;
	}
	
	private String getCompletionMessage(PvpStatus currentStatus, PvpStatus targetStatus) {
		String message = "@faction_recruiter:";
		
		if((currentStatus == PvpStatus.ONLEAVE || currentStatus == PvpStatus.SPECIALFORCES) && targetStatus == PvpStatus.COMBATANT)
			message += "covert_complete";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			message += "overt_complete";
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.ONLEAVE )
			message += "on_leave_complete";
			
		return message;
	}
	
	private long getDelay(PvpStatus currentStatus, PvpStatus targetStatus) {
		long delay = 0;
		
		if(currentStatus == PvpStatus.ONLEAVE && targetStatus == PvpStatus.COMBATANT)
			delay = 1;
		else if(currentStatus == PvpStatus.COMBATANT && targetStatus == PvpStatus.SPECIALFORCES)
			delay = 30;
		else if(currentStatus == PvpStatus.SPECIALFORCES && targetStatus == PvpStatus.COMBATANT)
			delay = 300;
		
		return delay;
	}
	
	private void handleTypeChange(FactionIntent fi) {
		TangibleObject target = fi.getTarget();
		PvpFaction newFaction = fi.getNewFaction();
		
		target.setPvpFaction(newFaction);
		target.setPvpStatus(PvpStatus.COMBATANT);
		
		handleFlagChange(target);		// We don't want to remain attackable if we leave our current faction.
		
		if(target instanceof CreatureObject) {
			int rank;
			
			if(newFaction != PvpFaction.NEUTRAL)
				rank = 1;		// We're given rank 1 upon joining a PvP faction
			else
				rank = 0;		// Neutrals have no ranks
			
			((CreatureObject) target).setFactionRank((byte) rank);
		}
	}
	
	private void handleStatusChange(FactionIntent fi) {
		final PvpFlag pvpFlag;
		final TangibleObject target = fi.getTarget();
		final PvpStatus currentStatus = target.getPvpStatus();
		final PvpStatus targetStatus;
		
		if(target.hasPvpFlag(PvpFlag.GOING_COVERT) || target.hasPvpFlag(PvpFlag.GOING_OVERT)) {
			sendSystemMessage(target, "@faction_recruiter:pvp_status_changing");
		} else {
			if(currentStatus == PvpStatus.COMBATANT) {
				pvpFlag = PvpFlag.GOING_OVERT;
				targetStatus = PvpStatus.SPECIALFORCES;
			} else {	// Covers both ONLEAVE and SPECIALFORCES
				pvpFlag = PvpFlag.GOING_COVERT;
				targetStatus = PvpStatus.COMBATANT;
			}
			
			target.setPvpFlags(pvpFlag);
			sendSystemMessage(target, getBeginMessage(currentStatus, targetStatus));
			executor.schedule(new Runnable() {
				@Override
				public void run() {	
					target.setPvpStatus(targetStatus);
					target.getOwner().sendPacket(new ChatSystemMessage(
							SystemChatType.SCREEN_AND_CHAT,
							getCompletionMessage(currentStatus, targetStatus)));
					target.clearPvpFlags(pvpFlag);
				}
			}, getDelay(currentStatus, targetStatus), TimeUnit.SECONDS);
		}
	}
	
	private void handleFlagChange(TangibleObject object) {
//		Player objOwner = object.getOwner();
//		for (SWGObject o : object.getObservers()) {
//			if (!(o instanceof TangibleObject))
//				continue;
//			TangibleObject observer = (TangibleObject) o;
//			Player obsOwner = observer.getOwner();
//			int pvpBitmask = 0;
//			
//			// They CAN be enemies if they're not from the same faction and neither of them are neutral
//			if (object.getPvpFaction() != observer.getPvpFaction() && observer.getPvpFaction() != PvpFaction.NEUTRAL) {
//				if (object.getPvpStatus() == PvpStatus.SPECIALFORCES && observer.getPvpStatus() == PvpStatus.SPECIALFORCES) {
//					pvpBitmask |= PvpFlag.AGGRESSIVE.getBitmask() | PvpFlag.ATTACKABLE.getBitmask();
//				}
//			}
//			UpdatePvpStatusMessage objectPacket = createPvpStatusMessage(object, observer, object.getPvpFlags() | pvpBitmask);
//			UpdatePvpStatusMessage targetPacket = createPvpStatusMessage(object, observer, observer.getPvpFlags() | pvpBitmask);
//			if (objOwner != null)
//				objOwner.sendPacket(objectPacket, targetPacket);
//			if (obsOwner != null)
//				obsOwner.sendPacket(objectPacket);
//		}
	}
	
//	private UpdatePvpStatusMessage createPvpStatusMessage(TangibleObject object, TangibleObject observer, int flags) {
//		Set<PvpFlag> flagSet = PvpFlag.getFlags(object.getPvpFlags());
//		return new UpdatePvpStatusMessage(object.getPvpFaction(), object.getObjectId(), flagSet.toArray(new PvpFlag[flagSet.size()]));
//	}
	
}