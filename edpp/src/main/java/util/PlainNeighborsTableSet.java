package util;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class PlainNeighborsTableSet implements PlainNeighborsTable {

	private Set<PlainNeighbor> neighborsList;
	
	public PlainNeighborsTableSet() {
		neighborsList = Collections.synchronizedSet(new HashSet<PlainNeighbor>());
	}
	
	public PlainNeighborsTableSet(int initCapacity) {
		neighborsList = Collections.synchronizedSet(new HashSet<PlainNeighbor>(initCapacity));
	}
	
	@Override
	public PlainNeighbor getNeighbor(Id nodeId) {
		synchronized (neighborsList) {
			for (PlainNeighbor n : neighborsList) {
				if (n.getId().equals(nodeId)) {
					return n;
				}
			}			
		}
		return null;
	}

	@Override
	public PlainNeighbor getNeighbor(String stringId) {
		synchronized (neighborsList) {
			for (Iterator<PlainNeighbor> i = neighborsList.iterator(); i.hasNext();) {
				PlainNeighbor n = i.next();
				if (n.getId().toString().equals(stringId)) {   
					return n;
				}
			}			
		}
		return null;
	}

	@Override
	public boolean addNeighbor(PlainNeighbor node) {
		return neighborsList.add(node);

	}

	@Override
	public boolean removeNeighbor(Id nodeId) {
		synchronized (neighborsList) {
			for (Iterator<PlainNeighbor> i = neighborsList.iterator(); i.hasNext();) {
				PlainNeighbor n = i.next();
				if (n.getId().equals(nodeId)) {   
					i.remove();
					return true;
				}
			}			
		}
		return false;
	}

	@Override
	public boolean removeNeighbor(PlainNeighbor node) {
		return neighborsList.remove(node);
	}

	@Override
	public PlainNeighbor[] toArray() {
		return neighborsList.toArray(new PlainNeighbor[0]);
	}

	@Override
	public Iterator<PlainNeighbor> iterator() {
		return neighborsList.iterator();
	}

	@Override
	public int getSize() {
		return neighborsList.size();
	}

	@Override
	public double getWeight(Neighbor node) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.equals(node))
					return pn.getWeight();
			}			
		}
		return -1;
	}

	@Override
	public double getWeight(Id nodeId) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.getId().equals(nodeId))
					return pn.getWeight();
			}			
		}
		return -1;
	}

	@Override
	public double getWeight(String nodeId) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.getId().toString().equals(nodeId))
					return pn.getWeight();
			}			
		}
		return -1;
	}

	@Override
	public boolean setWeight(Neighbor node, double weight) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.equals(node)) {
					pn.setWeight(weight);
					return true;
				}
			}			
		}
		return false;
	}

	@Override
	public boolean setWeight(Id nodeId, double weight) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.getId().equals(nodeId)) {
					pn.setWeight(weight);
					return true;
				}
			}			
		}
		return false;
	}

	@Override
	public boolean setWeight(String nodeId, double weight) {
		synchronized (neighborsList) {
			for (PlainNeighbor pn : neighborsList) {
				if (pn.getId().toString().equals(nodeId)) {
					pn.setWeight(weight);
					return true;
				}
			}			
		}
		return false;
	}

}
