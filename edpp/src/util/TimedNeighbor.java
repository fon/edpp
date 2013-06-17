package util;

public interface TimedNeighbor extends Neighbor {

	/**
	 * 
	 * @return The time remaining until a check
	 * needs to be performed on the node
	 */
	public long getTimeToCheck();
	
}
