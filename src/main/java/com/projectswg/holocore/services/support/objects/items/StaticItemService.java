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
package com.projectswg.holocore.services.support.objects.items;

import com.projectswg.common.network.packets.swg.zone.object_controller.ShowLootBox;
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent;
import com.projectswg.holocore.intents.support.objects.items.CreateStaticItemIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.data.server_info.loader.DataLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.StaticItemLoader;
import com.projectswg.holocore.resources.support.data.server_info.loader.StaticItemLoader.StaticItemInfo;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject;
import com.projectswg.holocore.resources.support.objects.swg.weapon.WeaponObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author mads
 */
public class StaticItemService extends Service {
	
	public StaticItemService() {
		
	}
	
	@IntentHandler
	private void handleCreateStaticItemIntent(CreateStaticItemIntent csii) {
		SWGObject container = csii.getContainer();
		String[] itemNames = csii.getItemNames();
		ObjectCreationHandler objectCreationHandler = csii.getObjectCreationHandler();
		
		// If adding these items to the container would exceed the max capacity...
		if (!objectCreationHandler.isIgnoreVolume() && container.getVolume() + itemNames.length > container.getMaxContainerSize()) {
			objectCreationHandler.containerFull();
			return;
		}
		
		List<SWGObject> objects = new ArrayList<>();
		for (String itemName : itemNames) {
			SWGObject object = createItem(itemName, container);
			if (object != null) {
				objects.add(object);
			} else {
				Log.d("%s could not be spawned because the item name is unknown", itemName);
				Player requesterOwner = csii.getRequester().getOwner();
				if (requesterOwner != null)
					SystemMessageIntent.broadcastPersonal(requesterOwner, String.format("%s could not be spawned because the item name is unknown", itemName));
			}
		}
		objectCreationHandler.success(Collections.unmodifiableList(objects));
	}
	
	private SWGObject createItem(String itemName, SWGObject container) {
		StaticItemInfo info = DataLoader.staticItems().getItemByName(itemName);
		if (info == null)
			return null;
		SWGObject swgObject = ObjectCreator.createObjectFromTemplate(info.getIffTemplate());
		if (!(swgObject instanceof TangibleObject))
			return null;
		
		TangibleObject object = (TangibleObject) swgObject; 
		applyAttributes(object, info);
		
		object.moveToContainer(container);
		Log.d("Successfully moved %s into container %s", itemName, container);
		
		ObjectCreatedIntent.broadcast(object);
		return object;
	}
	
