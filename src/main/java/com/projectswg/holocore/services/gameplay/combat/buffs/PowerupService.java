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
package com.projectswg.holocore.services.gameplay.combat.buffs;

import com.projectswg.common.data.objects.GameObjectType;
import com.projectswg.holocore.intents.gameplay.combat.buffs.BuffIntent;
import com.projectswg.holocore.intents.gameplay.combat.buffs.PowerupIntent;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.ContainerTransferIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.BuffLoader.BuffInfo;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons;
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import me.joshlarson.jlcommon.concurrency.BasicScheduledThread;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class PowerupService extends Service {
	
	private static final long CHECK_RATE = TimeUnit.MINUTES.toMillis(1);	// Reduce powerup duration every minute
	public static final String BREASTPLATE = "object/tangible/loot/generic_usable/shared_copper_battery_usuable.iff";
	public static final String SHIRT = "object/tangible/loot/generic_usable/shared_chassis_blueprint_usuable.iff";
	public static final String WEAPON = "object/tangible/loot/generic_usable/shared_scope_weapon_generic.iff";
	
	private final BasicScheduledThread timerCheckThread;
	private final Set<SWGObject> monitored;
	
	public PowerupService() {
		timerCheckThread = new BasicScheduledThread("powerup-timer-check", this::checkPoweredObjects);
		monitored = new HashSet<>();
	}
	
	@Override
	public boolean start() {
		timerCheckThread.startWithFixedRate(CHECK_RATE, CHECK_RATE);
		return super.start();
	}
	
	@Override
	public boolean stop() {
		timerCheckThread.stop();
		return super.stop();
	}
	
	@IntentHandler
	private void handlePowerupIntent(PowerupIntent intent) {
		CreatureObject actor = intent.getActor();
		TangibleObject powerupObject = intent.getPowerupObject();
		
		// Let's figure out where we should apply the power up buff
		String powerupTemplate = powerupObject.getTemplate();
		SWGObject applicableObject = applicableObject(powerupTemplate, actor);
		
		Player owner = actor.getOwner();
		
		if (owner == null) {
			return;
		}
		
		if (applicableObject == null) {
			SystemMessageIntent.broadcastPersonal(owner, "@spam:powerup_must_equip_item");
			return;
		}
		
		if (isPoweredUp(applicableObject)) {
			// This object already has a powerup applied to it. Ask the player if they're sure they want to replace it.
			SuiMessageBox message = new SuiMessageBox(
					SuiButtons.YES_NO,
					"@spam:powerup_override_title",
					"@spam:powerup_override",
					"callback",
					(event, parameters) -> {
						switch (event) {
							case OK_PRESSED:
								removePowerup(applicableObject);
								applyPowerup(actor, applicableObject, powerupObject);
								break;
						}
					});
			
			message.display(owner);
		} else {
			// No existing powerup is applied to this object
			applyPowerup(actor, applicableObject, powerupObject);
		}
	}
	
	@IntentHandler
	private void handleContainerTransferIntent(ContainerTransferIntent intent) {
		SWGObject object = intent.getObject();
		SWGObject newContainer = intent.getContainer();
		
		if (!monitored.contains(object)) {
			// Object does not have a powerup applied to it. Do nothing.
			return;
		}
		
		if (newContainer == null) {
			return;
		}
		
		// Add the relevant powerup buff if they're equipping the item to their character and remove it if they're unequipping
		boolean removeBuff = newContainer.getGameObjectType() != GameObjectType.GOT_CREATURE_CHARACTER;
		Player owner = object.getOwner();
		
		if (owner == null) {
			return;
		}
		
		// Apply or remove buff
		buff(object, owner, removeBuff);
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent intent) {
		switch (intent.getEvent()) {
			case PE_DISAPPEAR: {
				// Player character disappeared. Stop deducting time from their powerups applied to items.
				Player player = intent.getPlayer();
				CreatureObject creatureObject = player.getCreatureObject();
				Collection<SWGObject> childObjectsRecursively = creatureObject.getChildObjectsRecursively();
				
				for (SWGObject swgObject : childObjectsRecursively) {
					if (isPoweredUp(swgObject)) {
						monitored.remove(swgObject);
					}
				}
			}
			break;
			case PE_FIRST_ZONE: {
				// Player character reappeared. Start deducting time from their powerups applied to items.
				Player player = intent.getPlayer();
				CreatureObject creatureObject = player.getCreatureObject();
				Collection<SWGObject> childObjectsRecursively = creatureObject.getChildObjectsRecursively();
				
				for (SWGObject swgObject : childObjectsRecursively) {
					if (isPoweredUp(swgObject)) {
						monitored.add(swgObject);
					}
				}
				
			}
			break;
		}
	}
	
	private boolean isPoweredUp(SWGObject object) {
		return object.hasAttribute("@spam:pup_expire_time");
	}
	
	private void checkPoweredObjects() {
		List<SWGObject> monitoredCopy;
		synchronized (this.monitored) {
			monitoredCopy = new ArrayList<>(monitored);
		}
		for (SWGObject poweredObject : monitoredCopy) {
			String timeRemaining = poweredObject.getAttribute("@spam:pup_expire_time");
			int oldTime = Integer.parseInt(timeRemaining);
			int newTime = oldTime - 1;	// A minute has passed - deduct it
			
			if (newTime <= 0) {
				// Powerup expired
				removePowerup(poweredObject);
			} else {
				// Powerup still has time left - udpate the attribute
				poweredObject.addAttribute("@spam:pup_expire_time", Integer.toString(newTime));
			}
		}
	}
	
	private SWGObject applicableObject(String powerupTemplate, CreatureObject actor) {
		switch (powerupTemplate) {
			case BREASTPLATE: return actor.getSlottedObject("chest2");
			case SHIRT: return actor.getSlottedObject("chest1");
			case WEAPON: return actor.getSlottedObject("hold_r");
			default: return null;
		}
	}
	
	private void applyPowerup(CreatureObject actor, SWGObject applicableObject, TangibleObject powerupObject) {
		// Apply the powerup attribute to the piece of equipment
		Map<String, String> powerupAttributes = powerupObject.getAttributes();
		
		for (Map.Entry<String, String> entry : powerupAttributes.entrySet()) {
			String modifier = entry.getKey();
			String value = entry.getValue();
			
			applicableObject.addAttribute("@spam:pup_modifier", modifier);
			applicableObject.addAttribute("@spam:pup_power", value);
		}
		
		applicableObject.addAttribute("@spam:pup_expire_time", "30");	// Powerups last 30 minutes
		
		// Consume a single use of the powerup
		consume(powerupObject);
		
		// Buff the player
		buff(applicableObject, actor.getOwner(), false);
		
		// Watch the timer on the powered up object
		this.monitored.add(applicableObject);
	}
	
	private void removePowerup(@Nullable SWGObject object) {
		if (object == null) {
			return;
		}
		
		if (!monitored.remove(object)) {
			Log.w("Attempted to remove powerup from an unmonitored object: %s", object);
		}
		
		object.removeAttribute("@spam:pup_modifier");
		object.removeAttribute("@spam:pup_power");
		object.removeAttribute("@spam:pup_expire_time");
		
		Player owner = object.getOwner();
		
		if (owner == null) {
			return;
		}
		
		buff(object, owner, true);	// Remove the buff
	}
	
	private void buff(SWGObject object, Player owner, boolean remove) {
		// Figure out which buff to add or remove
		GameObjectType gameObjectType = object.getGameObjectType();
		String powerupBuff = buffName(gameObjectType);
		
		if (powerupBuff == null)
			return;
		
		BuffInfo buffOriginal = DataLoader.Companion.buffs().getBuff(powerupBuff);
		if (buffOriginal == null)
			return;
		
		String modifier = object.getAttribute("@spam:pup_modifier").replace("@stat_n:", "");
		int value = Integer.parseInt(object.getAttribute("@spam:pup_power"));
		int timeMinutes = Integer.parseInt(object.getAttribute("@spam:pup_expire_time"));
		int timeSeconds = (int) TimeUnit.MINUTES.toSeconds(timeMinutes);
		
		BuffInfo buff = buffOriginal.builder()
				.setDuration(timeSeconds)
				.addEffect(modifier, value)
				.build();
		
		CreatureObject creatureObject = owner.getCreatureObject();
		BuffIntent.broadcast(buff, creatureObject, creatureObject, remove);
	}
	
	private String buffName(GameObjectType type) {
		if (type.name().startsWith("GOT_WEAPON")) {
			return "powerup_weapon";
		}
		
		switch (type) {
			case GOT_CLOTHING_SHIRT: return "powerup_shirt";
			case GOT_CLOTHING_VEST:
			case GOT_CLOTHING_JACKET:
			case GOT_CLOTHING_CLOAK:
			case GOT_CLOTHING_DRESS:
			case GOT_CLOTHING_ROBE:
			case GOT_ARMOR_BODY: return "powerup_chest_armor";
			default: return null;
		}
	}
	
	private void consume(TangibleObject powerupObject) {
		int counter = powerupObject.getCounter();
		int newCounter = counter - 1;
		
		if (newCounter <= 0) {
			// No uses left - destroy
			DestroyObjectIntent.broadcast(powerupObject);
		} else {
			// More uses left, just decrement the counter
			powerupObject.setCounter(newCounter);
		}
	}
	
}
