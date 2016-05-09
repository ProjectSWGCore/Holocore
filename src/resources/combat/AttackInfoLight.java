package resources.combat;

public class AttackInfoLight {
	
	private int damage;
	
	public AttackInfoLight() {
		this(0);
	}
	
	public AttackInfoLight(int damage) {
		setDamage(damage);
	}
	
	public int getDamage() {
		return damage;
	}
	
	public void setDamage(int damage) {
		this.damage = damage;
	}
	
}
