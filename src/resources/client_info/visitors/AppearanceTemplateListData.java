package resources.client_info.visitors;

import resources.client_info.ClientData;
import resources.client_info.IffNode;
import resources.client_info.SWGFile;

public class AppearanceTemplateListData extends ClientData {
	
	private String lodFile = null;
	
	@Override
	public void readIff(SWGFile iff) {
		readForms(iff);
	}
	
	private void readForms(SWGFile iff) {
		IffNode form = null;
		while ((form = iff.enterNextForm()) != null) {
			switch (form.getTag()) {
				case "0000":
					readChunks(form);
					break;
				default:
					System.err.println("Unknown APT form: " + form.getTag() + " in " + iff.getCurrentForm().getTag());
					break;
			}
			iff.exitForm();
		}
	}
	
	private void readChunks(IffNode node) {
		IffNode chunk = null;
		while ((chunk = node.getNextUnreadChunk()) != null) {
			switch (chunk.getTag()) {
				case "NAME":
					lodFile = chunk.readString();
					break;
				default:
					System.err.println("Unknown APT chunk: " + chunk.getTag());
					break;
			}
		}
	}
	
	public String getAppearanceFile() {
		return lodFile;
	}
	
}
