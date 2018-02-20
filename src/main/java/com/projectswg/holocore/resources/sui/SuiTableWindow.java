/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/
package com.projectswg.holocore.resources.sui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SuiTableWindow extends SuiWindow {
	
	private List<SuiTableColumn> table;
	
	public SuiTableWindow(SuiButtons buttons, String title, String prompt, boolean exportBtn) {
		super("Script.tablePage", buttons, title, prompt);
		table = new ArrayList<>();
		
		if (prompt == null || prompt.isEmpty())
			setProperty("comp.Prompt", "Visible", "false");
		
		setShowExportButton(exportBtn);
		
		clearDataSource("comp.TablePage.dataTable");
		
	}
	
	@Override
	protected void onDisplayRequest() {
		addReturnableProperty("comp.TablePage.table", "SelectedRow");
	}
	
	@Override
	protected void setButtons(SuiButtons buttons) {
		switch(buttons) {
			case OK_CANCEL:
				setOkButtonText();
				setCancelButtonText();
				break;
			case OK_CANCEL_ALL:
				setOkButtonText();
				setCancelButtonText();
				setShowOtherButton(true, "@all");
				break;
			case OK_REFRESH:
				setOkButtonText();
				setCancelButtonText("@refresh");
				break;
			case OK_REFRESH_CANCEL:
				setOkButtonText();
				setCancelButtonText();
				setShowOtherButton(true, "@refresh");
				break;
			case YES_NO:
				setOkButtonText("@yes");
				setCancelButtonText("@no");
				break;
			case REFRESH:
				setOkButtonText("@refresh");
				setShowCancelButton(false);
				break;
			case REFRESH_CANCEL:
				setOkButtonText("@refresh");
				setCancelButtonText();
				break;
			case OK:
			default:
				setShowCancelButton(false);
				setOkButtonText();
				break;
		}
	}
	
	public void addColumn(String columnName, String type) {
		int index = table.size();
		
		SuiTableColumn column = new SuiTableColumn();
		String sIndex = String.valueOf(index);
		
		addDataSource("comp.TablePage.dataTable", "Name", sIndex);
		setProperty("comp.TablePage.dataTable." + sIndex, "Label", columnName);
		setProperty("comp.TablePage.dataTable." + sIndex, "Type", type);
		
		table.add(column);
	}
	
	public void addCell(String cellName, long cellObjId, int columnIndex) {
		SuiTableColumn column = table.get(columnIndex);
		
		if (column == null)
			return;
		
		int cellIndex = column.getCells().size();
		
		SuiTableCell cell = new SuiTableCell(cellName, cellObjId);
		column.getCells().add(cell);
		
		addDataItem("comp.TablePage.dataTable." + columnIndex, "Name", "data" + cellIndex);
		setProperty("comp.TablePage.dataTable." + columnIndex + ".data" + cellIndex, "Value", cellName);
	}
	
	public void addCell(String cellName, int columnIndex) {
		addCell(cellName, 0, columnIndex);
	}
	
	public void setScrollExtent(int x, int z) {
		setProperty("comp.TablePage.header", "ScrollExtent", String.valueOf(x) + "," + String.valueOf(z));
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
	
	private void setShowExportButton(boolean show) {
		setProperty("btnExport", "Visible", String.valueOf(show));
	}
	
	public static int getSelectedRow(Map<String, String> parameters) {
		String selectedIndex = parameters.get("comp.TablePage.table.SelectedRow");
		if (selectedIndex != null) return Integer.parseInt(selectedIndex);
		return -1;
	}
	
	private static class SuiTableColumn {
		
		private List<SuiTableCell> cells;
		
		public SuiTableColumn(List<SuiTableCell> cells) {
			this.cells = cells;
		}
		
		public SuiTableColumn() {
			this(new ArrayList<>());
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
