package util;

import java.net.InetAddress;

public interface Neighbor {
	
	/**
	 * 
	 * @return The unique Id of the neighbor
	 */
	public String getId();
	
	/**
	 * 
	 * @return The IP address of the neighbor
	 */
	public InetAddress getAddress();

}
