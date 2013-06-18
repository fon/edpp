package util;

public interface NeighborsTable extends Iterable<Neighbor>{

	public Neighbor getNeighbor(int index);
	
	public void addNeighbor(Neighbor node);
	
	public boolean removeNeighbor(String nodeId);
	
	public boolean removeNeighbor(Neighbor node);
	
	public Neighbor [] toArray();
	
}
