package com.projectswg.holocore;

import com.projectswg.common.network.packets.swg.holo.HoloConnectionStopped.ConnectionStoppedReason;
import com.projectswg.holocore.client.HolocoreSocket;
import me.joshlarson.jlcommon.concurrency.BasicThread;
import me.joshlarson.jlcommon.concurrency.Delay;

import java.net.InetAddress;

public class ProjectSWGRunner {
	
	private final BasicThread runner;
	
	public ProjectSWGRunner() {
		this.runner = new BasicThread("holocore", () -> ProjectSWG.run(new String[0]));
	}
	
	public void start() {
		runner.start();
		{
			try (HolocoreSocket socket = new HolocoreSocket(InetAddress.getLoopbackAddress(), 44463)) {
				long start = System.nanoTime();
				boolean connected = false;
				while (System.nanoTime() - start <= 60E9) { // 60s max wait
					connected = socket.getServerStatus().equals("UP");
					if (connected)
						break;
					Delay.sleepSeconds(1);
				}
				if (connected) {
					start = System.nanoTime();
					while (System.nanoTime() - start <= 60E9) { // 60s max wait
						if (socket.connect(1000)) {
							socket.disconnect(ConnectionStoppedReason.NETWORK);
							break;
						}
					}
				}
			}
		}
	}
	
	public void stop() {
		runner.stop(true);
		runner.awaitTermination(5000);
	}
	
}
