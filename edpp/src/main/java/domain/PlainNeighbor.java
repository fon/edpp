package domain;

import java.net.InetAddress;

import com.google.common.util.concurrent.AtomicDouble;

public class PlainNeighbor extends Neighbor {

	private AtomicDouble weight;
	
	public PlainNeighbor(byte[] id, InetAddress address, double weight) {
		super(id, address);
		this.weight = new AtomicDouble(weight);
	}

	public PlainNeighbor(Id id, InetAddress address, double weight) {
		super(id, address);
		this.weight = new AtomicDouble(weight);
	}
	
	public PlainNeighbor(String stringId, InetAddress address, double weight) {
		super(stringId, address);
		this.setWeight(weight);
	}

	public double getWeight() {
		return weight.get();
	}

	public void setWeight(double weight) {
		this.weight.set(weight);
	}
	
}
