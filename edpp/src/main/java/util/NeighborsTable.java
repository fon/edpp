package util;


public interface NeighborsTable<T> extends Iterable<T>{

	public T getNeighbor(Id nodeId);
	
	public T getNeighbor(String stringId);
	
	public boolean addNeighbor(T node);
	
	public boolean removeNeighbor(Id nodeId);
	
	public boolean removeNeighbor(T node);
	
	public T [] toArray();
	
	public int getSize();
	
}
