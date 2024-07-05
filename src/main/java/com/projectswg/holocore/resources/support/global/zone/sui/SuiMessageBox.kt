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

class SuiMessageBox : SuiWindow() {

	override var buttons: SuiButtons = SuiButtons.OK_CANCEL
		get() = super.buttons
		set(value) {
			field = value
			when (buttons) {
				SuiButtons.OK_CANCEL          -> {
					setOkButtonText()
					setCancelButtonText()
					setShowRevertButton(false)
				}

				SuiButtons.YES_NO             -> {
					setOkButtonText("@yes")
					setCancelButtonText("@no")
					setShowRevertButton(false)
				}

				SuiButtons.YES_NO_CANCEL      -> {
					setOkButtonText("@yes")
					setRevertButtonText("@no")
					setCancelButtonText()
				}

				SuiButtons.YES_NO_MAYBE       -> {
					setOkButtonText("@yes")
					setRevertButtonText("@maybe")
					setCancelButtonText("@no")
				}

				SuiButtons.YES_NO_ABSTAIN     -> {
					setOkButtonText("@yes")
					setCancelButtonText("@no")
					setRevertButtonText("@abstain")
				}

				SuiButtons.RETRY_CANCEL       -> {
					setOkButtonText("@retry")
					setCancelButtonText("@cancel")
					setShowRevertButton(false)
				}

				SuiButtons.RETRY_ABORT_CANCEL -> {
					setOkButtonText("@abort")
					setCancelButtonText("@cancel")
					setRevertButtonText("@retry")
				}

				SuiButtons.OK_LEAVE_GROUP     -> {
					setOkButtonText()
					setCancelButtonText("@group:leave_group")
					setShowRevertButton(false)
				}

				SuiButtons.OK                 -> {
					setOkButtonText()
					setShowRevertButton(false)
					setShowCancelButton(false)
				}

				else                          -> {
					setOkButtonText()
					setShowRevertButton(false)
					setShowCancelButton(false)
				}
			}
		}

	init {
		super.suiScript = "Script.messageBox"
	}

	private fun setShowRevertButton(shown: Boolean) {
		val value = shown.toString()
		setProperty("btnRevert", "Enabled", value)
		setProperty("btnRevert", "Visible", value)
	}

	private fun setRevertButtonText(text: String) {
		setProperty("btnRevert", "Text", text)
	}
}
