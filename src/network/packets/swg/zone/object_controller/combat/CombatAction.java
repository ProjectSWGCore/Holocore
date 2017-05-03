/************************************************************************************
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
package network.packets.swg.zone.object_controller.combat;

import java.util.HashSet;
import java.util.Set;

import com.projectswg.common.data.location.Point3D;
import com.projectswg.common.network.NetBuffer;

import network.packets.swg.zone.object_controller.ObjectController;
import resources.Posture;
import resources.combat.HitLocation;
import resources.combat.TrailLocation;
import resources.objects.creature.CreatureObject;

public class CombatAction extends ObjectController {
	
	public static final int CRC = 0x00CC;
	
	private int actionCrc;
	private long attackerId;
	private long weaponId;
	private Posture posture;
	private Point3D position;
	private long cell;
	private TrailLocation trail;
	private byte clientEffectId;
	private int commandCrc;
	private boolean useLocation;
	private Set<Defender> defenders;
	
	public CombatAction(long objectId) {
		super(objectId, CRC);
		defenders = new HashSet<>();
	}
	
	public CombatAction(NetBuffer data) {
		super(CRC);
		defenders = new HashSet<>();
		decode(data);
	}
	
	@Override
	public void decode(NetBuffer data) {
		decodeHeader(data);
		actionCrc = data.getInt();
		attackerId = data.getLong();
		weaponId = data.getLong();
		posture = Posture.getFromId(data.getByte());
		trail = TrailLocation.getTrailLocation(data.getByte());
		clientEffectId = data.getByte();
		commandCrc = data.getInt();
		useLocation = data.getBoolean();
		if (useLocation) {
			position = data.getEncodable(Point3D.class);
			cell = data.getLong();
		}
		int count = data.getShort();
		for (int i = 0; i < count; i++) {
			Defender d = new Defender();
			d.setCreatureId(data.getLong());
			d.setPosture(Posture.getFromId(data.getByte()));
			d.setDefense(data.getBoolean());
			d.setClientEffectId(data.getByte());
			d.setHitLocation(HitLocation.getHitLocation(data.getByte()));
			d.setDamage(data.getShort());
			defenders.add(d);
		}
	}
	
	@Override
	public NetBuffer encode() {
		NetBuffer data = NetBuffer.allocate(HEADER_LENGTH + 30 + defenders.size() * 14 + (useLocation ? 20 : 0));
		encodeHeader(data);
		data.addInt(actionCrc);
		data.addLong(attackerId);
		data.addLong(weaponId);
		data.addByte(posture.getId());
		data.addByte(trail.getNum());
		data.addByte(clientEffectId);
		data.addInt(commandCrc);
		data.addBoolean(useLocation);
		if (useLocation) {
			data.addEncodable(position);
			data.addLong(cell);
		}
		data.addShort(defenders.size());
		for (Defender d : defenders) {
			data.addLong(d.getCreatureId());
			data.addByte(d.getPosture().getId());
			data.addBoolean(d.isDefense());
			data.addByte(d.getClientEffectId());
			data.addByte(d.getHitLocation().getNum());
			data.addShort(d.getDamage());
		}
		return data;
	}
	
	public int getActionCrc() {
		return actionCrc;
	}
	
	public long getAttackerId() {
		return attackerId;
	}
	
	public long getWeaponId() {
		return weaponId;
	}
	
	public Posture getPosture() {
		return posture;
	}
	
	public Point3D getPosition() {
		return position;
	}
	
	public long getCell() {
		return cell;
	}
	
	public TrailLocation getTrail() {
		return trail;
	}
	
	public byte getClientEffectId() {
		return clientEffectId;
	}
	
	public int getCommandCrc() {
		return commandCrc;
	}
	
	public boolean isUseLocation() {
		return useLocation;
	}
	
	public Set<Defender> getDefenders() {
		return defenders;
	}
	
	public void setActionCrc(int actionCrc) {
		this.actionCrc = actionCrc;
	}
	
	public void setAttacker(CreatureObject attacker) {
		setAttackerId(attacker.getObjectId());
		setWeaponId(attacker.getEquippedWeapon() == null ? 0 : attacker.getEquippedWeapon().getObjectId());
		setPosture(attacker.getPosture());
	}
	
	public void setAttackerId(long attackerId) {
		this.attackerId = attackerId;
	}
	
	public void setWeaponId(long weaponId) {
		this.weaponId = weaponId;
	}
	
	public void setPosture(Posture posture) {
		this.posture = posture;
	}
	
	public void setPosition(Point3D position) {
		this.position = position;
	}
	
	public void setCell(long cell) {
		this.cell = cell;
	}
	
	public void setTrail(TrailLocation trail) {
		this.trail = trail;
	}
	
	public void setClientEffectId(byte clientEffectId) {
		this.clientEffectId = clientEffectId;
	}
	
	public void setCommandCrc(int commandCrc) {
		this.commandCrc = commandCrc;
	}
	
	public void setUseLocation(boolean useLocation) {
		this.useLocation = useLocation;
	}
	
	public void addDefender(CreatureObject creature, boolean defense, byte clientEffectId, HitLocation location, short damage) {
		defenders.add(new Defender(creature.getObjectId(), creature.getPosture(), defense, clientEffectId, location, damage));
	}
	
	public static class Defender {
		
		private long creatureId;
		private Posture posture;
		private boolean defense;
		private byte clientEffectId;
		private HitLocation hitLocation;
		private short damage;
		
		public Defender() {
			this(0, Posture.UPRIGHT, false, (byte) 0, HitLocation.HIT_LOCATION_BODY, (short) 0);
		}
		
		public Defender(long creatureId, Posture p, boolean defense, byte clientEffectId, HitLocation location, short damage) {
			setCreatureId(creatureId);
			setPosture(p);
			setDefense(defense);
			setClientEffectId(clientEffectId);
			setHitLocation(location);
			setDamage(damage);
		}
		
		public long getCreatureId() {
			return creatureId;
		}
		
		public Posture getPosture() {
			return posture;
		}
		
		public boolean isDefense() {
			return defense;
		}
		
		public byte getClientEffectId() {
			return clientEffectId;
		}
		
		public HitLocation getHitLocation() {
			return hitLocation;
		}
		
		public short getDamage() {
			return damage;
		}
		
		public void setCreatureId(long creatureId) {
			this.creatureId = creatureId;
		}
		
		public void setPosture(Posture p) {
			this.posture = p;
		}
		
		public void setDefense(boolean defense) {
			this.defense = defense;
		}
		
		public void setClientEffectId(byte clientEffectId) {
			this.clientEffectId = clientEffectId;
		}
		
		public void setHitLocation(HitLocation hitLocation) {
			this.hitLocation = hitLocation;
		}
		
		public void setDamage(short damage) {
			this.damage = damage;
		}
		
		public int hashCode() {
			return Long.hashCode(creatureId);
		}
		
		public boolean equals(Defender d) {
			return creatureId == d.getCreatureId();
		}
		
		public String toString() {
			return String.format("CREO=%d:%s  Defense=%b  EffectId=%d  HitLoc=%s  Damage=%d", creatureId, posture, defense, clientEffectId, hitLocation, damage);
		}
	}

}

