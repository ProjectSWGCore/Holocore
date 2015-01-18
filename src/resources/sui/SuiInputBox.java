package resources.sui;

import java.util.ArrayList;
import java.util.List;

import resources.player.Player;

public class SuiInputBox extends SuiBaseWindow {

	public SuiInputBox(Player owner, InputBoxType type, String title, String prompt) {
		super("Script.inputBox", owner, title, prompt);
		switch(type){
		case OK:
			setProperty("btnOk:visible", "True");
			setProperty("btnOk:Text", "@ok");
			setProperty("btnCancel:visible", "False");
			setProperty("cmbInput:visible", "False");
			break;
		case OK_CANCEL:
			setProperty("btnOk:visible", "True");
			setProperty("btnCancel:visible", "True");
			setProperty("btnCancel:Text", "@cancel");
			setProperty("btnOk:Text", "@ok");
			setProperty("cmbInput:visible", "False");
			break;
		default: break;
		}
	}

	public SuiInputBox(Player owner, String title, String prompt) {
		this(owner, InputBoxType.OK_CANCEL, title, prompt);
	}
	
	public void addInputTextCallback(int eventType, ISuiCallback callback) {
		List<String> returnParams = new ArrayList<String>();
		returnParams.add("txtInput:LocalText");
		addCallback(eventType, "", Trigger.OK, returnParams, callback);
	}
	
	public void allowStringCharacters(boolean stringCharacters) {
		setProperty("txtInput:NumericInteger", String.valueOf(stringCharacters));
	}
	
	public void setMaxInputLength(int maxLength) {
		setProperty("txtInput:MaxLength", String.valueOf(maxLength));
	}
	
	public enum InputBoxType {
		OK,
		OK_CANCEL;
	}
}
