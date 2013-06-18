package util;

public interface TimedNeighborsTable extends NeighborsTable {

	public TimedNeighbor [] getExpiredNeighbors();
	
	public TimedNeighbor [] getValidNeighbors();
	
	public boolean setNeighborTimer(TimedNeighbor node, long time);
	
	public boolean setNeighborTimer(String nodeId, long time);
	
	public boolean renewTimer(TimedNeighbor node);

	public boolean renewTimer(String nodeId);
	
	public void setTimeValue(long time);
}
