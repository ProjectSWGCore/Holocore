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
package com.projectswg.holocore.resources.support.global.zone.sui;

import com.projectswg.common.data.sui.ISuiCallback;
import com.projectswg.common.data.sui.SuiBaseWindow;
import com.projectswg.common.data.sui.SuiEvent;
import com.projectswg.holocore.intents.support.global.zone.sui.SuiWindowIntent;
import com.projectswg.holocore.resources.support.global.player.Player;

public class SuiWindow extends SuiBaseWindow {
	
	public SuiWindow() {
		
	}
	
	public SuiWindow(String script, SuiButtons buttons, String title, String prompt) {
		super(script);
		setTitle(title);
		setPrompt(prompt);
		setButtons(buttons);
	}

	public SuiWindow(String script, SuiButtons buttons, String title, String prompt, String callback, ISuiCallback suiCallback) {
		this(script, buttons, title, prompt);

		addOkButtonCallback(callback, suiCallback);
		addCancelButtonCallback(callback, suiCallback);
	}

	public SuiWindow(String script, String title, String prompt) {
		this(script, SuiButtons.DEFAULT, title, prompt);
	}

	public SuiWindow(String script) {
		super(script);
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
		setProperty("this", "Location", String.valueOf(x) + ',' + String.valueOf(y));
	}

	public void setSize(int width, int height) {
		setProperty("this", "Size", String.valueOf(width) + ',' + String.valueOf(height));
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

	public void addCancelButtonCallback(String callback, ISuiCallback suiCallback) {
		addCallback(SuiEvent.CANCEL_PRESSED, callback, suiCallback);
	}

	protected void addOtherButtonReturnable() {
		addReturnableProperty(SuiEvent.OK_PRESSED, "this", "otherPressed");
	}

	public final void display(Player player) {
		onDisplayRequest();
		new SuiWindowIntent(player, this, SuiWindowIntent.SuiWindowEvent.NEW).broadcast();
	}

	public final void close(Player player) {
		new SuiWindowIntent(player, this, SuiWindowIntent.SuiWindowEvent.CLOSE).broadcast();
	}

	// Add a simple ok/cancel button subscriptions if no callbacks is assigned so the server is sent a SuiEventNotification when client destroys the page.
	// Doing it this way will ensures the server removes the stored SuiWindow from memory.
	private void prepare() {
		if (hasSubscriptionComponent())
			return;

		subscribeToEvent(SuiEvent.OK_PRESSED.getValue(), "", "handleSUI");
		subscribeToEvent(SuiEvent.CANCEL_PRESSED.getValue(), "", "handleSUI");
	}

	protected void onDisplayRequest() {
		prepare();
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
