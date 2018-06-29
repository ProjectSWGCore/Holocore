package com.projectswg.holocore.integration.resources;

import com.projectswg.common.data.CRC;
import com.projectswg.common.data.location.Location;
import com.projectswg.common.data.location.Terrain;
import com.projectswg.common.network.NetBuffer;
import com.projectswg.common.network.packets.PacketType;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.common.network.packets.swg.holo.login.HoloLoginRequestPacket;
import com.projectswg.common.network.packets.swg.holo.login.HoloLoginResponsePacket;
import com.projectswg.common.network.packets.swg.login.EnumerateCharacterId.SWGCharacter;
import com.projectswg.common.network.packets.swg.zone.CmdSceneReady;
import com.projectswg.common.network.packets.swg.zone.SceneCreateObjectByCrc;
import com.projectswg.common.network.packets.swg.zone.SceneEndBaselines;
import com.projectswg.common.network.packets.swg.zone.baselines.Baseline;
import com.projectswg.common.network.packets.swg.zone.insertion.CmdStartScene;
import com.projectswg.common.network.packets.swg.zone.insertion.SelectCharacter;
import com.projectswg.connection.HolocoreSocket;
import com.projectswg.connection.RawPacket;
import com.projectswg.connection.ServerConnectionChangedReason;
import com.projectswg.holocore.resources.support.objects.ObjectCreator;
import com.projectswg.holocore.resources.support.objects.swg.SWGObject;
import me.joshlarson.jlcommon.concurrency.BasicThread;
import me.joshlarson.jlcommon.concurrency.Delay;
import org.junit.Assert;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class HolocoreClient {
	
	private final HolocoreSocket socket;
	private final BasicThread listenThread;
	private final AtomicReference<Location> location;
	private final AtomicReference<Terrain> terrain;
	private final Map<Long, SWGObject> objectsAware;
	private final Map<Long, SWGObject> objectsInProgress;
	private final Map<Long, String> characters;
	private final BlockingQueue<SWGPacket> packets;
	private final AtomicLong characterId;
	private final AtomicBoolean zonedIn;
	
	public HolocoreClient() {
		this(new InetSocketAddress("localhost", 44463));
	}
	
	public HolocoreClient(InetSocketAddress address) {
		this.socket = new HolocoreSocket(address.getAddress(), address.getPort());
		this.listenThread = new BasicThread("holocore-client-listen", this::listen);
		this.location = new AtomicReference<>(null);
		this.terrain = new AtomicReference<>(null);
		this.objectsAware = new ConcurrentHashMap<>();
		this.objectsInProgress = new ConcurrentHashMap<>();
		this.characters = new ConcurrentHashMap<>();
		this.packets = new LinkedBlockingQueue<>();
		this.characterId = new AtomicLong(0);
		this.zonedIn = new AtomicBoolean(false);
	}
	
	public long getCharacterId() {
		return characterId.get();
	}
	
	public String getCharacterName() {
		return characters.get(getCharacterId());
	}
	
	public void addCharacter(long id, String name) {
		this.characters.put(id, name);
	}
	
	public boolean login(String username, String password) {
		Assert.assertTrue(socket.connect(5000));
		listenThread.start();
		send(new HoloLoginRequestPacket(username, password));
		HoloLoginResponsePacket response = receiveNext(PacketType.HOLO_LOGIN_RESPONSE);
		return response.isSuccess();
	}
	
	public void zoneIn(long characterId) {
		this.characterId.set(characterId);
		send(new SelectCharacter(characterId));
	}
	
	public void waitForZoneIn() {
		long start = System.nanoTime();
		while ((!zonedIn.get() || !objectsAware.containsKey(getCharacterId())) && System.nanoTime() - start < 10E9) {
			Delay.sleepMilli(100);
		}
		Assert.assertTrue(zonedIn.get());
		Assert.assertTrue(objectsAware.containsKey(getCharacterId()));
	}
	
	public void disconnect() {
		listenThread.stop(false);
		socket.disconnect(ServerConnectionChangedReason.CLIENT_DISCONNECT);
		socket.terminate();
		listenThread.awaitTermination(1000);
	}
	
	public void send(SWGPacket packet) {
		socket.send(packet.encode().array());
	}
	
	@SuppressWarnings("unchecked")
	public <T extends SWGPacket> T receiveNext(PacketType type) {
		SWGPacket packet;
		while ((packet = receive()) != null) {
			if (packet.getPacketType() == type) {
				return (T) packet;
			}
		}
		return null;
	}
	
	public SWGPacket receive() {
		try {
			return packets.poll(5, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			return null;
		}
	}
	
	private void listen() {
		RawPacket received;
		while ((received = socket.receive()) != null) {
			SWGPacket packet = PacketType.getForCrc(received.getCrc());
			Assert.assertNotNull(packet);
			packet.decode(NetBuffer.wrap(received.getData()));
			process(packet);
			this.packets.add(packet);
		}
	}
	
	private void process(SWGPacket packet) {
		switch (packet.getPacketType()) {
			case HOLO_LOGIN_RESPONSE:
				processLoginResponse((HoloLoginResponsePacket) packet);
				break;
			case CMD_START_SCENE:
				zonedIn.set(true);
				location.set(((CmdStartScene) packet).getLocation());
				terrain.set(((CmdStartScene) packet).getLocation().getTerrain());
				send(new CmdSceneReady());
				break;
			case SCENE_CREATE_OBJECT_BY_CRC:
				processSceneCreateObject((SceneCreateObjectByCrc) packet);
				break;
			case BASELINE:
				processBaseline((Baseline) packet);
				break;
			case SCENE_END_BASELINES:
				processEndBaselines((SceneEndBaselines) packet);
				break;
		}
	}
	
	private void processLoginResponse(HoloLoginResponsePacket login) {
		for (SWGCharacter character : login.getCharacters()) {
			characters.put(character.getId(), character.getName());
		}
	}
	
	private void processSceneCreateObject(SceneCreateObjectByCrc create) {
		SWGObject obj = ObjectCreator.createObjectFromTemplate(CRC.getString(create.getObjectCrc()));
		obj.setLocation(Location.builder(create.getLocation()).setTerrain(terrain.get()).build());
		objectsInProgress.put(create.getObjectId(), obj);
	}
	
	private void processBaseline(Baseline base) {
		SWGObject obj = objectsInProgress.get(base.getObjectId());
		Objects.requireNonNull(obj);
		obj.parseBaseline(base);
	}
	
	private void processEndBaselines(SceneEndBaselines end) {
		SWGObject obj = objectsInProgress.get(end.getObjectId());
		Objects.requireNonNull(obj);
		objectsAware.put(end.getObjectId(), obj);
	}
	
}
