/***********************************************************************************
 * Copyright (c) 2018 /// Project SWG /// www.projectswg.com                       *
 *                                                                                 *
 * ProjectSWG is the first NGE emulator for Star Wars Galaxies founded on          *
 * July 7th, 2011 after SOE announced the official shutdown of Star Wars Galaxies. *
 * Our goal is to create an emulator which will provide a server for players to    *
 * continue playing a game similar to the one they used to play. We are basing     *
 * it on the final publish of the game prior to end-game events.                   *
 *                                                                                 *
 * This file is part of Holocore.                                                  *
 *                                                                                 *
 * --------------------------------------------------------------------------------*
 *                                                                                 *
 * Holocore is free software: you can redistribute it and/or modify                *
 * it under the terms of the GNU Affero General Public License as                  *
 * published by the Free Software Foundation, either version 3 of the              *
 * License, or (at your option) any later version.                                 *
 *                                                                                 *
 * Holocore is distributed in the hope that it will be useful,                     *
 * but WITHOUT ANY WARRANTY; without even the implied warranty of                  *
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the                   *
 * GNU Affero General Public License for more details.                             *
 *                                                                                 *
 * You should have received a copy of the GNU Affero General Public License        *
 * along with Holocore.  If not, see <http://www.gnu.org/licenses/>.               *
 ***********************************************************************************/

package com.projectswg.utility.packets;

import com.projectswg.common.network.hcap.HcapInputStream;
import com.projectswg.common.network.hcap.PacketRecord;
import com.projectswg.common.network.packets.SWGPacket;
import com.projectswg.utility.packets.PacketCaptureAnalysis.PacketCaptureAssertion;

import java.io.*;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({ "UseOfSystemOutOrSystemErr", "CallToPrintStackTrace" })
public class ProcessPacketCapture {
	
	public static void main(String [] args) {
		for (String arg : args) {
			if (!arg.endsWith(".hcap")) {
				System.out.println("Skipping " + arg + " - does not have .hcap extension");
				continue;
			}
			
			try (HcapInputStream packetCapture = new HcapInputStream(new FileInputStream(new File(arg)))) {
				try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(arg.replace(".hcap", ".txt"))), StandardCharsets.UTF_8))) {
					Map<String, Object> information = packetCapture.getSystemInformation();
					List<PacketRecord> packets = readPackets(packetCapture);
					
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss.SSS").withZone((ZoneId) information.get("time.time_zone"));
					int packetNumber = 0;
					int packetPadding = (int) Math.floor(Math.log10(packets.size())) + 1;
					for (PacketRecord packet : packets) {
						SWGPacket parsed = packet.parse();
						String parsedInfo = (parsed == null) ? String.format("%08X", ByteBuffer.wrap(packet.getData()).order(ByteOrder.LITTLE_ENDIAN).getInt(2)) : parsed.toString();
						output.write(String.format("%s [%0"+packetPadding+"d] %s %s%n", formatter.format(packet.getTime()), packetNumber, (packet.isServer() ? "OUT: " : "IN:  "), parsedInfo));
						packetNumber++;
					}
					
					PacketCaptureAnalysis analysis = PacketCaptureAnalysis.from(packets);
					output.write(String.format("Read %d packets%n", packets.size()));
					output.write(String.format("Analysis:%n"));
					output.write(String.format("    Objects Created: %d%n", analysis.getObjectCreations()));
					output.write(String.format("    Objects Deleted: %d [Implicit: %d]%n", analysis.getObjectDeletions(), analysis.getObjectDeletionsImplicit()));
					output.write(String.format("    Zone-ins:        %d %s%n", analysis.getCharacterZoneIns(), analysis.getPlayers()));
					output.write(String.format("    Errors:          %d%n", analysis.getErrors().size()));
					for (PacketCaptureAssertion e : analysis.getErrors()) {
						output.write("        " + e.getMessage() + System.lineSeparator());
						output.write("            " + e.getPacket() + System.lineSeparator());
					}
					System.out.println("Wrote " + packets.size() + " packets with analysis to " + arg.replace(".hcap", ".txt"));
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	private static List<PacketRecord> readPackets(HcapInputStream packetCapture) throws IOException {
		List<PacketRecord> packets = new ArrayList<>(1024);
		PacketRecord record;
		while ((record = packetCapture.readPacket()) != null) {
			try {
				if (record.getData().length < 6)
					continue;
				record.parse();
				packets.add(record);
			} catch (BufferUnderflowException e) {
				System.err.printf("Packet parser failed for packet of length %d at %s%n", record.getData().length, record.getTime());
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return packets;
	}
	
}