	private void applyAttributes(TangibleObject object, StaticItemInfo info) {
		object.addAttribute("condition", String.format("%d/%d", info.getHitPoints(), info.getHitPoints()));
		object.addAttribute("volume", String.valueOf(info.getVolume()));
		object.setObjectName(info.getStringName());
		
		applyAttributes(object, info.getArmorInfo());
		applyAttributes(object, info.getWearableInfo());
		applyAttributes(object, info.getWeaponInfo());
		applyAttributes(object, info.getCollectionInfo());
		applyAttributes(object, info.getCostumeInfo());
		applyAttributes(object, info.getDnaInfo());
		applyAttributes(object, info.getGrantInfo());
		applyAttributes(object, info.getGenericInfo());
		applyAttributes(object, info.getObjectInfo());
		applyAttributes(object, info.getSchematicInfo());
		applyAttributes(object, info.getStorytellerInfo());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.ArmorItemInfo info) {
		if (info == null)
			return;
		
		object.addAttribute("required_combat_level", String.valueOf(info.getRequiredLevel()));
		int kineticMax, energyMax;
		switch (info.getArmorType()) {
			case ASSAULT:
				kineticMax = 7000;
				energyMax = 5000;
				object.addAttribute("armor_category", "@obj_attr_n:armor_assault");
				break;
			case BATTLE:
				kineticMax = 6000;
				energyMax = 6000;
				object.addAttribute("armor_category", "@obj_attr_n:armor_battle");
				break;
			case RECON:
				kineticMax = 5000;
				energyMax = 7000;
				object.addAttribute("armor_category", "@obj_attr_n:armor_reconnaissance");
				break;
			default:
				throw new AssertionError("Armor type '"+info.getArmorType()+"' not implemented yet");
		}
		
		object.addAttribute("cat_armor_standard_protection.kinetic", calculateProtection(kineticMax, info.getProtection()));
		object.addAttribute("cat_armor_standard_protection.energy", calculateProtection(energyMax, info.getProtection()));
		object.addAttribute("cat_armor_special_protection.special_protection_type_heat", calculateProtection(6000, info.getProtection()));
		object.addAttribute("cat_armor_special_protection.special_protection_type_cold", calculateProtection(6000, info.getProtection()));
		object.addAttribute("cat_armor_special_protection.special_protection_type_acid", calculateProtection(6000, info.getProtection()));
		object.addAttribute("cat_armor_special_protection.special_protection_type_electricity", calculateProtection(6000, info.getProtection()));
		
		applySkillMods(object, info.getSkillMods());
		applyColors(object, info.getColor());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.WearableItemInfo info) {
		if (info == null)
			return;
		
		object.addAttribute("class_required", "@ui_roadmap:title_"+info.getRequiredProfession());
		object.addAttribute("required_combat_level", String.valueOf(info.getRequiredLevel()));
		
		if (!info.getRequiredFaction().isEmpty())
			object.addAttribute("faction_restriction", "@pvp_factions:"+info.getRequiredFaction());
		
		// Apply the mods!
		applySkillMods(object, info.getSkillMods());
		applyColors(object, info.getColor());
		
		// Add the race restrictions only if there are any
		String raceRestriction = buildRaceRestrictionString(info);
		if (!raceRestriction.isEmpty())
			object.addAttribute("species_restrictions.species_name", raceRestriction);
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.WeaponItemInfo info) {
		if (info == null)
			return;
		
		object.addAttribute("cat_wpn_damage.wpn_damage_type", "@obj_attr_n:" + info.getDamageType());
		object.addAttribute("cat_wpn_damage.wpn_category", "@obj_attr_n:wpn_category_" + info.getWeaponType().getNum());
		object.addAttribute("cat_wpn_damage.wpn_attack_speed", String.valueOf(info.getAttackSpeed()));
		object.addAttribute("cat_wpn_damage.damage", String.format("%d-%d", info.getMinDamage(), info.getMaxDamage()));
		if (info.getElementalType() != null) {	// Not all weapons have elemental damage.
			object.addAttribute("cat_wpn_damage.wpn_elemental_type", "@obj_attr_n:elemental_" + info.getElementalType());
			object.addAttribute("cat_wpn_damage.wpn_elemental_value", String.valueOf(info.getElementalDamage()));
		}
		
		object.addAttribute("cat_wpn_damage.weapon_dps", String.valueOf(info.getActualDps()));
		
		if (!info.getProcEffect().isEmpty())	// Not all weapons have a proc effect
			object.addAttribute("proc_name", info.getProcEffect());
		
		// TODO set DPS
		
		object.addAttribute("cat_wpn_other.wpn_range", String.format("%d-%dm", info.getMinRange(), info.getMaxRange()));
		// Ziggy: Special Action Cost would go under cat_wpn_other as well, but it's a pre-NGE artifact.
		
		WeaponObject weapon = (WeaponObject) object;
		weapon.setType(info.getWeaponType());
		weapon.setAttackSpeed((float) info.getAttackSpeed());
		weapon.setMinRange(info.getMinRange());
		weapon.setMaxRange(info.getMaxRange());
		weapon.setDamageType(info.getDamageType());
		weapon.setElementalType(info.getElementalType());
		weapon.setMinDamage(info.getMinDamage());
		weapon.setMaxDamage(info.getMaxDamage());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.CollectionItemInfo info) {
		if (info == null)
			return;
		
		object.addAttribute("collection_name", info.getSlotName());
		
		applyColors(object, info.getColor());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.CostumeItemInfo info) {
//		if (info == null)
//			return;
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.DnaItemInfo info) {
//		if (info == null)
//			return;
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.GrantItemInfo info) {
//		if (info == null)
//			return;
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.GenericItemInfo info) {
		if (info == null)
			return;
		
		if (info.getValue() != 0)
			object.addAttribute("charges", Integer.toString(info.getValue()));
		
		applyColors(object, info.getColor());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.ObjectItemInfo info) {
		if (info == null)
			return;
		
		if (info.getValue() != 0)
			object.addAttribute("charges", Integer.toString(info.getValue()));
		
		applyColors(object, info.getColor());
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.SchematicItemInfo info) {
//		if (info == null)
//			return;
	}
	
	private void applyAttributes(TangibleObject object, StaticItemLoader.StorytellerItemInfo info) {
//		if (info == null)
//			return;
	}
	
	private static void applySkillMods(TangibleObject object, Map<String, Integer> skillMods) {
		for (Map.Entry<String, Integer> modEntry : skillMods.entrySet())
			object.addAttribute(modEntry.getKey(), String.valueOf(modEntry.getValue()));
	}
	
	private static void applyColors(TangibleObject object, int [] colors) {
		for (int i = 0; i < 4; i++) {
			if (colors[i] >= 0) {
				object.putCustomization("/private/index_color_" + i, colors[i]);
			}
		}
	}
	
	private String buildRaceRestrictionString(StaticItemLoader.WearableItemInfo info) {
		String races = "";
		
		if (info.isRaceWookie())
			races = races.concat("Wookiee ");
		if (info.isRaceIthorian())
			races = races.concat("Ithorian ");
		if (info.isRaceRodian())
			races = races.concat("Rodian ");
		if (info.isRaceTrandoshan())
			races = races.concat("Trandoshan ");
		if (info.isRaceRest())
			races = races.concat("MonCal Human Zabrak Bothan Sullustan Twi'lek ");
		
		if (races.isEmpty())
			return "";
		return races.substring(0, races.length() - 1);
	}
	
	private static String calculateProtection(int max, double protection) {
		return String.valueOf(Math.floor(max * protection));
	}
	
	public interface ObjectCreationHandler {
		void success(List<SWGObject> createdObjects);
		boolean isIgnoreVolume();
		
		default void containerFull() {
			
		}
	}
	
	public static final class LootBoxHandler implements ObjectCreationHandler {

		private final CreatureObject receiver;

		public LootBoxHandler(CreatureObject receiver) {
			this.receiver = receiver;
		}
		
		@Override
		public void success(List<SWGObject> createdObjects) {
			long[] objectIds = new long[createdObjects.size()];

			for (int i = 0; i < objectIds.length; i++) {
				objectIds[i] = createdObjects.get(i).getObjectId();
			}

			receiver.sendSelf(new ShowLootBox(receiver.getObjectId(), objectIds));
		}

		@Override
		public boolean isIgnoreVolume() {
			return true;
		}
		
	}
}
