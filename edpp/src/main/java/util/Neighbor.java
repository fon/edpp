package util;

import java.net.InetAddress;

public class Neighbor {
	
	protected Id id;
	protected InetAddress address;
	
		
	public Neighbor(byte [] id, InetAddress address) {
		Id i = new Id(id);
		this.id = i;
		this.address = address;
	}
	
	public Neighbor(Id id, InetAddress address) {
		this.id = id;
		this.address = address;
	}
	
	public Id getId() {
		return id;
	}
	
	/**
	 * 
	 * @return The IP address of the neighbor
	 */
	public InetAddress getAddress() {
		return address;
	}
	
	@Override
	public String toString() {
		return id.toString();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if(!(obj instanceof Neighbor)) return false;
		Neighbor n = (Neighbor)obj;
		
		return this.id.equals(n.getId()) && 
				this.address.equals(n.getAddress());
	}

}
