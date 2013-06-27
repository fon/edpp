package util;

import java.net.InetAddress;
import java.util.concurrent.atomic.AtomicLong;

public class TimedNeighbor extends Neighbor {
	
	private volatile AtomicLong remainingTime;
	
	public static final long INF = Long.MAX_VALUE;
	static final long DEFAULT_TIMER = 1000;
	
	public TimedNeighbor(byte[] id, InetAddress address) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	public TimedNeighbor(Id id, InetAddress address) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}
	
	public TimedNeighbor(String stringId, InetAddress address) {
		super(stringId, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}
	
	public TimedNeighbor(byte[] id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}

	public TimedNeighbor(Id id, InetAddress address, long remainingTime) {
		super(id, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}
	
	public TimedNeighbor(String stringId, InetAddress address, long remainingTime) {
		super(stringId, address);
		this.remainingTime = new AtomicLong(DEFAULT_TIMER);
	}
	
	/**
	 * 
	 * @return The time remaining until
	 * the node needs to be probed for liveness
	 */
	public long getTimeToProbe() {
		return remainingTime.get();
	}
	
	/**
	 * Set the value for time remaining
	 * @param time The new remaining time
	 */
	public void setRemainingTime(long time) {
		this.remainingTime.set(time);
	}
	
	/**
	 * Decreases the remaining time
	 * @param time Decrease remaining time by a time parameter
	 * remaining time is never negative
	 */
	public void decreaseTime(long time) {
		if (remainingTime.get()-time < 0)
			remainingTime.set(0);
		else
			remainingTime.addAndGet(-time);
	}
	
	/**
	 * Increases the remaining time
	 * @param time Increase remaining time by time
	 */
	public void increaseTime(long time) {
		remainingTime.addAndGet(time);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null) return false;
		if(this.getClass()!=obj.getClass()) return false;
		TimedNeighbor n = (TimedNeighbor)obj;
		
		return this.id.equals(n.getId());
	}
	
}