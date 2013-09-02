package domain.structure;

import domain.Id;
import domain.TimedNeighbor;


public interface TimedNeighborsTable extends NeighborsTable<TimedNeighbor> {

	public TimedNeighbor [] getExpiredNeighbors();
	
	public TimedNeighbor [] getValidNeighbors();
	
	public boolean setNeighborTimer(Id nodeId, long time);
	
	public boolean renewTimer(TimedNeighbor node);
	
	public boolean renewTimer(Id nodeId);

	public boolean renewTimer(String nodeId);
	
	public void setDefaultTimeValue(long time);

	public void renewTimers();

	public boolean setTimerToInf(String nodeId);
}
