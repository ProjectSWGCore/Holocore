package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.holocore.resources.gameplay.world.map.MappingTemplate;
import com.projectswg.holocore.resources.support.data.server_info.SdbLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MappingTemplateLoader extends DataLoader {
	
	private final Map<String, MappingTemplate> mappingTemplates;
	
	public MappingTemplateLoader() {mappingTemplates = new HashMap<>();}
	
	@Override
	public void load() throws IOException {
		try (SdbLoader.SdbResultSet set = SdbLoader.load(new File("serverdata/map/map_locations.sdb"))) {
			while (set.next()) {
				MappingTemplate template = new MappingTemplate();
				template.setTemplate(ClientFactory.formatToSharedFile(set.getText("Template")));
				template.setName(set.getText("Name"));
				template.setCategory(set.getText("Category"));
				template.setSubcategory(set.getText("Subcategory"));
				template.setType((int) set.getInt("Type"));
				template.setFlag((int) set.getInt("Flag"));
				
				mappingTemplates.put(template.getTemplate(), template);
			}
		}			
	}
	
	@Nullable
	public MappingTemplate getMappingTemplate(String key) {
		return mappingTemplates.get(key);
	}
}
