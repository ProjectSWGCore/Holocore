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

class SuiTableWindow(prompt: String?, exportBtn: Boolean) : SuiWindow() {
	private val table = ArrayList<SuiTableColumn>()
	override var prompt: String?
		get() = super.prompt
		set(value) {
			super.prompt = value
			// Table windows use a different widget for prompt text
			setPropertyText("comp.Prompt.lblPrompt", value ?: return)
		}
	override var buttons: SuiButtons = SuiButtons.OK
		get() = super.buttons
		set(value) {
			field = value
			when (buttons) {
				SuiButtons.OK_CANCEL         -> {
					setOkButtonText()
					setCancelButtonText()
				}

				SuiButtons.OK_CANCEL_ALL     -> {
					setOkButtonText()
					setCancelButtonText()
					setShowOtherButton(true, "@all")
				}

				SuiButtons.OK_REFRESH        -> {
					setOkButtonText()
					setCancelButtonText("@refresh")
				}

				SuiButtons.OK_REFRESH_CANCEL -> {
					setOkButtonText()
					setCancelButtonText()
					setShowOtherButton(true, "@refresh")
				}

				SuiButtons.YES_NO            -> {
					setOkButtonText("@yes")
					setCancelButtonText("@no")
				}

				SuiButtons.REFRESH           -> {
					setOkButtonText("@refresh")
					setShowCancelButton(false)
				}

				SuiButtons.REFRESH_CANCEL    -> {
					setOkButtonText("@refresh")
					setCancelButtonText()
				}

				SuiButtons.OK                -> {
					setShowCancelButton(false)
					setOkButtonText()
				}

				else                         -> {
					setShowCancelButton(false)
					setOkButtonText()
				}
			}
		}

	init {
		this.suiScript = "Script.tablePage"

		if (prompt.isNullOrEmpty())
			setProperty("comp.Prompt", "Visible", "false")

		setShowExportButton(exportBtn)

		clearDataSource("comp.TablePage.dataTable")
	}

	override fun onDisplayRequest() {
		addReturnableProperty("comp.TablePage.table", "SelectedRow")
	}

	fun addColumn(columnName: String?, type: String?) {
		val index = table.size

		val column = SuiTableColumn()
		val sIndex = index.toString()

		addDataSource("comp.TablePage.dataTable", "Name", sIndex)
		setProperty("comp.TablePage.dataTable.$sIndex", "Label", columnName!!)
		setProperty("comp.TablePage.dataTable.$sIndex", "Type", type!!)

		table.add(column)
	}

	fun addCell(cellName: String?, cellObjId: Long, columnIndex: Int) {
		val column = table[columnIndex]

		val cellIndex: Int = column.cells.size

		val cell = SuiTableCell(cellName, cellObjId)
		column.cells.add(cell)

		addDataItem("comp.TablePage.dataTable.$columnIndex", "Name", "data$cellIndex")
		setProperty("comp.TablePage.dataTable.$columnIndex.data$cellIndex", "Value", cellName!!)
	}

	fun addCell(cellName: String?, columnIndex: Int) {
		addCell(cellName, 0, columnIndex)
	}

	fun setScrollExtent(x: Int, z: Int) {
		setProperty("comp.TablePage.header", "ScrollExtent", "$x,$z")
	}

	fun getCellId(column: Int, row: Int): Long {
		val cell: SuiTableCell = table[column].cells[row]

		return cell.id
	}

	fun getCellValue(column: Int, row: Int): String? {
		val cell: SuiTableCell = table[column].cells[row]

		return cell.value
	}

	private fun setShowExportButton(show: Boolean) {
		setProperty("btnExport", "Visible", show.toString())
	}

	private class SuiTableColumn(val cells: MutableList<SuiTableCell> = ArrayList())

	private class SuiTableCell(val value: String?, val id: Long)
	companion object {
		fun getSelectedRow(parameters: Map<String?, String?>): Int {
			val selectedIndex = parameters["comp.TablePage.table.SelectedRow"]
			if (selectedIndex != null) return selectedIndex.toInt()
			return -1
		}
	}
}
