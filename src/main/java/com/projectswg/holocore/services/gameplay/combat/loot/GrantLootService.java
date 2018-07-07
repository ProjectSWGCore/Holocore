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
package com.projectswg.holocore.services.gameplay.combat.loot;

import com.projectswg.common.data.encodables.oob.ProsePackage;
import com.projectswg.common.data.encodables.oob.StringId;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.network.packets.swg.zone.ClientOpenContainerMessage;
import com.projectswg.common.network.packets.swg.zone.PlayMusicMessage;
import com.projectswg.common.network.packets.swg.zone.StopClientEffectObjectByLabelMessage;
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupCloseLotteryWindow;
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupOpenLotteryWindow;
import com.projectswg.common.network.packets.swg.zone.object_controller.loot.GroupRequestLotteryItems;
import com.projectswg.holocore.intents.gameplay.combat.CreatureKilledIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.CorpseLootedIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootItemIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootLotteryStartedIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent;
import com.projectswg.holocore.intents.gameplay.combat.loot.LootRequestIntent.LootType;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.data.config.ConfigFile;
import com.projectswg.holocore.resources.support.data.server_info.DataManager;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.permissions.ContainerPermissionsType;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject;
import com.projectswg.holocore.resources.support.objects.swg.group.GroupObject;
import com.projectswg.holocore.resources.support.objects.swg.group.LootRule;
import com.projectswg.holocore.resources.support.objects.swg.tangible.CreditObject;
import com.projectswg.holocore.services.support.objects.ObjectStorageService.ObjectLookup;
import me.joshlarson.jlcommon.concurrency.ScheduledThreadPool;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.utilities.Arguments;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public final class GrantLootService extends Service {
	
	private final Map<CreatureObject, CorpseLootRestrictions> lootRestrictions;
	private final ScheduledThreadPool executor;
	private final int lootRange;
	
	public GrantLootService() {
		this.lootRestrictions = new ConcurrentHashMap<>();
		this.executor = new ScheduledThreadPool(1, "grant-loot-service");
		this.lootRange = DataManager.getConfig(ConfigFile.LOOTOPTIONS).getInt("LOOT-RANGE", 64);
	}
	
	@Override
	public boolean start() {
		executor.start();
		return true;
	}
	
	@Override
	public boolean stop() {
		executor.stop();
		return executor.awaitTermination(1000);
	}
	
	@IntentHandler
	private void handleCreatureKilledIntent(CreatureKilledIntent cki) {
		CreatureObject corpse = cki.getCorpse();
		if (corpse.isPlayer())
			return;
		
		Location corpseWorldLocation = corpse.getWorldLocation();
		CreatureObject highestDamageDealer = corpse.getHighestDamageDealer();
		if (highestDamageDealer == null || !highestDamageDealer.isPlayer())
			return;
		long groupId = highestDamageDealer.getGroupId();
		
		List<CreatureObject> looters = new ArrayList<>();
		if (groupId != 0) {
			GroupObject group = (GroupObject) ObjectLookup.getObjectById(groupId);
			assert group != null;
			switch (group.getLootRule()) {
				case MASTER_LOOTER:
					looters.add((CreatureObject) ObjectLookup.getObjectById(group.getLootMaster()));
					break;
				case FREE_FOR_ALL:
				case RANDOM:
				case LOTTERY:
					// Get all looters within range
					looters.addAll(group.getGroupMemberObjects().stream().filter(m -> m.getWorldLocation().flatDistanceTo(corpseWorldLocation) <= lootRange).collect(Collectors.toList()));
					break;
			}
			if (group.getLootRule() == LootRule.LOTTERY) {
				lootRestrictions.put(corpse, new LotteryLootRestrictions(corpse, looters));
				return;
			} else if (group.getLootRule() == LootRule.RANDOM) {
				lootRestrictions.put(corpse, new RandomLootRestrictions(corpse, looters));
				return;
			}
		} else {
			looters.add(highestDamageDealer);
		}
		lootRestrictions.put(corpse, new StandardLootRestrictions(corpse, looters));
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		// For when the corpse is gone
		SWGObject object = doi.getObject();
		if (object instanceof CreatureObject) {
			CorpseLootRestrictions restrictions = lootRestrictions.remove(object);
			if (restrictions != null)
				restrictions.setValid(false);
		}
	}
	
	@IntentHandler
	private void handleInboundPacketIntent(InboundPacketIntent ipi) {
		if (!(ipi.getPacket() instanceof GroupRequestLotteryItems))
			return;
		Player player = ipi.getPlayer();
		GroupRequestLotteryItems request = (GroupRequestLotteryItems) ipi.getPacket();
		SWGObject requestObject = ObjectLookup.getObjectById(request.getInventoryId());
		if (requestObject == null)
			return;
		requestObject = requestObject.getParent();
		if (!(requestObject instanceof CreatureObject))
			return;
		CorpseLootRestrictions restriction = lootRestrictions.get(requestObject);
		if (!(restriction instanceof LotteryLootRestrictions))
			return;
		((LotteryLootRestrictions) restriction).updatePreferences(player.getCreatureObject(), request.getRequestedItems().stream().map(ObjectLookup::getObjectById).filter(Objects::nonNull).collect(Collectors.toList()));
	}
	
	@IntentHandler
	private void handleContainerTransfer(ContainerTransferIntent cti) {
		// Only check transfers into players
		SWGObject container = cti.getContainer();
		if (container == null || container.getOwner() == null)
			return;
		
		// If this is a looted item, reset the container permissions to default
		SWGObject object = cti.getObject();
		if (object.getContainerPermissions() == ContainerPermissionsType.LOOT)
			object.setContainerPermissions(ContainerPermissionsType.DEFAULT);
	}
	
	@IntentHandler
	private void handleLootRequestIntent(LootRequestIntent lri) {
		Player player = lri.getPlayer();
		CreatureObject looter = player.getCreatureObject();
		CreatureObject corpse = lri.getTarget();
		
		Arguments.validate(corpse instanceof AIObject, "Attempted to loot a non-AI object");
		CorpseLootRestrictions restrictions = lootRestrictions.get(corpse);
		if (restrictions == null || !restrictions.canLoot(looter)) {
			SystemMessageIntent.broadcastPersonal(player, "You don't have permission to loot '"+corpse.getObjectName()+ '\'');
			return;
		}
		
		if (restrictions instanceof LotteryLootRestrictions) {
			if (((LotteryLootRestrictions) restrictions).isStarted())
				return; // Don't keep re-starting the lottery
			executor.execute(30000, () -> {
				// Close the windows after 30 seconds, and grant the items won
				if (!restrictions.isValid())
					return;
				((LotteryLootRestrictions) restrictions).commitLottery();
			});
		}
		restrictions.handle(looter, lri.getType());
	}
	
	@IntentHandler
	private void handleLootItemIntent(LootItemIntent lii) {
		CreatureObject looter = lii.getLooter();
		CreatureObject corpse = lii.getCorpse();
		SWGObject item = lii.getItem();
		
		Arguments.validate(corpse instanceof AIObject, "Attempted to loot a non-AI object");
		CorpseLootRestrictions restrictions = lootRestrictions.get(corpse);
		if (restrictions == null || !restrictions.canLoot(looter)) {
			SystemMessageIntent.broadcastPersonal(looter.getOwner(), "You don't have permission to loot '"+corpse.getObjectName()+ '\'');
			return;
		}
		
		restrictions.loot(looter, item);
	}
	
	private static abstract class CorpseLootRestrictions {
		
		private final CreatureObject corpse;
		private final List<CreatureObject> looters;
		private final AtomicBoolean valid;
		
		public CorpseLootRestrictions(CreatureObject corpse, List<CreatureObject> looters) {
			this.corpse = corpse;
			this.looters = looters;
			this.valid = new AtomicBoolean(true);
		}
		
		public boolean isValid() {
			return valid.get();
		}
		
		public CreatureObject getCorpse() {
			return corpse;
		}
		
		public List<CreatureObject> getLooters() {
			return Collections.unmodifiableList(looters);
		}
		
		public boolean canLoot(CreatureObject looter) {
			return looters.contains(looter);
		}
		
		public void setValid(boolean valid) {
			this.valid.set(valid);
		}
		
		public synchronized void loot(CreatureObject looter, SWGObject item) {
			throw new UnsupportedOperationException("this loot restriction cannot do manual looting");
		}
		
		public abstract void handle(CreatureObject looter, LootType type);
		
		protected synchronized void transferItem(CreatureObject looter, SWGObject item) {
			if (!getCorpse().getInventory().getContainedObjects().contains(item) || !canLoot(looter)) {
				return;
			}
			Player player = looter.getOwner();
			
			switch (item.moveToContainer(looter, looter.getSlottedObject("inventory"))) {
				case SUCCESS: {
					String itemName = item.getObjectName();
					
					if (item instanceof CreditObject) {
						onLootedCredits(looter, ((CreditObject) item).getAmount());
					} else {
						new SystemMessageIntent(player, new ProsePackage("StringId", new StringId("loot_n", "solo_looted"), "TO", itemName)).broadcast();
					}
					onLooted(looter, getCorpse());
					break;
				}
				case CONTAINER_FULL:
					new SystemMessageIntent(player, "@container_error_message:container03").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_danger_message.snd", 1, false));
					break;
				case NO_PERMISSION:
					new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
				case SLOT_NO_EXIST:
					new SystemMessageIntent(player, "@container_error_message:container06").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
				case SLOT_OCCUPIED:
					new SystemMessageIntent(player, "@container_error_message:container08").broadcast();
					player.sendPacket(new PlayMusicMessage(0, "sound/ui_negative.snd", 1, false));
					break;
			}
		}
		
		protected void splitCredits(CreditObject target, Collection<CreatureObject> looters) {
			long amount = target.getAmount() / looters.size();
			assert amount > 0;
			
			for (CreatureObject looter : looters) {
				looter.addToBank(amount);
				onLootedCredits(looter, amount);
			}
			
			DestroyObjectIntent.broadcast(target);
		}
		
		protected void onLooted(CreatureObject looter, CreatureObject corpse) {
			SWGObject lootInventory = corpse.getInventory();
			if (lootInventory.getContainedObjects().isEmpty()) {
				new CorpseLootedIntent(corpse).broadcast();
				
				long looterGroupId = looter.getGroupId();
				if (looterGroupId != 0) {
					GroupObject killerGroup = (GroupObject) ObjectLookup.getObjectById(looterGroupId);
					assert killerGroup != null;
					for (CreatureObject groupMember : killerGroup.getGroupMemberObjects())
						groupMember.getOwner().sendPacket(new StopClientEffectObjectByLabelMessage(corpse.getObjectId(), "lootMe", false));
				} else {
					looter.getOwner().sendPacket(new StopClientEffectObjectByLabelMessage(corpse.getObjectId(), "lootMe", false));
				}
			}
		}
		
		protected void onLootedCredits(CreatureObject looter, long amount) {
			// Perhaps "prose_coin_loot_no_target" is the proper string?
			new SystemMessageIntent(looter.getOwner(), new ProsePackage("StringId", new StringId("base_player", "prose_transfer_success"), "DI", (int) amount)).broadcast();
		}
		
	}
	
	private static class StandardLootRestrictions extends CorpseLootRestrictions {
		
		public StandardLootRestrictions(CreatureObject corpse, List<CreatureObject> looters) {
			super(corpse, looters);
		}
		
		@Override
		public synchronized void loot(CreatureObject looter, SWGObject item) {
			transferItem(looter, item);
		}
		
		@Override
		public synchronized void handle(CreatureObject looter, LootType type) {
			SWGObject lootInventory = getCorpse().getInventory();
			Collection<SWGObject> lootItems = lootInventory.getContainedObjects();
			switch (type) {
				case LOOT: // Open loot box
					if (!lootItems.isEmpty())
						looter.getOwner().sendPacket(new ClientOpenContainerMessage(lootInventory.getObjectId(), ""));
					break;
				case LOOT_ALL: // Request to loot all items
					for (SWGObject loot : lootItems)
						LootItemIntent.broadcast(looter, getCorpse(), loot);
					break;
			}
		}
	}
	
	private static class RandomLootRestrictions extends CorpseLootRestrictions {
		
		public RandomLootRestrictions(CreatureObject corpse, List<CreatureObject> looters) {
			super(corpse, looters);
		}
		
		@Override
		public synchronized void loot(CreatureObject looter, SWGObject item) {
			if (item instanceof CreditObject && ((CreditObject) item).getAmount() >= getLooters().size())
				splitCredits((CreditObject) item, getLooters());
			else
				transferItem(looter, item);
		}
		
		@Override
		public synchronized void handle(CreatureObject looter, LootType type) {
			Collection<SWGObject> lootItems = getCorpse().getInventory().getContainedObjects();
			for (SWGObject loot : lootItems)
				LootItemIntent.broadcast(looter, getCorpse(), loot);
		}
		
	}
	
	private static class LotteryLootRestrictions extends CorpseLootRestrictions {
		
		private final Map<CreatureObject, List<SWGObject>> preferences;
		private final AtomicBoolean started;
		private final AtomicBoolean committed;
		
		public LotteryLootRestrictions(CreatureObject corpse, List<CreatureObject> looters) {
			super(corpse, looters);
			this.preferences = new HashMap<>();
			this.started = new AtomicBoolean(false);
			this.committed = new AtomicBoolean(false);
		}
		
		public boolean isStarted() {
			return started.get();
		}
		
		@Override
		public synchronized void handle(CreatureObject looter, LootType type) {
			if (started.getAndSet(true))
				return;
			if (getCorpse().getInventory().getContainedObjects().isEmpty())
				return;
			LootLotteryStartedIntent.broadcast(getCorpse());
			for (CreatureObject creature : getLooters()) {
				Player player = creature.getOwner();
				if (player != null)
					player.sendPacket(new GroupOpenLotteryWindow(creature.getObjectId(), getCorpse().getInventory().getObjectId()));
			}
		}
		
		public synchronized void updatePreferences(CreatureObject looter, List<SWGObject> objects) {
			preferences.put(looter, objects);
			if (preferences.size() == getLooters().size())
				commitLottery();
		}
		
		public synchronized void commitLottery() {
			if (committed.getAndSet(true))
				return;
			// Build item map
			Map<SWGObject, List<CreatureObject>> itemPreferences = new HashMap<>();
			for (Entry<CreatureObject, List<SWGObject>> preference : preferences.entrySet()) {
				for (SWGObject item : preference.getValue())
					itemPreferences.computeIfAbsent(item, i -> new ArrayList<>()).add(preference.getKey());
			}
			
			// Notify the lottery is done
			for (CreatureObject creature : getLooters()) {
				Player player = creature.getOwner();
				if (player != null)
					player.sendPacket(new GroupCloseLotteryWindow(creature.getObjectId(), getCorpse().getInventory().getObjectId()));
			}
			
			// Give out items
			Random random = new Random();
			for (Entry<SWGObject, List<CreatureObject>> itemEntry : itemPreferences.entrySet()) {
				List<CreatureObject> looters = itemEntry.getValue();
				assert !looters.isEmpty();
				if (itemEntry.getKey() instanceof CreditObject && ((CreditObject) itemEntry.getKey()).getAmount() >= looters.size()) {
					splitCredits((CreditObject) itemEntry.getKey(), itemEntry.getValue());
				} else {
					CreatureObject winner = looters.get(random.nextInt(looters.size()));
					transferItem(winner, itemEntry.getKey());
				}
			}
		}
		
	}
	
}
