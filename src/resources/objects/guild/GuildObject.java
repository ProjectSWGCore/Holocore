package resources.objects.guild;

import network.packets.swg.zone.baselines.Baseline.BaselineType;
import resources.collections.SWGMap;
import resources.collections.SWGSet;
import resources.network.BaselineBuilder;
import resources.network.NetBuffer;
import resources.objects.SWGObject;
import resources.player.Player;
import utilities.Encoder.StringType;

public class GuildObject extends SWGObject {
	
	private static final long serialVersionUID = 1L;
	
	private SWGSet<String> abbreviations = new SWGSet<String>(3, 4, StringType.ASCII);
	private SWGMap<String, Integer> gcwImperialScorePercentileThisGalaxy = new SWGMap<>(6, 2);
	private SWGMap<String, Integer> gcwGroupImperialScorePercentileThisGalaxy = new SWGMap<>(6, 3);
	private SWGMap<String, Long> gcwImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 4);
	private SWGMap<String, Long> gcwGroupImperialScorePercentileHistoryThisGalaxy = new SWGMap<>(6, 5);
	
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
	}
	
	@Override
	protected void parseBaseline3(NetBuffer buffer) {
		super.parseBaseline3(buffer);
		abbreviations = buffer.getSwgSet(3, 5, StringType.ASCII);
	}
	
	@Override
	protected void parseBaseline6(NetBuffer buffer) {
		super.parseBaseline6(buffer);
		gcwImperialScorePercentileThisGalaxy = buffer.getSwgMap(6, 2, StringType.ASCII, Integer.class, true);
		gcwGroupImperialScorePercentileThisGalaxy = buffer.getSwgMap(6, 3, StringType.ASCII, Integer.class, true);
		gcwImperialScorePercentileHistoryThisGalaxy = buffer.getSwgMap(6, 4, StringType.ASCII, Long.class, true);
		gcwGroupImperialScorePercentileHistoryThisGalaxy = buffer.getSwgMap(6, 5, StringType.ASCII, Long.class, true);
	}
	
}
