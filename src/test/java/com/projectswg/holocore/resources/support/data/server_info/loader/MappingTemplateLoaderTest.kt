package com.projectswg.holocore.resources.support.data.server_info.loader

import junit.framework.TestCase.assertNotNull
import org.junit.Test

class MappingTemplateLoaderTest {
	
	@Test
	fun canLoadRandomTemplate() {
		val randomKey = "object/building/corellia/shared_capitol_corellia.iff"
		val mappingTemplate = DataLoader.mappingTemplates().getMappingTemplate(randomKey)
		assertNotNull(mappingTemplate)
	}
}