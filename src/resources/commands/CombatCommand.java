package resources.commands;

import resources.combat.AttackType;
import resources.combat.DamageType;
import resources.combat.ValidTarget;

public class CombatCommand extends Command {
	
	private ValidTarget validTarget;
	private boolean forceCombat;
	private AttackType attackType;
	private double healthCost;
	private double actionCost;
	private DamageType damageType;
	private boolean ignoreDistance;
	private boolean pvpOnly;
	private int attackRolls;
	
	public CombatCommand(String name) {
		super(name);
	}
	
	public ValidTarget getValidTarget() {
		return validTarget;
	}
	
	public boolean isForceCombat() {
		return forceCombat;
	}
	
	public AttackType getAttackType() {
		return attackType;
	}
	
	public double getHealthCost() {
		return healthCost;
	}
	
	public double getActionCost() {
		return actionCost;
	}
	
	public DamageType getDamageType() {
		return damageType;
	}
	
	public boolean isIgnoreDistance() {
		return ignoreDistance;
	}
	
	public boolean isPvpOnly() {
		return pvpOnly;
	}
	
	public int getAttackRolls() {
		return attackRolls;
	}
	
	public void setValidTarget(ValidTarget validTarget) {
		this.validTarget = validTarget;
	}
	
	public void setForceCombat(boolean forceCombat) {
		this.forceCombat = forceCombat;
	}
	
	public void setAttackType(AttackType attackType) {
		this.attackType = attackType;
	}
	
	public void setHealthCost(double healthCost) {
		this.healthCost = healthCost;
	}
	
	public void setActionCost(double actionCost) {
		this.actionCost = actionCost;
	}
	
	public void setDamageType(DamageType damageType) {
		this.damageType = damageType;
	}
	
	public void setIgnoreDistance(boolean ignoreDistance) {
		this.ignoreDistance = ignoreDistance;
	}
	
	public void setPvpOnly(boolean pvpOnly) {
		this.pvpOnly = pvpOnly;
	}
	
	public void setAttackRolls(int attackRolls) {
		this.attackRolls = attackRolls;
	}
	
}
