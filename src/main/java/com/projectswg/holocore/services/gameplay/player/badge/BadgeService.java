package com.projectswg.holocore.services.gameplay.player.badge;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.holocore.intents.gameplay.player.badge.GrantBadgeIntent;
import com.projectswg.holocore.intents.gameplay.player.badge.SetTitleIntent;
import com.projectswg.holocore.intents.gameplay.player.collections.GrantClickyCollectionIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.gameplay.player.collections.ClickyCollectionItem;
import com.projectswg.holocore.resources.gameplay.player.collections.CollectionItem;
import com.projectswg.holocore.resources.support.data.server_info.loader.CollectionLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.CollectionLoader.CollectionSlotInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.player.PlayerObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;

public class BadgeService extends Service {
	
	// TODO grant item rewards and/or schematics
	// TODO grant XP
	// TODO clearon complete (repeatable)
	// TODO track server first
	// TODO research categories
	// TODO fix to appropriate message ex: kill_merek_activation_01
	
	public BadgeService() {
		
	}
	
	@IntentHandler
	private void handleGrantClickyCollectionIntent(GrantClickyCollectionIntent gcci) {
		CreatureObject creature = gcci.getCreature();
		PlayerObject player = creature.getPlayerObject();
		SWGObject inventoryItem = gcci.getInventoryItem();
		CollectionItem collection = gcci.getCollection();
		
		CollectionSlotInfo slotInfo = DataLoader.collections().getSlotByName(collection.getSlotName());
		if (slotInfo == null)
			return;
		
		grantBadge(player, slotInfo);
		
		if (!(collection instanceof ClickyCollectionItem))
			DestroyObjectIntent.broadcast(inventoryItem);
	}
	
	@IntentHandler
	private void handleGrantBadgeIntent(GrantBadgeIntent gbi) {
		CreatureObject creature = gbi.getCreature();
		PlayerObject player = creature.getPlayerObject();
		String slotName = gbi.getCollectionBadgeName();
		
		CollectionLoader collectionLoader = DataLoader.collections();
		// Grant the requested badge if allowed (checks within grantBadge)
		CollectionSlotInfo slot = collectionLoader.getSlotByName(slotName);
		if (slot == null)
			return;
		grantBadge(player, slot);
	}
	
	@IntentHandler
	private void handleSetTitleIntent(SetTitleIntent sti) {
		String title = sti.getTitle();
		PlayerObject requester = sti.getRequester();
		
		CollectionSlotInfo slot = DataLoader.collections().getSlotByName(title);
		if (slot == null || !hasCompletedCollection(requester, slot)) {
			// You can't be assigned a title for a collection you haven't completed/doesn't exist
			return;
		}
		
		requester.setTitle(title);
	}
	
	private void grantAccumulationBadges(PlayerObject player) {
		// Update the aggregation badges
		CollectionLoader collections = DataLoader.collections();
		List<CollectionSlotInfo> grantedSlots = player.getCollectionBadgeIds().stream().map(collections::getSlotByBeginSlot).collect(Collectors.toList());
		
		int badges = countSlots(grantedSlots, null);
		int expBadges = countSlots(grantedSlots, "bdg_explore");
		
		// Generic accumulation
		if (badges >= 5)
			grantBadge(player, collections.getSlotByName("count_5"));
		if (badges >= 10)
			grantBadge(player, collections.getSlotByName("count_10"));
		for (int i = 25; i <= 500 && i < badges; i+=25)
			grantBadge(player, collections.getSlotByName("count_"+i));
		
		// Exploration
		for (int i = 10; i <= 40 && i < expBadges; i += 10)
			grantBadge(player, collections.getSlotByName("count_"+i));
		if (expBadges >= 45)
			grantBadge(player, collections.getSlotByName("count_45"));
	}
	
