/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package intents;

import resources.control.Intent;
import resources.objects.creature.CreatureObject;

/**
 *
 * @author Mads
 */
public class DanceIntent extends Intent {
	public static final String TYPE = "DanceIntent";
	private final String danceName;
	private final CreatureObject creatureObject;
	
	/**
	 * Start dancing
	 * @param danceName
	 * @param creatureObject 
	 */
	public DanceIntent(String danceName, CreatureObject creatureObject) {
		super(TYPE);
		this.danceName = danceName;
		this.creatureObject = creatureObject;
	}
	
	/**
	 * Stop dancing
	 * @param creatureObject 
	 */
	public DanceIntent(CreatureObject creatureObject) {
		this(null, creatureObject);
	}

	public String getDanceName() {
		return danceName;
	}

	public CreatureObject getCreatureObject() {
		return creatureObject;
	}
	
	public boolean isStartDance() {
		return danceName != null;
	}
	
}
