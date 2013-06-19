package util;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PlainNeighborsTableSet implements PlainNeighborsTable {

	private Set<Neighbor> neighborsList;
	
	public PlainNeighborsTableSet() {
		neighborsList = new HashSet<Neighbor>();
	}
	
	public PlainNeighborsTableSet(int initCapacity) {
		neighborsList = new HashSet<Neighbor>(initCapacity);
	}
	
	@Override
	public Neighbor getNeighbor(Id nodeId) {
		for (Neighbor n : neighborsList) {
			if (n.getId().equals(nodeId)) {
				return n;
			}
		}
		return null;
	}

	@Override
	public Neighbor getNeighbor(String stringId) {
		for (Iterator<Neighbor> i = neighborsList.iterator(); i.hasNext();) {
		    Neighbor n = i.next();
		    if (n.getId().toString().equals(stringId)) {   
		        return n;
		    }
		}
		return null;
	}

	@Override
	public boolean addNeighbor(Neighbor node) {
		return neighborsList.add(node);

	}

	@Override
	public boolean removeNeighbor(Id nodeId) {
		for (Iterator<Neighbor> i = neighborsList.iterator(); i.hasNext();) {
		    Neighbor n = i.next();
		    if (n.getId().equals(nodeId)) {   
		        i.remove();
		        return true;
		    }
		}
		return false;
	}

	@Override
	public boolean removeNeighbor(Neighbor node) {
		return neighborsList.remove(node);
	}

	@Override
	public Neighbor[] toArray() {
		return neighborsList.toArray(new Neighbor[0]);
	}

	@Override
	public Iterator<Neighbor> iterator() {
		return neighborsList.iterator();
	}

	@Override
	public int getSize() {
		return neighborsList.size();
	}

}
