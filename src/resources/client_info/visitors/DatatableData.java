package resources.client_info.visitors;

import java.nio.ByteBuffer;

import utilities.ByteUtilities;

public class DatatableData extends ClientData {
	
	private String[] columnNames;
	private String[] columnTypes;
	private Object[][] table;
	//private Map<String, Integer> enums;
	
	@Override
	public void handleData(String node, ByteBuffer data, int size) {

		switch(node) {
		
		case "0001COLS":
			columnNames = new String[data.getInt()];
			
			for (int i = 0; i < columnNames.length; i++) {
				columnNames[i] = ByteUtilities.nextString(data);
				data.get(); // empty separator byte
			}
			break;
			
		case "TYPE":
			parseTypes(data);
			break;
			
		case "ROWS":
			parseRows(data);
		}
	}

	private void parseTypes(ByteBuffer data) {
		columnTypes = new String[columnNames.length];

		for (int t = 0; t < columnTypes.length; t++) {
			String type = ByteUtilities.nextString(data);
			
			if (type.contains("["))
				type = type.split("\\[")[0];
			
			if (type.contains("(")) // relevant only to enums
				type = type.split("\\(")[0];
			
			columnTypes[t] = type;
			data.get(); // empty separator byte
			
/* TODO: Need to come up with a better way of doing enums. Example of what this type looks like:
* e(RIFLE=0,CARBINE=1,PISTOL=2,HEAVY=3,1HAND_MELEE=4,2HAND_MELEE=5,UNARMED=6,POLEARM=7,THROWN=8,1HAND_LIGHTSABER=9,2HAND_LIGHTSABER=10,POLEARM_LIGHTSABER=11)
			if (type.startsWith("e(")) {
				// Cleanup string and put enum inside the enums map
				String enumEntries = type;
				enumEntries = type.replace("e(", "").replace(")", "");
			}
*/

		}
	}
	
	private void parseRows(ByteBuffer data) {
		int rows = data.getInt();
		table = new Object[rows][columnTypes.length];
		
		for (int r = 0; r < rows; r++) {
			// Cell within row
			for (int t = 0; t < columnTypes.length; t++) {
				String type = columnTypes[t];
				
				switch(type) {
				
				case "b": // Boolean
					table[r][t] = (data.getInt() == 1);
					break;
					
				case "e": // Enumerator
					table[r][t] = data.getInt();
					break;
					
				case "f": // Float
					table[r][t] = data.getFloat();
					break;
				
				case "h": // CRC
					table[r][t] = data.getInt();
					break;
					
				case "i": // Integer
					table[r][t] = data.getInt();
					break;
				
				case "s": // String
					if (data.get() != 0) {
						data.position(data.position() - 1);
						table[r][t] = ByteUtilities.nextString(data);
						data.get();
					} else {
						table[r][t] = "";
					}
					
					break;
					
				case "v":
					// TODO: Datatable Enums
					data.getInt();
					break;
				default:
					System.err.println("FATAL: Don't know how to decode type " + type + " in row " + r + " column " + t);
					break;
				}
			}
		}
	}
	
	public int getRowCount() {
		if(table == null)
			return 0;
		return table.length;
	}
	
	public int getColumnCount() {
		if(table == null)
			return 0;
		return columnNames.length;
	}
	
	public Object[] getRowsByColumnName(String columnName) {
		
		for(int i = 0; i < table.length; i++) {
			for(int j=0; j < columnTypes.length; ++j) {
				String currentName = columnNames[j];
				if(currentName.equals(columnName))
					return table[i];
			}
		}
		return null;
	}
	
	public Object [] getRow(int row) {
		return table[row];
	}
	
	public Object getCell(int row, int column) {
		return table[row][column];
	}
	
	public Object getCell(String columnName, int rowIndex) {
		for(int j=0; j < columnTypes.length; ++j) {
			String currentName = columnNames[j];
			if(currentName.equals(columnName))
				return table[rowIndex][j];
		}
		return null;
	}
	
	public String getColumnName(int column) {
		if (column < 0 || column >= columnNames.length)
			return null;
		return columnNames[column];
	}
	
	public String getColumnType(int column) {
		if (column < 0 || column >= columnTypes.length)
			return null;
		return columnTypes[column];
	}

}