	private int countSlots(List<CollectionSlotInfo> grantedSlots, String pageName) {
		int count = 0;
		for (CollectionSlotInfo slot  : grantedSlots) {
			if (!slot.getBookName().equals("badge_book") || (pageName != null && !slot.getPageName().equals(pageName)))
				continue;
			count++;
		}
		return count;
	}
	
	private void grantBadge(@NotNull PlayerObject player, @Nullable CollectionSlotInfo slotInfo) {
		if (slotInfo == null || !hasPreReqComplete(player, slotInfo))
			return;
		
		if (slotInfo.getEndSlotId() == -1) {
			grantBadgeIndividual(player, slotInfo);
		} else {
			grantBadgeSlotted(player, slotInfo);
		}
		
		if (slotInfo.getBookName().equals("badge_book"))
			grantAccumulationBadges(player);
	}
	
	private void grantBadgeIndividual(@NotNull PlayerObject player, @NotNull CollectionSlotInfo slotInfo) {
		if (player.getCollectionFlag(slotInfo.getBeginSlotId()))
			return;
		
		player.setCollectionFlag(slotInfo.getBeginSlotId());
		sendCollectionGrantedMessage(player, slotInfo);
	}
	
	private void grantBadgeSlotted(@NotNull PlayerObject player, @NotNull CollectionSlotInfo slotInfo) {
		BitSet collections = player.getCollectionBadges();
		BitSet flippedBits = new BitSet();
		
		long slotValue = 0;
		for (int i = slotInfo.getBeginSlotId(); i < slotInfo.getEndSlotId(); i++) {
			flippedBits.set(i);
			slotValue |= 1 << (i - slotInfo.getBeginSlotId());
			if (!collections.get(i))
				break;
		}
		
		if (slotValue >= slotInfo.getMaxSlotValue())
			return;
		
		player.toggleCollectionFlags(flippedBits);
	}
	
	private boolean hasPreReqComplete(PlayerObject player, CollectionSlotInfo slot) {
		CollectionLoader collections = DataLoader.collections();
		
		for (String prereq : slot.getPrereqSlotNames()) {
			if (prereq == null || prereq.isEmpty())
				continue; // No prerequisite to check for
			
			CollectionSlotInfo prereqSlot = collections.getSlotByName(prereq);
			if (prereqSlot == null)
				continue; // Invalid prerequisite
			
			if (!player.getCollectionFlag(prereqSlot.getBeginSlotId()))
				return false;
		}
		return true;
	}
	
	private void sendCollectionGrantedMessage(PlayerObject player, CollectionSlotInfo slot) {
		Player owner = player.getOwner();
		if (owner == null)
			return;
		
		if (slot.isHidden())
			SystemMessageIntent.broadcastPersonal(owner, new ProsePackage(new StringId("@collection:player_hidden_slot_added"), "TO", "@collection_n:" + slot.getCollectionName()));
		else
			SystemMessageIntent.broadcastPersonal(owner, new ProsePackage(new StringId("@collection:player_slot_added"), "TU", "@collection_n:" + slot.getSlotName(), "TO", "@collection_n:" + slot.getCollectionName()));
		
		if (slot.getMusic().isEmpty())
			owner.sendPacket(new PlayMusicMessage(0, "sound/utinni.snd", 1, false));
		else
			owner.sendPacket(new PlayMusicMessage(0, slot.getMusic(), 1, false));
		
		if (hasCompletedCollection(player, slot))
			SystemMessageIntent.broadcastPersonal(owner, new ProsePackage(new StringId("@collection:player_collection_complete"), "TO", "@collection_n:" + slot.getCollectionName()));
	}
	
	private boolean hasCompletedCollection(PlayerObject player, CollectionSlotInfo completedSlot) {
		List<CollectionSlotInfo> slots = DataLoader.collections().getCollectionByName(completedSlot.getCollectionName());
		if (slots == null)
			return true;
		
		for (CollectionSlotInfo slot : slots) {
			if (!player.getCollectionFlag(slot.getBeginSlotId()))
				return false;
		}
		return true;
	}
	
}
