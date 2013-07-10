package network;

import java.util.Set;

import util.Id;
import util.Neighbor;

public interface Node {

	public Set<Neighbor> getOutNeighbors();
	
	public Id getLocalId();
	
//	public boolean isAlive(Neighbor n);
	
	public int getDiameter();
	
	
}
