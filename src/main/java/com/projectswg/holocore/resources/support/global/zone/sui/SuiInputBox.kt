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

class SuiInputBox : SuiWindow() {

	override var buttons: SuiButtons = SuiButtons.OK_CANCEL
		get() = super.buttons
		set(value) {
			field = value
			when (field) {
				SuiButtons.OK_CANCEL       -> {
					setProperty("btnOk", "Visible", "True")
					setProperty("btnCancel", "Visible", "True")
					setCancelButtonText("@cancel")
					setOkButtonText("@ok")
					displayComboBox(false)
				}

				SuiButtons.COMBO_OK        -> {
					setProperty("btnOk", "Visible", "True")
					setProperty("btnCancel", "Visible", "False")
					setOkButtonText("@ok")
					displayComboBox(true)
				}

				SuiButtons.COMBO_OK_CANCEL -> {
					setProperty("btnOk", "Visible", "True")
					setProperty("btnCancel", "Visible", "True")
					setOkButtonText("@ok")
					setCancelButtonText("@cancel")
					displayComboBox(true)
				}

				SuiButtons.OK              -> {
					setProperty("btnOk", "Visible", "True")
					setProperty("btnCancel", "Visible", "False")
					setOkButtonText("@ok")
					displayComboBox(false)
				}

				else                       -> {
					setProperty("btnOk", "Visible", "True")
					setProperty("btnCancel", "Visible", "False")
					setOkButtonText("@ok")
					displayComboBox(false)
				}
			}
		}

	init {
		super.suiScript = "Script.inputBox"
		super.buttons = buttons
		super.title = title
		super.prompt = prompt
	}

	override fun onDisplayRequest() {
		addReturnableProperty("txtInput", "LocalText")
		addReturnableProperty("cmbInput", "SelectedText")
		addReturnableProperty("txtInput", "MaxLength")
	}

	private fun displayComboBox(show: Boolean) {
		if (show) {
			setProperty("cmbInput", "Visible", "True")
			setProperty("cmbInput", "Visible", "True")

			setProperty("txtInput", "Enabled", "False")
			setProperty("txtInput", "Visible", "False")
		} else {
			setProperty("cmbInput", "Visible", "False")
			setProperty("cmbInput", "Visible", "False")

			setProperty("txtInput", "Enabled", "True")
			setProperty("txtInput", "Visible", "True")
		}
	}

	fun allowStringCharacters(stringCharacters: Boolean) {
		setProperty("txtInput", "NumericInteger", stringCharacters.toString())
	}

	fun setMaxLength(maxLength: Int) {
		setProperty("txtInput", "MaxLength", maxLength.toString())
	}

	companion object {
		fun getEnteredText(parameters: Map<String, String>): String {
			val input = parameters["txtInput.LocalText"]
			return input ?: ""
		}

		fun getSelectedComboText(parameters: Map<String, String>): String? {
			return parameters["cmbInput.SelectedText"]
		}
	}
}
