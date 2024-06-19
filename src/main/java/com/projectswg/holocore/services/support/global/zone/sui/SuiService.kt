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
package com.projectswg.holocore.services.support.global.zone.sui

import com.projectswg.common.data.sui.SuiBaseWindow
import com.projectswg.common.data.sui.SuiComponent
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.common.network.packets.swg.zone.server_ui.SuiCreatePageMessage
import com.projectswg.common.network.packets.swg.zone.server_ui.SuiEventNotification
import com.projectswg.common.network.packets.swg.zone.server_ui.SuiForceClosePage
import com.projectswg.holocore.intents.support.global.network.InboundPacketIntent
import com.projectswg.holocore.intents.support.global.zone.SuiWindowIntent
import com.projectswg.holocore.resources.support.global.player.Player
import me.joshlarson.jlcommon.control.IntentHandler
import me.joshlarson.jlcommon.control.Service
import me.joshlarson.jlcommon.log.Log
import java.util.concurrent.ConcurrentHashMap

class SuiService : Service() {
	private val windows: MutableMap<Long, MutableList<SuiBaseWindow>> = ConcurrentHashMap()

	@IntentHandler
	private fun handleInboundPacketIntent(gpi: InboundPacketIntent) {
		val packet = gpi.packet
		if (packet is SuiEventNotification)
			handleSuiEventNotification(gpi.player, packet)
	}

	@IntentHandler
	private fun handleSuiWindowIntent(swi: SuiWindowIntent) {
		when (swi.event) {
			SuiWindowIntent.SuiWindowEvent.NEW   -> displayWindow(swi.player, swi.window)
			SuiWindowIntent.SuiWindowEvent.CLOSE -> {
				closeWindow(swi.player, swi.window)
			}
		}
	}

	private fun handleSuiEventNotification(player: Player, p: SuiEventNotification) {
		val activeWindows: MutableList<SuiBaseWindow>? = windows[player.networkId]
		if (activeWindows == null || activeWindows.size <= 0) return

		val window: SuiBaseWindow? = getWindowById(activeWindows, p.windowId)

		if (window == null) {
			Log.w("Received window ID %d that is not assigned to the player %s", p.windowId, player)
			return
		}

		val component: SuiComponent? = window.getSubscriptionByIndex(p.eventIndex)

		if (component == null) {
			Log.w("SuiWindow %s retrieved null subscription from supplied event index %d", window, p.eventIndex)
			return
		}

		val suiSubscribedProperties: List<String> = component.subscribedProperties ?: return

		val eventNotificationProperties: List<String> = p.subscribedToProperties
		val eventPropertySize = eventNotificationProperties.size

		if (suiSubscribedProperties.size < eventPropertySize) return

		val callback: String? = component.subscribeToEventCallback
		val event: SuiEvent = SuiEvent.valueOf(component.subscribedToEventType)

		val parameters: MutableMap<String, String> = HashMap()
		for (i in 0 until eventPropertySize) {
			parameters[suiSubscribedProperties[i]] = eventNotificationProperties[i]
		}

		window.getJavaCallback(callback)?.accept(event, parameters)

		// Both of these events "closes" the sui window for the client, so we have no need for the server to continue tracking the window.
		if (event == SuiEvent.OK_PRESSED || event == SuiEvent.CANCEL_PRESSED) activeWindows.remove(window)
	}

	private fun displayWindow(player: Player, window: SuiBaseWindow) {
		val id = createWindowId()
		window.id = id

		player.sendPacket(SuiCreatePageMessage(window))

		val networkId: Long = player.networkId
		val activeWindows: MutableList<SuiBaseWindow> = windows.computeIfAbsent(networkId) { ArrayList() }
		activeWindows.add(window)
	}

	private fun closeWindow(player: Player, window: SuiBaseWindow) {
		val id: Int = window.id

		val activeWindows: MutableList<SuiBaseWindow>? = windows[player.networkId]

		if (activeWindows == null) {
			Log.w("Tried to close window id %d for player %s but it doesn't exist in the active windows.", id, player)
			return
		}

		if (!activeWindows.remove(window)) {
			Log.w("Tried to close window id %d for player %s but it doesn't exist in the active windows.", id, player)
			return
		}

		player.sendPacket(SuiForceClosePage(id))
	}

	private fun closeWindow(player: Player, windowId: Int) {
		val activeWindows: List<SuiBaseWindow?> = windows[player.networkId]!!
		val window: SuiBaseWindow? = activeWindows[windowId]
		if (window == null) {
			Log.w("Cannot close window with id %d as it doesn't exist in player %s active windows", windowId, player)
			return
		}

		closeWindow(player, window)
	}

	private fun getWindowById(windows: List<SuiBaseWindow>, id: Int): SuiBaseWindow? {
		return windows.firstOrNull { it.id == id }
	}

	companion object {
		fun createWindowId(): Int {
			return (System.currentTimeMillis() % Int.MAX_VALUE).toInt()
		}
	}
}
