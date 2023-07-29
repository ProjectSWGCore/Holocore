/***********************************************************************************
 * Copyright (c) 2023 /// Project SWG /// www.projectswg.com                       *
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
package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.CRC
import com.projectswg.common.data.encodables.oob.StringId
import com.projectswg.common.data.schematic.DraftSchematic
import com.projectswg.common.data.schematic.DraftSlotDataOption
import com.projectswg.common.data.schematic.IngridientSlot
import com.projectswg.common.data.schematic.IngridientSlot.IngridientType
import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.support.data.server_info.StandardLog
import me.joshlarson.json.JSON
import me.joshlarson.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.text.Charsets.UTF_8

class DraftSchematicLoader : DataLoader() {

	private val draftSchematics: MutableMap<String, DraftSchematic> = HashMap()

	fun getDraftSchematic(draftSchematicIff: String): DraftSchematic? {
		return draftSchematics[draftSchematicIff]
	}

	override fun load() {
		val what = "draft schematics"
		val start = StandardLog.onStartLoad(what)

		loadAllDraftSchematics()

		StandardLog.onEndLoad(draftSchematics.size, what, start)
	}

	private fun loadAllDraftSchematics() {
		val files = findAllDraftSchematicJsonFiles()

		for (file in files) {
			val jsonFilePath = file.path
			val iffDraftSchematicPath = jsonFilePath.replace("\\", "/").replaceFirst("serverdata/", "object/").replace(".json", ".iff")
			val fileToJsonString = fileToJsonString(file)
			val sharedIffDraftSchematicPath = ClientFactory.formatToSharedFile(iffDraftSchematicPath)
			val draftSchematic = jsonToDraftSchematic(fileToJsonString, sharedIffDraftSchematicPath)
			draftSchematic.isCanManufacture = true

			draftSchematics[sharedIffDraftSchematicPath] = draftSchematic
		}
	}

	private fun findAllDraftSchematicJsonFiles(): List<File> {
		val base = Paths.get("serverdata/draft_schematic")
		val pathStream = Files.find(base, 10, { path, _ -> path.toString().endsWith(".json") })
		
		return pathStream.map { it.toFile() }.toList()
	}

	private fun fileToJsonString(file: File): String {
		return file.readText(charset = UTF_8)
	}

	private fun jsonToDraftSchematic(json: String, iffDraftSchematicPath: String): DraftSchematic {
		val draftSchematic = DraftSchematic()
		val jsonObject = JSON.readObject(json)

		setItemsPerContainer(jsonObject, draftSchematic)
		setCraftedSharedTemplate(jsonObject, draftSchematic)
		setCombinedCrc(iffDraftSchematicPath, draftSchematic)
		setVolume(jsonObject, draftSchematic)
		setComplexity(jsonObject, draftSchematic)
		setSlots(jsonObject, draftSchematic)

		return draftSchematic
	}

	private fun setSlots(jsonObject: JSONObject, draftSchematic: DraftSchematic) {
		if (jsonObject.containsKey("slots")) {
			val array = jsonObject.getArray("slots")
			for (any in array) {
				val slotObject = any as Map<*, *>
				val name = stringIdName(slotObject)
				val optional = slotObject["optional"] as Boolean
				val slot = IngridientSlot(name, optional)
				draftSchematic.ingridientSlot.add(slot)

				setOptions(slotObject, slot)
			}
		}
	}

	private fun setOptions(slotObject: Map<*, *>, slot: IngridientSlot) {
		val options = slotObject["options"] as List<Map<*, *>>
		for (option in options) {
			val ingredientType = ingridientType(option)
			setIngredients(option, slot, ingredientType)
		}
	}

	private fun ingridientType(option: Map<*, *>) : IngridientType {
		val value = option["ingredientType"] as String
		
		return when (value) {
			"IT_none" -> IngridientType.IT_NONE
			"IT_resourceType" -> IngridientType.IT_RESOURCE_TYPE
			"IT_resourceClass" -> IngridientType.IT_RESOURCE_CLASS
			"IT_template" -> IngridientType.IT_TEMPLATE
			"IT_templateGeneric" -> IngridientType.IT_TEMPLATE
			"IT_schematic" -> IngridientType.IT_SCHEMATIC
			else -> throw IllegalArgumentException("Unknown ingredient type: $value. Maybe it just needs to be mapped?")
		}
	}

	private fun setIngredients(option: Map<*, *>, slot: IngridientSlot, ingredientType: IngridientType) {
		val ingredients = option["ingredients"] as List<Map<*, *>>

		for (ingredient in ingredients) {
			val name = stringIdName(ingredient)
			val ingredientName = ingredient["ingredient"] as String
			val amount = (ingredient["count"] as Long).toInt()
			slot.addSlotDataOption(DraftSlotDataOption(name, ingredientName, ingredientType, amount))
		}
	}

	private fun setComplexity(jsonObject: JSONObject, draftSchematic: DraftSchematic) {
		val complexity = jsonObject["complexity"] as Long?
		if (complexity != null) {
			draftSchematic.complexity = complexity.toInt()
		}
	}

	private fun setVolume(jsonObject: JSONObject, draftSchematic: DraftSchematic) {
		val volume = jsonObject["volume"] as Long?
		if (volume != null) {
			draftSchematic.volume = volume.toInt()
		}
	}

	private fun setItemsPerContainer(jsonObject: JSONObject, draftSchematic: DraftSchematic) {
		val itemsPerContainer = jsonObject["itemsPerContainer"] as Long?
		if (itemsPerContainer != null) {
			draftSchematic.itemsPerContainer = itemsPerContainer.toInt()
		}
	}

	private fun setCraftedSharedTemplate(jsonObject: JSONObject, draftSchematic: DraftSchematic) {
		val craftedObjectTemplate = jsonObject["craftedObjectTemplate"] as String?
		if (!craftedObjectTemplate.isNullOrEmpty()) {
			draftSchematic.craftedSharedTemplate = ClientFactory.formatToSharedFile(craftedObjectTemplate)
		}
	}

	private fun setCombinedCrc(iffDraftSchematicPath: String, draftSchematic: DraftSchematic) {
		val serverCrc = getDraftSchematicServerCrc(iffDraftSchematicPath)
		val clientCrc = getDraftSchematicClientCrc(iffDraftSchematicPath)
		val combinedCrc = combinedCrc(serverCrc = serverCrc, clientCrc = clientCrc)
		draftSchematic.combinedCrc = combinedCrc
	}

	private fun stringIdName(map: Map<*, *>): StringId {
		val nameStrings = map["name"] as List<String>
		return StringId(nameStrings[0], nameStrings[1])
	}

	private fun getDraftSchematicServerCrc(schematicInGroupShared: String): Int {
		return CRC.getCrc(schematicInGroupShared)
	}

	private fun getDraftSchematicClientCrc(schematicInGroupShared: String): Int {
		val templateWithoutPrefix = schematicInGroupShared.replace("object/draft_schematic/", "")
		return CRC.getCrc(templateWithoutPrefix)
	}

	private fun combinedCrc(serverCrc: Int, clientCrc: Int): Long {
		return serverCrc.toLong() shl 32 and -0x100000000L or (clientCrc.toLong() and 0x00000000FFFFFFFFL)
	}
}