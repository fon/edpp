package util;

import java.net.InetAddress;

public class TimedNeighbor extends Neighbor {
	
	private long remainingTime;
	
	
	public TimedNeighbor(byte[] id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = remainingTime;
	}

	public TimedNeighbor(Id id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = remainingTime;
	}
	
	/**
	 * 
	 * @return The time remaining until
	 * the node needs to be probed for liveness
	 */
	public long getTimeToProbe() {
		return remainingTime;
	}
	
	public void setRemainingTime(long time) {
		this.remainingTime = time;
	}
	
}
