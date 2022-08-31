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
import org.jetbrains.annotations.Nullable;

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
		PlayerLookup.setAuthority(this);
	}
	
	@Override
	public boolean initialize() {
		PlayerLookup.setAuthority(this);
		return true;
	}
	
	@Override
	public boolean terminate() {
		PlayerLookup.setAuthority(null);
		return true;
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
	
	@Nullable
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
		
		@Nullable
		public static Player getPlayerByFullName(String name) {
			CharacterLookupService characterLookupService = AUTHORITY.get();
			
			if (characterLookupService == null) {
				return null;
			}
			
			return characterLookupService.getPlayerByFullName(name);
		}
		
		@Nullable
		public static Player getPlayerByFirstName(String name) {
			CharacterLookupService characterLookupService = AUTHORITY.get();
			
			if (characterLookupService == null) {
				return null;
			}
			
			return characterLookupService.getPlayerByFirstName(name);
		}
		
		@Nullable
		public static CreatureObject getCharacterByFullName(String name) {
			CharacterLookupService characterLookupService = AUTHORITY.get();
			
			if (characterLookupService == null) {
				return null;
			}
			
			return characterLookupService.getCharacterByFullName(name);
		}
		
		@Nullable
		public static CreatureObject getCharacterByFirstName(String name) {
			CharacterLookupService characterLookupService = AUTHORITY.get();
			
			if (characterLookupService == null) {
				return null;
			}
			
			return characterLookupService.getCharacterByFirstName(name);
		}
		
		@Nullable
		public static Collection<CreatureObject> getLoggedInCharacters() {
			CharacterLookupService characterLookupService = AUTHORITY.get();
			
			if (characterLookupService == null) {
				return null;
			}
			
			return characterLookupService.getLoggedInCharacters();
		}
		
	}
	
}
