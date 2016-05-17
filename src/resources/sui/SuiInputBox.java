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

import java.util.Map;

public class SuiInputBox extends SuiWindow {

	public SuiInputBox(SuiButtons buttons, String title, String prompt) {
		super("Script.inputBox", buttons, title, prompt);
	}

	public SuiInputBox(String title, String prompt) {
		this(SuiButtons.OK_CANCEL, title, prompt);
	}

	public static String getEnteredText(Map<String, String> parameters) {
		String input = parameters.get("txtInput.LocalText");
		return input != null ? input : "";
	}

	public static String getSelectedComboText(Map<String, String> parameters) {
		return parameters.get("cmbInput.SelectedText");
	}

	@Override
	protected void setButtons(SuiButtons buttons) {
		switch(buttons) {
			case OK_CANCEL:
				setProperty("btnOk", "Visible", "True");
				setProperty("btnCancel", "Visible", "True");
				setCancelButtonText("@cancel");
				setOkButtonText("@ok");
				displayComboBox(false);
				break;
			case COMBO_OK:
				setProperty("btnOk", "Visible", "True");
				setProperty("btnCancel", "Visible", "False");
				setOkButtonText("@ok");
				displayComboBox(true);
				break;
			case COMBO_OK_CANCEL:
				setProperty("btnOk", "Visible", "True");
				setProperty("btnCancel", "Visible", "True");
				setOkButtonText("@ok");
				setCancelButtonText("@cancel");
				displayComboBox(true);
				break;
			case OK:
			default:
				setProperty("btnOk", "Visible", "True");
				setProperty("btnCancel", "Visible", "False");
				setOkButtonText("@ok");
				displayComboBox(false);
				break;
		}
	}

	@Override
	protected void onDisplayRequest() {
		addReturnableProperty("txtInput", "LocalText");
		addReturnableProperty("cmbInput", "SelectedText");
		addReturnableProperty("txtInput", "MaxLength");
	}

	private void displayComboBox(boolean show) {
		if (show) {
			setProperty("cmbInput", "Visible", "True");
			setProperty("cmbInput", "Visible", "True");

			setProperty("txtInput", "Enabled", "False");
			setProperty("txtInput", "Visible", "False");
		} else {
			setProperty("cmbInput", "Visible", "False");
			setProperty("cmbInput", "Visible", "False");

			setProperty("txtInput", "Enabled", "True");
			setProperty("txtInput", "Visible", "True");
		}
	}

	public void allowStringCharacters(boolean stringCharacters) {
		setProperty("txtInput", "NumericInteger", String.valueOf(stringCharacters));
	}

	public void setMaxLength(int maxLength) {
		setProperty("txtInput", "MaxLength", String.valueOf(maxLength));
	}
}
