package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiTableWindow extends SuiBaseWindow {

	private List<SuiTableColumn> table;
	
	public SuiTableWindow(Player owner, String title, String prompt) {
		super("Script.tablePage", owner, title, prompt);
		table = new ArrayList<SuiTableColumn>();
		
		if (prompt == null)
			setProperty("comp.Prompt:Visible", "false");
	}

	public void addColumn(String columnName, String type) {
		int index = table.size();
		
		SuiTableColumn column = new SuiTableColumn();
        addTableDataSource("comp.TablePage.dataTable:Name", String.valueOf(index));
        setProperty("comp.TablePage.dataTable." + index + ":Label", columnName);
        setProperty("comp.TablePage.dataTable." + index + ":Type", type);
        
        table.add(column);
	}
	
	public void addCell(String cellName, long cellObjId, int columnIndex) {
		SuiTableColumn column = table.get(columnIndex);
		
		if (column == null)
			return;
		
		int cellIndex = column.getCells().size();
		
		SuiTableCell cell = new SuiTableCell(cellName, cellObjId);
		column.getCells().add(cell);
		
		addDataItem("comp.TablePage.dataTable." + columnIndex + ":Name", "data" + cellIndex);
		setProperty("comp.TablePage.dataTable." + columnIndex + ".data" + cellIndex + ":Value", cellName);
	}
	
	public void addCell(String cellName, int columnIndex) {
		addCell(cellName, 0, columnIndex);
	}
	
	public void showExportButton(boolean show) {
		setProperty("btnExport:Visible", String.valueOf(show));
	}
	
	public void setScrollExtent(int x, int z) {
		setProperty("comp.TablePage.header:ScrollExtent", String.valueOf(x) + "," + String.valueOf(z));
	}
	
	public long getCellId(int column, int row) {
		SuiTableCell cell = table.get(column).getCells().get(row);
		
		if (cell == null) return 0;
		else return cell.getId();
	}
	
	public String getCellValue(int column, int row) {
		SuiTableCell cell = table.get(column).getCells().get(row);
		
		if (cell == null) return null;
		else return cell.getValue();
	}
	
	private static class SuiTableColumn {
		
		private List<SuiTableCell> cells;

		public SuiTableColumn(List<SuiTableCell> cells) {
			this.cells = cells;
		}
		
		public SuiTableColumn() {
			this(new ArrayList<SuiTableCell>());
		}
		
		public List<SuiTableCell> getCells() { return this.cells; }
	}
	
	private static class SuiTableCell {
		private String value;
		private long id;
		
		public SuiTableCell(String value, long id) {
			this.value = value;
			this.id = id;
		}

		public String getValue() { return this.value; }
		public long getId() { return this.id; }
	}
}
