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
package com.projectswg.holocore.resources.support.global.zone.sui

import com.projectswg.common.data.sui.ISuiCallback
import com.projectswg.common.data.sui.SuiBaseWindow
import com.projectswg.common.data.sui.SuiEvent
import com.projectswg.holocore.intents.support.global.zone.SuiWindowIntent
import com.projectswg.holocore.resources.support.global.player.Player

open class SuiWindow : SuiBaseWindow() {
	open var title: String? = null
		set(value) {
			field = value
			setPropertyText("bg.caption.lblTitle", value ?: return)
		}
	open var prompt: String? = null
		set(value) {
			field = value
			setPropertyText("Prompt.lblPrompt", value ?: return)
		}
	open var buttons: SuiButtons = SuiButtons.OK
		set(value) {
			field = value
			when (buttons) {
				SuiButtons.OK_CANCEL -> {
					setOkButtonText()
					setCancelButtonText()
				}

				SuiButtons.OK        -> setOkButtonText()
				else                 -> setOkButtonText()
			}
		}

	fun setPropertyText(widget: String, value: String) {
		setProperty(widget, "Text", value)
	}

	fun setAutosave(autosave: Boolean) {
		setProperty("this", "autosave", autosave.toString())
	}

	fun setLocation(x: Int, y: Int) {
		setProperty("this", "Location", "$x,$y")
	}

	fun setSize(width: Int, height: Int) {
		setProperty("this", "Size", "$width,$height")
	}

	protected fun setOkButtonText() {
		setOkButtonText("@ok")
	}

	protected fun setOkButtonText(text: String) {
		setProperty("btnOk", "Text", text)
	}

	protected fun setCancelButtonText() {
		setCancelButtonText("@cancel")
	}

	protected fun setCancelButtonText(text: String) {
		setProperty("btnCancel", "Text", text)
	}

	protected fun setShowOtherButton(display: Boolean, text: String) {
		setProperty("btnOther", "Visible", display.toString())
		if (display) {
			setProperty("btnOther", "Text", text)
			addOtherButtonReturnable()
		}
	}

	protected fun setShowCancelButton(display: Boolean) {
		val value = display.toString()
		setProperty("btnCancel", "Enabled", value)
		setProperty("btnCancel", "Visible", value)
	}

	fun addOkButtonCallback(callback: String, suiCallback: ISuiCallback) {
		addCallback(SuiEvent.OK_PRESSED, callback, suiCallback)
	}

	fun addCancelButtonCallback(callback: String, suiCallback: ISuiCallback) {
		addCallback(SuiEvent.CANCEL_PRESSED, callback, suiCallback)
	}

	protected fun addOtherButtonReturnable() {
		addReturnableProperty(SuiEvent.OK_PRESSED, "this", "otherPressed")
	}

	fun display(player: Player) {
		onDisplayRequest()
		SuiWindowIntent(player, this, SuiWindowIntent.SuiWindowEvent.NEW).broadcast()
	}

	fun close(player: Player) {
		SuiWindowIntent(player, this, SuiWindowIntent.SuiWindowEvent.CLOSE).broadcast()
	}

	// Add a simple ok/cancel button subscriptions if no callbacks is assigned so the server is sent a SuiEventNotification when client destroys the page.
	// Doing it this way will ensure the server removes the stored SuiWindow from memory.
	private fun prepare() {
		if (hasSubscriptionComponent()) return

		subscribeToEvent(SuiEvent.OK_PRESSED.value, "", "handleSUI")
		subscribeToEvent(SuiEvent.CANCEL_PRESSED.value, "", "handleSUI")
	}

	protected open fun onDisplayRequest() {
		prepare()
	}
}
