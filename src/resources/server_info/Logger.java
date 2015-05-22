package resources.server_info;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
	
	private final File file;
	private BufferedWriter writer;
	
	public Logger(String filename) {
		this.file = new File(filename);
	}
	
	public synchronized void open() throws IOException {
		writer = new BufferedWriter(new FileWriter(file));
	}
	
	public synchronized void close() throws IOException {
		writer.close();
	}
	
	public synchronized void write(String str) throws IOException {
		writer.write(str);
		writer.newLine();
		writer.flush();
	}
	
}
