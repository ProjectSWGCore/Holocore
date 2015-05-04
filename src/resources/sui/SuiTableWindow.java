/***********************************************************************************
* Copyright (c) 2015 /// Project SWG /// www.projectswg.com                        *
*                                                                                  *
* ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on           *
* July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies.  *
* Our goal is to create an emulator which will provide a server for players to     *
* continue playing a game similar to the one they used to play. We are basing      *
* it on the final publish of the game prior to end-game events.                    *
*                                                                                  *
* This file is part of Holocore.                                                   *
*                                                                                  *
* -------------------------------------------------------------------------------- *
*                                                                                  *
* Holocore is free software: you can redistribute it and/or modify                 *
* it under the terms of the GNU Affero General Public License as                   *
* published by the Free Software Foundation, either version 3 of the               *
* License, or (at your option) any later version.                                  *
*                                                                                  *
* Holocore is distributed in the hope that it will be useful,                      *
* but WITHOUT ANY WARRANTY; without even the implied warranty of                   *
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                    *
* GNU Affero General Public License for more details.                              *
*                                                                                  *
* You should have received a copy of the GNU Affero General Public License         *
* along with Holocore.  If not, see <http://www.gnu.org/licenses/>.                *
*                                                                                  *
***********************************************************************************/
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
