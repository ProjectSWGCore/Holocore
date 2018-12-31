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

import com.projectswg.utility.packets.PacketCaptureAnalysis.PacketCaptureAssertion;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ProcessPacketCapture {
	
	public static void main(String [] args) throws IOException {
		try (DataInputStream packetCapture = new DataInputStream(new FileInputStream(new File(args[0])))) {
			byte version = packetCapture.readByte();
			assert version == 2;
			Map<String, Object> information = readSystemInformation(version, packetCapture);
			List<PacketRecord> packets = readPackets(packetCapture);
			System.out.println("Read " + packets.size() + " packets");
			
			if (args.length > 1 && args[1].equalsIgnoreCase("--printPackets")) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm:ss.SSS").withZone((ZoneId) information.get("time.time_zone"));
				int packetNumber = 0;
				int packetPadding = (int) Math.floor(Math.log10(packets.size())) + 1;
				for (PacketRecord packet : packets) {
					System.out.printf("%s [%0"+packetPadding+"d] %s %s%n", formatter.format(packet.getTime()), packetNumber, (packet.isServer() ? "OUT: " : "IN:  "), packet.parse());
					packetNumber++;
				}
			} else {
				PacketCaptureAnalysis analysis = PacketCaptureAnalysis.from(packets);
				System.out.println("Analysis:");
				System.out.println("    Objects Created: " + analysis.getObjectCreations());
				System.out.println("    Objects Deleted: " + analysis.getObjectDeletions() + " [Implicit: " + analysis.getObjectDeletionsImplicit() + "]");
				System.out.println("    Zone-ins:        " + analysis.getCharacterZoneIns() + " " + analysis.getPlayers());
				System.out.println("    Errors:          " + analysis.getErrors().size());
				for (PacketCaptureAssertion e : analysis.getErrors()) {
					System.err.println("        " + e.getMessage());
					System.err.println("            " + e.getPacket());
				}
			}
		}
	}
	
	private static Map<String, Object> readSystemInformation(byte version, DataInputStream packetCapture) throws IOException {
		int count = packetCapture.readByte();
		System.out.println("System Information:");
		Map<String, Object> information = new LinkedHashMap<>();
		for (int i = 0; i < count; i++) {
			Map.Entry<String, Object> entry = parseEntry(version, packetCapture.readUTF());
			information.put(entry.getKey(), entry.getValue());
		}
		
		int maxLength = information.keySet().stream().mapToInt(String::length).max().orElse(10);
		for (Entry<String, Object> e : information.entrySet()) {
			System.out.printf("    %-"+maxLength+"s = %s%n", e.getKey(), e.getValue().toString());
		}
		return information;
	}
	
	private static List<PacketRecord> readPackets(DataInputStream packetCapture) throws IOException {
		List<PacketRecord> packets = new ArrayList<>(1024);
		while (packetCapture.available() >= 11) {
			boolean server = packetCapture.readBoolean();
			Instant time = Instant.ofEpochMilli(packetCapture.readLong());
			int dataLength = packetCapture.readUnsignedShort();
			byte [] data = new byte[dataLength];
			int n = packetCapture.read(data);
			while (n < dataLength)
				n += packetCapture.read(data, n, dataLength - n);
			assert n == dataLength;
			packets.add(new PacketRecord(server, time, data));
		}
		return packets;
	}
	
	private static Map.Entry<String, Object> parseEntry(byte version, String str) {
		String [] keyValue = str.split("=", 2);
		assert keyValue.length == 2;
		String key = keyValue[0].toLowerCase(Locale.US);
		String value = keyValue[1];
		
		if (version == 2) {
			switch (key) {
				case "time.current_time":
					return Map.entry(key, Instant.ofEpochMilli(Long.parseLong(value)));
				case "time.time_zone":
					return Map.entry(key, ZoneId.of(value.split(":")[0]));
				default:
					return Map.entry(key, value);
			}
		} else if (version == 3) {
			switch (key) {
				case "time.current_time":
					return Map.entry(key, Instant.parse(value));
				case "time.time_zone":
					return Map.entry(key, ZoneId.of(value));
				default:
					return Map.entry(key, value);
			}
		} else {
			return Map.entry(key, value);
		}
	}
	
}
