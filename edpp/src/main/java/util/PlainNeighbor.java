package util;

import java.net.InetAddress;

public class PlainNeighbor extends Neighbor {

	private double weight;
	
	public PlainNeighbor(byte[] id, InetAddress address, double weight) {
		super(id, address);
		this.setWeight(weight);
	}

	public PlainNeighbor(Id id, InetAddress address, double weight) {
		super(id, address);
		this.setWeight(weight);
	}
	
	public PlainNeighbor(String stringId, InetAddress address, double weight) {
		super(stringId, address);
		this.setWeight(weight);
	}

	public double getWeight() {
		return weight;
	}

	public void setWeight(double weight) {
		this.weight = weight;
	}
	
}
