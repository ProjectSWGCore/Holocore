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

import java.util.*
import kotlin.collections.ArrayList

class SuiListBox : SuiWindow() {
	private val _list: MutableList<SuiListBoxItem> = ArrayList()
	val list: List<SuiListBoxItem>
		get() = Collections.unmodifiableList(_list)

	override var buttons: SuiButtons = SuiButtons.OK_CANCEL
		get() = super.buttons
		set(value) {
			field = value
			when (buttons) {
				SuiButtons.OK_CANCEL              -> {
					setOkButtonText("@ok")
					setCancelButtonText("@cancel")
				}

				SuiButtons.YES_NO                 -> {
					setOkButtonText("@yes")
					setCancelButtonText("@no")
				}

				SuiButtons.OK_REFRESH             -> {
					setOkButtonText("@ok")
					setCancelButtonText("@refresh")
				}

				SuiButtons.OK_CANCEL_REFRESH      -> {
					setOkButtonText("@ok")
					setCancelButtonText("@cancel")
					setShowOtherButton(true, "@refresh")
				}

				SuiButtons.OK_CANCEL_ALL          -> {
					setOkButtonText("@ok")
					setCancelButtonText("@cancel")
					setShowOtherButton(true, "@all")
				}

				SuiButtons.REFRESH                -> {
					setOkButtonText("@refresh")
					setShowCancelButton(false)
				}

				SuiButtons.REFRESH_CANCEL         -> {
					setOkButtonText("@refresh")
					setCancelButtonText("@cancel")
				}

				SuiButtons.REMOVE_CANCEL          -> {
					setOkButtonText("@remove")
					setCancelButtonText("@cancel")
				}

				SuiButtons.MOVEUP_MOVEDOWN_DONE   -> {
					setOkButtonText("@moveup")
					setCancelButtonText("@done")
					setShowOtherButton(true, "@movedown")
				}

				SuiButtons.BET_MAX_BET_ONE_SPIN   -> {
					setOkButtonText("@ok")
					setCancelButtonText("@spin")
					setShowOtherButton(true, "@bet_one")
				}

				SuiButtons.OK, SuiButtons.DEFAULT -> {
					setOkButtonText("@ok")
					setShowCancelButton(false)
				}

				else                              -> {
					setOkButtonText("@ok")
					setShowCancelButton(false)
				}
			}
		}

	init {
		super.suiScript = "Script.listBox"
		super.buttons = buttons
		super.title = title
		super.prompt = prompt
		clearDataSource("List.dataList")
	}

	override fun onDisplayRequest() {
		addReturnableProperty("List.lstList", "SelectedRow")
		addReturnableProperty("bg.caption.lblTitle", "Text")
	}

	@JvmOverloads
	fun addListItem(name: String, id: Long = -1, obj: Any? = null) {
		val item = SuiListBoxItem(name, id, obj)

		val index = _list.size
		val sIndex = index.toString()
		addDataItem("List.dataList", "Name", sIndex)
		setProperty("List.dataList.$sIndex", "Text", name)

		_list.add(item)
	}

	fun addListItem(name: String, obj: Any?) {
		addListItem(name, -1, obj)
	}

	fun getListItem(index: Int): SuiListBoxItem {
		return _list[index]
	}

	class SuiListBoxItem(val name: String, val id: Long, val obj: Any?)

	companion object {
		fun getSelectedRow(parameters: Map<String, String>): Int {
			val selectedIndex = parameters["List.lstList.SelectedRow"]
			if (selectedIndex != null) return selectedIndex.toInt()
			return -1
		}
	}
}
