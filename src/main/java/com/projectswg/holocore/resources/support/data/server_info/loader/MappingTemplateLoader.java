package com.projectswg.holocore.resources.support.data.server_info.loader;

import com.projectswg.common.data.swgfile.ClientFactory;
import com.projectswg.common.data.swgfile.visitors.DatatableData;
import com.projectswg.holocore.resources.gameplay.world.map.MappingTemplate;
import com.projectswg.holocore.resources.support.data.client_info.ServerFactory;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MappingTemplateLoader extends DataLoader {
	
	private final Map<String, MappingTemplate> mappingTemplates;
	
	public MappingTemplateLoader() {mappingTemplates = new HashMap<>();}
	
	@Override
	public void load() throws IOException {
		DatatableData table = ServerFactory.getDatatable("map/map_locations.iff");
		for (int row = 0; row < table.getRowCount(); row++) {
			MappingTemplate template = new MappingTemplate();
			template.setTemplate(ClientFactory.formatToSharedFile(table.getCell(row, 0).toString()));
			template.setName(table.getCell(row, 1).toString());
			template.setCategory(table.getCell(row, 2).toString());
			template.setSubcategory(table.getCell(row, 3).toString());
			template.setType((Integer) table.getCell(row, 4));
			template.setFlag((Integer) table.getCell(row, 5));
			
			mappingTemplates.put(template.getTemplate(), template);
		}
	}
	
	@Nullable
	public MappingTemplate getMappingTemplate(String key) {
		return mappingTemplates.get(key);
	}
}
