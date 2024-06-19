/***********************************************************************************
 * Copyright (c) 2024 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.services.gameplay.junkdealer

import com.projectswg.common.data.encodables.oob.ProsePackage
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.gameplay.junkdealer.SellItemsIntent
import com.projectswg.holocore.intents.support.global.chat.SystemMessageIntent.Companion.broadcastPersonal
import com.projectswg.holocore.intents.support.objects.DestroyObjectIntent
import com.projectswg.holocore.intents.support.objects.MoveObjectIntent
import com.projectswg.holocore.resources.support.global.player.Player
import com.projectswg.holocore.resources.support.global.zone.sui.SuiButtons
import com.projectswg.holocore.resources.support.global.zone.sui.SuiListBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiMessageBox
import com.projectswg.holocore.resources.support.global.zone.sui.SuiWindow
import com.projectswg.holocore.resources.support.objects.swg.ServerAttribute
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject
import com.projectswg.holocore.resources.support.objects.swg.custom.AIObject
import com.projectswg.holocore.resources.support.objects.swg.tangible.TangibleObject
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import kotlin.math.max

class JunkDealerService : Service() {
	private val suiWindowMap: MutableMap<CreatureObject, Session> = HashMap()

	@IntentHandler
	private fun handleSellItemsIntent(intent: SellItemsIntent) {
		val player = intent.player
		val npc = intent.npc

		displaySuiWindow(player, npc)
	}

	@IntentHandler
	private fun handleMoveObjectIntent(intent: MoveObjectIntent) {
		val obj = intent.obj as? CreatureObject ?: return
		val session = suiWindowMap[obj] ?: return
		val npc: AIObject = session.npc

		if (isWithinRange(obj, npc)) {
			return
		}

		suiWindowMap.remove(obj)

		val suiWindow: SuiWindow = session.window

		obj.owner?.let { suiWindow.close(it) }
	}

	private fun displaySuiWindow(player: Player, npc: AIObject) {
		val sellableItems = getSellableItems(player)

		val window = if (sellableItems.isEmpty()) {
			createNoItemsWindow()
		} else {
			createSellableItemsWindow(player, sellableItems)
		}

		window.display(player)
		val session = Session(npc, window)
		suiWindowMap[player.creatureObject] = session
	}

	private fun createNoItemsWindow(): SuiWindow {
		SuiMessageBox().run {
			title = "@loot_dealer:sell_title"
			prompt = "@loot_dealer:no_items"
			buttons = SuiButtons.OK
			return this
		}
	}

	private fun createSellableItemsWindow(player: Player, sellableItems: List<TangibleObject>): SuiWindow {
		SuiListBox().run {
			title = "@loot_dealer:sell_title"
			prompt = "@loot_dealer:sell_prompt"

			for (sellableItem in sellableItems) {
				val listItem = createListItem(sellableItem)

				addListItem(listItem, sellableItem)
			}

			setProperty("btnOk", "Text", "@loot_dealer:btn_sell")
			addCallback(SuiEvent.OK_PRESSED, "handleItemChoice") { _: SuiEvent, parameters: Map<String, String> -> handleItemChoice(sellableItems, player, parameters) }

			return this
		}
	}

	private fun getSellableItems(player: Player): List<TangibleObject> {
		val creatureObject = player.creatureObject
		val inventory = creatureObject.inventory
		val sellableItems: MutableList<TangibleObject> = ArrayList()

		val childObjects = inventory.childObjectsRecursively

		for (childObject in childObjects) {
			if (childObject is TangibleObject) {
				val itemValue = childObject.getServerAttribute(ServerAttribute.ITEM_VALUE) as Int?

				if (itemValue != null) {
					if (getPriceForItem(childObject) > 0) {
						if (isContainer(childObject)) {
							if (isEmpty(childObject)) {
								sellableItems.add(childObject)
							}
						} else {
							sellableItems.add(childObject)
						}
					}
				}
			}
		}

		return sellableItems
	}

	private fun createListItem(item: TangibleObject): String {
		val baseFormat = "[%d] %s"
		val price = getPriceForItem(item)

		return String.format(baseFormat, price, item.objectName)
	}

	private fun handleItemChoice(sellableItems: List<TangibleObject>, player: Player, parameters: Map<String, String>) {
		val creatureObject = player.creatureObject
		val session = suiWindowMap.remove(creatureObject) ?: return

		val row = SuiListBox.getSelectedRow(parameters)
		val sellableItem = sellableItems[row]

		val price = getPriceForItem(sellableItem)

		creatureObject.addToCash(price.toLong())

		val itemSoldProse = ProsePackage(StringId("junk_dealer", "prose_sold_junk"), "TT", sellableItem.objectName, "DI", price)
		broadcastPersonal(player, itemSoldProse)

		val destroyObjectIntent = DestroyObjectIntent((sellableItem))
		val sellItemsIntent = SellItemsIntent(player, session.npc)
		sellItemsIntent.broadcastAfterIntent(destroyObjectIntent)
		destroyObjectIntent.broadcast()
	}

	private fun getPriceForItem(item: TangibleObject): Int {
		val itemValue = item.getServerAttribute(ServerAttribute.ITEM_VALUE) as Int? ?: return 0

		val stackSize = max(1.0, item.counter.toDouble()).toInt()

		return itemValue * stackSize
	}

	private fun isContainer(item: TangibleObject): Boolean {
		return item.containerType == 2
	}

	private fun isEmpty(item: TangibleObject): Boolean {
		return item.childObjects.isEmpty()
	}

	private fun isWithinRange(creatureObject: CreatureObject, npc: AIObject): Boolean {
		val playerWorldLocation = creatureObject.worldLocation
		val npcWorldLocation = npc.worldLocation
		val playerTerrain = playerWorldLocation.terrain
		val npcTerrain = npcWorldLocation.terrain

		if (playerTerrain != npcTerrain) {
			return false
		}

		val distanceTo = playerWorldLocation.distanceTo(npcWorldLocation)

		return distanceTo <= SUI_ALLOWED_DISTANCE
	}

	private class Session(val npc: AIObject, val window: SuiWindow)
	companion object {
		private const val SUI_ALLOWED_DISTANCE = 7
	}
}
