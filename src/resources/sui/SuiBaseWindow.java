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

import resources.player.Player;

public class SuiBaseWindow extends SuiWindow {

	public SuiBaseWindow(String script, Player owner, SuiButtons buttons, String title, String prompt) {
		super(script, owner);
		setTitle(title);
		setPrompt(prompt);
		setButtons(buttons);
	}

	public SuiBaseWindow(String script, Player owner, SuiButtons buttons, String title, String prompt, String callbackScript, String function) {
		this(script, owner, buttons, title, prompt);

		addOkButtonCallback(callbackScript, function);
		addCancelButtonCallback(callbackScript, function);
	}

	public SuiBaseWindow(String script, Player owner, SuiButtons buttons, String title, String prompt, String callback, ISuiCallback suiCallback) {
		this(script, owner, buttons, title, prompt);

		addOkButtonCallback(callback, suiCallback);
		addCancelButtonCallback(callback, suiCallback);
	}

	public SuiBaseWindow(String script, Player owner, String title, String prompt) {
		this(script, owner, SuiButtons.DEFAULT, title, prompt);
	}

	public void setTitle(String title) {
		if (title != null)
			setPropertyText("bg.caption.lblTitle", title);
	}
	
	public void setPrompt(String prompt) {
		if (prompt != null)
			setPropertyText("Prompt.lblPrompt", prompt);
	}

	public void setPropertyText(String widget, String value) {
		setProperty(widget, "Text", value);
	}

	public void setAutosave(boolean autosave) {
		setProperty("this", "autosave", String.valueOf(autosave));
	}

	public void setLocation(int x, int y) {
		setProperty("this", "Location", String.valueOf(x) + "," + String.valueOf(y));
	}

	public void setSize(int width, int height) {
		setProperty("this", "Size", String.valueOf(width) + "," + String.valueOf(height));
	}

	protected void setOkButtonText() {
		setOkButtonText("@ok");
	}

	protected void setOkButtonText(String text) {
		setProperty("btnOk", "Text", text);
	}

	protected void setCancelButtonText() {
		setCancelButtonText("@cancel");
	}

	protected void setCancelButtonText(String text) {
		setProperty("btnCancel", "Text", text);
	}

	protected void setShowOtherButton(boolean display, String text) {
		setProperty("btnOther", "Visible", String.valueOf(display));
		if (display) {
			setProperty("btnOther", "Text", text);
			addOtherButtonReturnable();
		}
	}

	protected void setShowCancelButton(boolean display) {
		String value = String.valueOf(display);
		setProperty("btnCancel", "Enabled", value);
		setProperty("btnCancel", "Visible", value);
	}

	public void addOkButtonCallback(String callback, ISuiCallback suiCallback) {
		addCallback(SuiEvent.OK_PRESSED, callback, suiCallback);
	}

	public void addOkButtonCallback(String script, String function) {
		addCallback(SuiEvent.OK_PRESSED, script, function);
	}

	public void addCancelButtonCallback(String callback, ISuiCallback suiCallback) {
		addCallback(SuiEvent.CANCEL_PRESSED, callback, suiCallback);
	}

	public void addCancelButtonCallback(String script, String function) {
		addCallback(SuiEvent.CANCEL_PRESSED, script, function);
	}

	public void addOtherButtonReturnable() {
		addReturnableProperty(SuiEvent.OK_PRESSED, "this", "otherPressed");
	}

	protected void setButtons(SuiButtons buttons) {
		switch(buttons) {
			case OK_CANCEL:
				setOkButtonText();
				setCancelButtonText();
				break;
			case OK:
			default:
				setOkButtonText();
				break;
		}
	}
}
