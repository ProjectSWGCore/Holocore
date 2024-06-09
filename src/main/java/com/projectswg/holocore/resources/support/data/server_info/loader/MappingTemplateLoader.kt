package com.projectswg.holocore.resources.support.data.server_info.loader

import com.projectswg.common.data.swgfile.ClientFactory
import com.projectswg.holocore.resources.gameplay.world.map.MappingTemplate
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader
import java.io.File
import java.io.IOException

class MappingTemplateLoader : DataLoader() {
	private val mappingTemplates: MutableMap<String?, MappingTemplate> = HashMap()

	@Throws(IOException::class)
	override fun load() {
		SdbLoader.load(File("serverdata/map/map_locations.sdb")).use { set ->
			while (set.next()) {
				val template = MappingTemplate()
				template.template = ClientFactory.formatToSharedFile(set.getText("Template"))
				template.name = set.getText("Name")
				template.category = set.getText("Category")
				template.subcategory = set.getText("Subcategory")
				template.type = set.getInt("Type").toInt()
				template.flag = set.getInt("Flag").toInt()

				mappingTemplates[template.template] = template
			}
		}
	}

	fun getMappingTemplate(key: String?): MappingTemplate? {
		return mappingTemplates[key]
	}
}
