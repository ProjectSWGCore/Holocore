package resources.client_info.visitors;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import utilities.ByteUtilities;

public class ProfTemplateData extends ClientData {
	
	private List<Template> templates = new ArrayList<Template>();
	
	private class Template {
		
		private List<String> items;
		private String template;
		
		public Template(String template) {
			this.template = template;
			this.items = new ArrayList<String>();
		}

		public List<String> getItems() {
			return items;
		}

		public String getTemplate() {
			return template;
		}
	}
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {
		switch(node) {
		
		case "PTMPNAME":
			templates.add(new Template(ByteUtilities.nextString(data)));
			break;
			
		case "ITEM":
			int index = templates.size() - 1;
			data.getInt(); // empty int it seems for all items
			templates.get(index).getItems().add(ByteUtilities.nextString(data));
			break;
			
		}
	}

	/**
	 * Gets the items for the race
	 * @param race Race IFF template
	 * @return {@link List} of the newbie clothing items for the racial template
	 */
	public List<String> getItems(String race) {
		for (Template t : templates) {
			if (t.getTemplate().equals(race))
				return t.getItems();
		}
		return null;
	}
}
