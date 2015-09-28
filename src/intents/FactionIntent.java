package intents;

import resources.PvpFaction;
import resources.control.Intent;
import resources.objects.tangible.TangibleObject;

public class FactionIntent extends Intent {

	public static final String TYPE = "FactionIntent";
	private TangibleObject target;
	private PvpFaction newFaction;
	private FactionIntentType updateType;
	
	private FactionIntent(TangibleObject target) {
		super(TYPE);
		this.target = target;
	}
	
	public FactionIntent(TangibleObject target, FactionIntentType updateType) {
		this(target);
		this.updateType = updateType;
	}
	
	public FactionIntent(TangibleObject target, PvpFaction newFaction) {
		this(target, FactionIntentType.FACTIONUPDATE);
		this.newFaction = newFaction;
	}
	
	public TangibleObject getTarget() {
		return target;
	}
	
	public PvpFaction getNewFaction() {
		return newFaction;
	}
	
	public FactionIntentType getUpdateType() {
		return updateType;
	}
	
	public enum FactionIntentType {
		FLAGUPDATE,
		STATUSUPDATE,
		FACTIONUPDATE // Is automatically set in the correct constructor, don't use manually.
	}
	
}