package resources.client_info;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Waverunner on 6/4/2015
 */
public class SWGFile {
	private File file;
	private IffNode master;
	private IffNode currentForm;

	public SWGFile(String type) {
		this.master = new IffNode(type, true);
		this.currentForm = master;
	}

	public SWGFile(File file, String type) {
		this(type);
		this.file = file;
	}

	public void save(File file) throws IOException {
		FileOutputStream outputStream = new FileOutputStream(file, false);
		outputStream.write(getData());
		outputStream.close();
	}

	public void save() throws IOException {
		save(file);
	}

	public void addForm(String tag) {
		addForm(tag, true);
	}

	public void addForm(String tag, boolean enterForm) {
		IffNode form = new IffNode(tag, true);
		currentForm.addChild(form);

		if (enterForm)
			currentForm = form;
	}

	public IffNode addChunk(String tag) {
		IffNode chunk = new IffNode(tag, false);
		currentForm.addChild(chunk);
		return chunk;
	}

	public byte[] getData() {
		return master.getBytes();
	}
}