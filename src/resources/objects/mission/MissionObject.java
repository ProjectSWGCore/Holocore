package resources.objects.mission;

import java.nio.ByteBuffer;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.Point3D;
import resources.Terrain;
import resources.common.CRC;
import resources.encodables.Encodable;
import resources.encodables.StringId;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.objects.intangible.IntangibleObject;
import resources.objects.waypoint.WaypointObject;
import resources.player.Player;

public class MissionObject extends IntangibleObject {
	
	private static final long serialVersionUID = 1L;
	
	private int difficulty					= 0;
	private MissionLocation location		= new MissionLocation();
	private String missionCreator			= "";
	private int reward						= 0;
	private MissionLocation startLocation	= new MissionLocation();
	private CRC targetAppearance			= new CRC();
	private StringId description			= new StringId();
	private StringId title					= new StringId();
	private int status						= 0;
	private CRC missionType					= new CRC();
	private String targetName				= "";
	private WaypointObject waypoint			= new WaypointObject(0);
	
	public MissionObject(long objectId) {
		super(objectId, BaselineType.MISO);
	}
	
	public void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addInt(difficulty);
		bb.addObject(location);
		bb.addUnicode(missionCreator);
		bb.addInt(reward);
		bb.addObject(startLocation);
		bb.addObject(targetAppearance);
		bb.addObject(description);
		bb.addObject(title);
		bb.addInt(status);
		bb.addObject(missionType);
		bb.addAscii(targetName);
		bb.addObject(waypoint);
	}
	
	public void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addInt(-1);
	}
	
	public void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		difficulty = buffer.getInt();
		location = buffer.getEncodable(MissionLocation.class);
		missionCreator = buffer.getUnicode();
		reward = buffer.getInt();
		startLocation = buffer.getEncodable(MissionLocation.class);
		targetAppearance = buffer.getEncodable(CRC.class);
		description = buffer.getEncodable(StringId.class);
		title = buffer.getEncodable(StringId.class);
		status = buffer.getInt();
		missionType = buffer.getEncodable(CRC.class);
		targetName = buffer.getAscii();
		int pos = buffer.position();
		buffer.seek(24);
		buffer.getUnicode();
		waypoint = new WaypointObject(buffer.getLong());
		buffer.position(pos);
		waypoint.decode(buffer.getBuffer());
	}
	
	public void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		buffer.getInt();
	}
	
	public static class MissionLocation implements Encodable {
		
		private Point3D location;
		private long objectId;
		private Terrain terrain;
		
		public MissionLocation() {
			location = new Point3D();
			objectId = 0;
			terrain = Terrain.TATOOINE;
		}
		
		@Override
		public byte[] encode() {
			NetBuffer data = NetBuffer.allocate(24);
			data.addEncodable(location);
			data.addLong(objectId);
			data.addInt(terrain.getCrc());
			return data.array();
		}
		
		@Override
		public void decode(ByteBuffer bb) {
			NetBuffer data = NetBuffer.wrap(bb);
			location = data.getEncodable(Point3D.class);
			objectId = data.getLong();
			terrain = Terrain.getTerrainFromCrc(data.getInt());
		}
		
	}
	
}
