package util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class TimedNeighborsTableSet implements TimedNeighborsTable {


	private Set<TimedNeighbor> neighborsList;
	private long defaultTimerValue;
	
	public TimedNeighborsTableSet() {
		neighborsList = new HashSet<TimedNeighbor>();
		this.defaultTimerValue = TimedNeighbor.DEFAULT_TIMER;
	}
	
	public TimedNeighborsTableSet(long timerValue) {
		neighborsList = new HashSet<TimedNeighbor>();
		this.defaultTimerValue = timerValue;
	}
	
	public TimedNeighborsTableSet(int initCapacity) {
		neighborsList = new HashSet<TimedNeighbor>(initCapacity);
		this.defaultTimerValue = TimedNeighbor.DEFAULT_TIMER;
	}
	
	public TimedNeighborsTableSet(int initCapacity, long timerValue) {
		neighborsList = new HashSet<TimedNeighbor>(initCapacity);
		this.defaultTimerValue = timerValue;
	}
	
	@Override
	public TimedNeighbor getNeighbor(Id nodeId) {
		for (TimedNeighbor n : neighborsList) {
			if (n.getId().equals(nodeId)) {
				return n;
			}
		}
		return null;
	}

	@Override
	public TimedNeighbor getNeighbor(String stringId) {
		for (Iterator<TimedNeighbor> i = neighborsList.iterator(); i.hasNext();) {
		    TimedNeighbor n = i.next();
		    if (n.getId().toString().equals(stringId)) {   
		        return n;
		    }
		}
		return null;
	}

	@Override
	public boolean addNeighbor(TimedNeighbor node) {
		return neighborsList.add(node);
	}

	@Override
	public boolean removeNeighbor(Id nodeId) {
		for (Iterator<TimedNeighbor> i = neighborsList.iterator(); i.hasNext();) {
		    TimedNeighbor n = i.next();
		    if (n.getId().equals(nodeId)) {   
		        i.remove();
		        return true;
		    }
		}
		return false;
	}

	@Override
	public boolean removeNeighbor(TimedNeighbor node) {
		return neighborsList.remove(node);
	}

	@Override
	public TimedNeighbor[] toArray() {
		return neighborsList.toArray(new TimedNeighbor[0]);
	}

	@Override
	public Iterator<TimedNeighbor> iterator() {
		return neighborsList.iterator();
	}

	@Override
	public TimedNeighbor[] getExpiredNeighbors() {
		HashSet<TimedNeighbor> s = new HashSet<TimedNeighbor>();
		for(TimedNeighbor n : neighborsList) {
			if(n.getTimeToProbe() == 0)
				s.add(n);
		}
		return s.toArray(new TimedNeighbor[0]);
	}

	@Override
	public TimedNeighbor[] getValidNeighbors() {
		HashSet<TimedNeighbor> s = new HashSet<TimedNeighbor>();
		for(TimedNeighbor n : neighborsList) {
			if(n.getTimeToProbe() > 0)
				s.add(n);
		}
		return s.toArray(new TimedNeighbor[0]);
	}

	@Override
	public boolean setNeighborTimer(Id nodeId, long time) {
		for(TimedNeighbor n : neighborsList) {
			if (n.getId().equals(nodeId)) {
				n.setRemainingTime(time);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean renewTimer(TimedNeighbor node) {
		for(TimedNeighbor n : neighborsList) {
			if (n.equals(node)) {
				n.setRemainingTime(defaultTimerValue);
				return true;
			}
		}
		return false;
	}
	
	@Override
	public boolean renewTimer(Id nodeId) {
		for(TimedNeighbor n : neighborsList) {
			if (n.getId().equals(nodeId)) {
				n.setRemainingTime(defaultTimerValue);
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean renewTimer(String nodeId) {
		for(TimedNeighbor n : neighborsList) {
			if (n.getId().toString().equals(nodeId)) {
				n.setRemainingTime(defaultTimerValue);
				return true;
			}
		}
		return false;
	}

	@Override
	public void setDefaultTimeValue(long time) {
		this.defaultTimerValue = time;
	}

	@Override
	public int getSize() {
		return neighborsList.size();
	}

}
