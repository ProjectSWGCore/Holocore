package resources.objects.guild;

import java.nio.ByteBuffer;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.collections.SWGMap;
import resources.collections.SWGSet;
import resources.encodables.Encodable;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Encoder.StringType;

public class GuildObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private SWGSet<String> abbreviations = new SWGSet<String>(3, 4, StringType.ASCII);
	private SWGMap<String, CurrentServerGCWZonePercent> gcwImperialScorePercentileThisGalaxy = new SWGMap<>(6, 2);
	private SWGMap<String, CurrentServerGCWZonePercent> gcwGroupImperialScorePercentileThisGalaxy = new SWGMap<>(6, 3);
	private SWGMap<String, CurrentServerGCWZoneHistory> gcwImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 4);
	private SWGMap<String, CurrentServerGCWZoneHistory> gcwGroupImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 5);
	private SWGMap<String, OtherServerGCWZonePercent> gcwImperialScorePercentileOtherGalaxies = new SWGMap<>(6, 6);
	private SWGMap<String, OtherServerGCWZonePercent> gcwGroupImperialScorePercentileOtherGalaxies = new SWGMap<>(6, 7);
	
	public GuildObject(long objectId) {
		super(objectId, BaselineType.GILD);
	}
	
	@Override
	protected void createBaseline3(Player target, BaselineBuilder bb) {
		super.createBaseline3(target, bb);
		bb.addObject(abbreviations);
	}
	
	@Override
	protected void createBaseline6(Player target, BaselineBuilder bb) {
		super.createBaseline6(target, bb);
		bb.addObject(gcwImperialScorePercentileThisGalaxy);
		bb.addObject(gcwGroupImperialScorePercentileThisGalaxy);
		bb.addObject(gcwImperialScorePercentileHistoryThisGalaxy);
		bb.addObject(gcwGroupImperialScorePercentileHistoryThisGalaxy);
		bb.addObject(gcwImperialScorePercentileOtherGalaxies);
		bb.addObject(gcwGroupImperialScorePercentileOtherGalaxies);
	}
	
	@Override
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		abbreviations = buffer.getSwgSet(3, 5, StringType.ASCII);
	}
	
	@Override
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		gcwImperialScorePercentileThisGalaxy = buffer.getSwgMap(6, 2, StringType.ASCII, CurrentServerGCWZonePercent.class);
		gcwGroupImperialScorePercentileThisGalaxy = buffer.getSwgMap(6, 3, StringType.ASCII, CurrentServerGCWZonePercent.class);
		gcwImperialScorePercentileHistoryThisGalaxy = buffer.getSwgMap(6, 4, StringType.ASCII, CurrentServerGCWZoneHistory.class);
		gcwGroupImperialScorePercentileHistoryThisGalaxy = buffer.getSwgMap(6, 5, StringType.ASCII, CurrentServerGCWZoneHistory.class);
		gcwImperialScorePercentileOtherGalaxies = buffer.getSwgMap(6, 6, StringType.ASCII, OtherServerGCWZonePercent.class);
		gcwGroupImperialScorePercentileOtherGalaxies = buffer.getSwgMap(6, 7, StringType.ASCII, OtherServerGCWZonePercent.class);
	}
	
	public static class CurrentServerGCWZonePercent implements Encodable {
		
		private int percentage = 0;
		
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(4);
			data.addInt(percentage);
			return data.array();
		}
		
		public void decode(ByteBuffer data) {
			percentage = data.getInt();
		}
		
		public String toString() {
			return percentage + "%";
		}
	}
	
	public static class CurrentServerGCWZoneHistory implements Encodable {
		
		private int lastUpdateTime = 0;
		private int percentage = 0;
		
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(8);
			data.addInt(lastUpdateTime);
			data.addInt(percentage);
			return data.array();
		}
		
		public void decode(ByteBuffer bb) {
			NetBuffer data = NetBuffer.wrap(bb);
			lastUpdateTime = data.getInt();
			percentage = data.getInt();
		}
		
		public String toString() {
			return lastUpdateTime + ":" + percentage + "%";
		}
	}
	
	public static class OtherServerGCWZonePercent implements Encodable {
		
		private String zone = "";
		private int percentage = 0;
		
		public byte [] encode() {
			NetBuffer data = NetBuffer.allocate(6 + zone.length());
			data.addAscii(zone);
			data.addInt(percentage);
			return data.array();
		}
		
		public void decode(ByteBuffer bb) {
			NetBuffer data = NetBuffer.wrap(bb);
			zone = data.getAscii();
			percentage = data.getInt();
		}
		
		public String toString() {
			return zone + ":" + percentage + "%";
		}
	}
	
}
