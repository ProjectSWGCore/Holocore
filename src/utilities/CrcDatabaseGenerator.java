package utilities;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import network.PacketType;
import resources.client_info.ClientFactory;
import resources.client_info.visitors.CrcStringTableData;
import resources.client_info.visitors.DatatableData;
import resources.server_info.CrcDatabase;

public class CrcDatabaseGenerator {
	
	public static void main(String [] args) {
		generate(true);
	}
	
	public static void generate(boolean output) {
		try (CrcDatabase database = new CrcDatabase()) {
			long start = System.nanoTime();
			database.loadStrings();
			if (output)
				System.out.printf("Loading CrcStringTableData... [%.3f]%n", (System.nanoTime()-start)/1E6);
			addStringTable(database);
			if (output)
				System.out.printf("Loading BuffTable...          [%.3f]%n", (System.nanoTime()-start)/1E6);
			addBuffTable(database);
			if (output)
				System.out.printf("Loading Clientdata...         [%.3f]%n", (System.nanoTime()-start)/1E6);
			addClientdata(database);
			if (output)
				System.out.printf("Loading Packets...            [%.3f]%n", (System.nanoTime()-start)/1E6);
			addPackets(database);
			if (output)
				System.out.printf("Loading Commands...           [%.3f]%n", (System.nanoTime()-start)/1E6);
			addCommands(database);
			database.commitBatch();
		}
	}
	
	private static void addStringTable(CrcDatabase database) {
		CrcStringTableData table = (CrcStringTableData) ClientFactory.getInfoFromFile("misc/object_template_crc_string_table.iff");
		for (String string : table.getStrings()) {
			database.addCrcBatch(string);
		}
	}
	
	private static void addBuffTable(CrcDatabase database) {
		DatatableData data = (DatatableData) ClientFactory.getInfoFromFile("datatables/buff/buff.iff");
		for (int row = 0; row < data.getRowCount(); row++) {
			database.addCrcBatch((String) data.getCell(row, 0));
		}
	}
	
	private static void addClientdata(CrcDatabase database) {
		try {
			Files.walkFileTree(new File("clientdata"+File.separator+"object").toPath(), new SimpleFileVisitor<Path>() {
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
					String template = file.toFile().getAbsolutePath();
					database.addCrcBatch(template.substring(template.indexOf("object"+File.separator)));
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void addPackets(CrcDatabase database) {
		for (PacketType type : PacketType.values()) {
			database.addCrcBatch(type.getSwgClass().getSimpleName());
		}
	}
	
	private static void addCommands(CrcDatabase database) {
		addCommands(database, "command_table");
		addCommands(database, "command_table_ground");
		addCommands(database, "client_command_table");
	}
	
	private static void addCommands(CrcDatabase database, String table) {
		DatatableData baseCommands = (DatatableData) ClientFactory.getInfoFromFile("datatables/command/"+table+".iff");
		for (int row = 0; row < baseCommands.getRowCount(); row++) {
			database.addCrcBatch((String) baseCommands.getCell(row, 0));
		}
	}
	
}
