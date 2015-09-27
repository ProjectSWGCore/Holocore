package utilities;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

public class SdbGenerator {
	
	private static final Charset ASCII = Charset.forName("ASCII");
	
	private final File file;
	private BufferedWriter writer;
	
	public SdbGenerator(File file) {
		if (file == null)
			throw new NullPointerException("File cannot be null!");
		this.file = file;
		this.writer = null;
	}
	
	public void open() throws FileNotFoundException {
		writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), ASCII));
	}
	
	public void setColumnNames(String ... names) throws IOException {
		for (int i = 0; i < names.length; i++) {
			writer.write(names[i]);
			if (i+1 < names.length)
				writer.write('\t');
		}
	}
	
	public void setColumnTypes(String ... types) throws IOException {
		writer.newLine();
		for (int i = 0; i < types.length; i++) {
			writer.write(types[i]);
			if (i+1 < types.length)
				writer.write('\t');
		}
	}
	
	public void writeLine(Object ... line) throws IOException {
		writer.newLine();
		for (int i = 0; i < line.length; i++) {
			writer.write(line[i].toString());
			if (i+1 < line.length)
				writer.write('\t');
		}
	}
	
	public void close() throws IOException {
		writer.close();
	}
	
}
