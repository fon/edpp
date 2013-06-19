package util;

public interface PlainNeighborsTable extends NeighborsTable<PlainNeighbor> {

	public double getWeight(Neighbor node);
	
	public double getWeight(Id nodeId);
	
	public double getWeight(String nodeId);
	
	public boolean setWeight(Neighbor node, double weight);
	
	public boolean setWeight(Id nodeId, double weight);
	
	public boolean setWeight(String nodeId, double weight);
}
