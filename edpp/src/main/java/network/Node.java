package network;

import java.util.Set;

import util.Neighbor;

public interface Node {

	public Set<Neighbor> getOutNeighbors();
	
	public boolean isAlive(Neighbor n);
	
	public int getNetworkSize();
	
}
