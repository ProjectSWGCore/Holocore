package network;

import java.nio.ByteOrder;

import network.packets.soe.Fragmented;
import resources.SortedLinkedList;

public class FragmentedHandler {
	
	private SortedLinkedList<Fragmented> fragPackets;
	private int fragSize;
	
	public FragmentedHandler() {
		fragPackets = new SortedLinkedList<Fragmented>();
		fragSize = 0;
	}
	
	public void reset() {
		fragPackets.clear();
		fragSize = 0;
	}
	
	public byte [] onReceived(Fragmented f) {
		synchronized (fragPackets) {
			if (insertIfNew(f) && getBufferedSize() == fragSize) {
				byte [] data = new byte[fragSize];
				int offset = 0;
				while (fragPackets.size() > 0 && offset < fragSize) {
					offset = spliceFragmentedIntoBuffer(fragPackets.removeFirst(), data, offset);
				}
				updateMetadata();
				return data;
			}
			return new byte[0];
		}
	}
	
	private boolean insertIfNew(Fragmented f) {
		synchronized (fragPackets) {
			if (!fragPackets.contains(f)) {
				fragPackets.add(f);
				updateMetadata();
				return true;
			}
		}
		return false;
	}
	
	private int spliceFragmentedIntoBuffer(Fragmented f, byte [] data, int offset) {
		byte [] fData = f.encode().array();
		int header = offset==0?8:4;
		System.arraycopy(fData, header, data, offset, fData.length-header);
		return offset + fData.length-header;
	}
	
	private void updateMetadata() {
		synchronized (fragPackets) {
			if (fragPackets.isEmpty())
				fragSize = 0;
			else
				fragSize = fragPackets.getFirst().encode().order(ByteOrder.BIG_ENDIAN).getInt(4);
		}
	}
	
	private int getBufferedSize() {
		int curSize = 0;
		int i = 0;
		short prevSeq = (short) (fragPackets.getFirst().getSequence()-1);
		for (Fragmented frag : fragPackets) {
			// Update previous sequence and verify all in-order
			if (prevSeq+1 != frag.getSequence())
				break;
			prevSeq = frag.getSequence();
			// Update current size
			curSize += (i == 0) ? frag.encode().array().length-8 : frag.encode().array().length-4;
			i++;
			if (curSize >= fragSize)
				break;
		}
		return curSize;
	}
	
}
