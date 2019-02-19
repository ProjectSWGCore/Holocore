package com.projectswg.holocore.services.support.global.zone;

import com.projectswg.holocore.intents.support.global.zone.PlayerEventIntent;
import com.projectswg.holocore.intents.support.objects.swg.DestroyObjectIntent;
import com.projectswg.holocore.intents.support.objects.swg.ObjectCreatedIntent;
import com.projectswg.holocore.resources.support.global.player.Player;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import com.projectswg.holocore.resources.support.objects.swg.creature.CreatureObject;
import me.joshlarson.jlcommon.control.IntentHandler;
import me.joshlarson.jlcommon.control.Service;
import me.joshlarson.jlcommon.utilities.Arguments;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

public class CharacterLookupService extends Service {
	
	private final Map<String, CreatureObject> charactersByFullName;
	private final Map<String, CreatureObject> charactersByFirstName;
	private final Set<CreatureObject> loggedInPlayers;
	private final Set<CreatureObject> loggedInPlayersView;
	
	public CharacterLookupService() {
		this.charactersByFullName = new ConcurrentHashMap<>();
		this.charactersByFirstName = new ConcurrentHashMap<>();
		this.loggedInPlayers = new CopyOnWriteArraySet<>();
		this.loggedInPlayersView = Collections.unmodifiableSet(loggedInPlayers);
	}
	
	@Override
	public boolean initialize() {
		PlayerLookup.setAuthority(this);
		return super.initialize();
	}
	
	@Override
	public boolean terminate() {
		PlayerLookup.setAuthority(null);
		return super.terminate();
	}
	
	@IntentHandler
	private void handlePlayerEventIntent(PlayerEventIntent pei) {
		switch (pei.getEvent()) {
			case PE_FIRST_ZONE:
				loggedInPlayers.add(pei.getPlayer().getCreatureObject());
				break;
			case PE_LOGGED_OUT:
				loggedInPlayers.remove(pei.getPlayer().getCreatureObject());
				break;
			default:
				break;
		}
	}
	
	private Collection<CreatureObject> getLoggedInCharacters() {
		return loggedInPlayersView;
	}
	
	private Player getPlayerByFullName(@NotNull String name) {
		CreatureObject creature = getCharacterByFullName(name);
		if (creature == null)
			return null;
		return creature.getOwner();
	}
	
	private Player getPlayerByFirstName(@NotNull String name) {
		CreatureObject creature = getCharacterByFirstName(name);
		if (creature == null)
			return null;
		return creature.getOwner();
	}
	
	private CreatureObject getCharacterByFullName(@NotNull String name) {
		Objects.requireNonNull(name, "name");
		name = name.trim().toLowerCase(Locale.ENGLISH);
		Arguments.validate(!name.isEmpty(), "name cannot be empty");
		return charactersByFullName.get(name);
	}
	
	private CreatureObject getCharacterByFirstName(@NotNull String name) {
		Objects.requireNonNull(name, "name");
		name = name.trim().toLowerCase(Locale.ENGLISH);
		Arguments.validate(!name.isEmpty(), "name cannot be empty");
		return charactersByFirstName.get(name);
	}
	
	@IntentHandler
	private void handleObjectCreatedIntent(ObjectCreatedIntent oci) {
		SWGObject obj = oci.getObject();
		if (!(obj instanceof CreatureObject))
			return;
		
		CreatureObject creature = (CreatureObject) obj;
		if (!creature.isPlayer())
			return;
		
		String name = creature.getObjectName();
		charactersByFullName.put(name, creature);
		if (name.indexOf(' ') != -1)
			name = name.substring(0, name.indexOf(' '));
		name = name.toLowerCase(Locale.US);
		charactersByFirstName.put(name, creature);
	}
	
	@IntentHandler
	private void handleDestroyObjectIntent(DestroyObjectIntent doi) {
		SWGObject obj = doi.getObject();
		if (!(obj instanceof CreatureObject))
			return;
		
		CreatureObject creature = (CreatureObject) obj;
		if (!creature.isPlayer())
			return;
		
		String name = creature.getObjectName();
		charactersByFullName.remove(name);
		if (name.indexOf(' ') != -1)
			name = name.substring(0, name.indexOf(' '));
		name = name.toLowerCase(Locale.US);
		charactersByFirstName.remove(name);
	}
	
	public static class PlayerLookup {
		
		private static final AtomicReference<CharacterLookupService> AUTHORITY = new AtomicReference<>(null);
		
		private static void setAuthority(CharacterLookupService authority) {
			AUTHORITY.set(authority);
		}
		
		public static boolean doesCharacterExistByFullName(String name) {
			return getCharacterByFullName(name) != null;
		}
		
		public static boolean doesCharacterExistByFirstName(String name) {
			return getCharacterByFirstName(name) != null;
		}
		
		public static Player getPlayerByFullName(String name) {
			return AUTHORITY.get().getPlayerByFullName(name);
		}
		
		public static Player getPlayerByFirstName(String name) {
			return AUTHORITY.get().getPlayerByFirstName(name);
		}
		
		public static CreatureObject getCharacterByFullName(String name) {
			return AUTHORITY.get().getCharacterByFullName(name);
		}
		
		public static CreatureObject getCharacterByFirstName(String name) {
			return AUTHORITY.get().getCharacterByFirstName(name);
		}
		
		public static Collection<CreatureObject> getLoggedInCharacters() {
			return AUTHORITY.get().getLoggedInCharacters();
		}
		
	}
	
}
