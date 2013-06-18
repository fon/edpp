package util;

import java.util.Arrays;

public class Id {
	
    static final String HEXES = "0123456789ABCDEF";


	private byte [] id;
	
	public Id(Id id) {
		this.id = id.getByteRepresentation();
	}
	
	public Id(byte [] id) {
		this.id = id.clone();
	}
	
	public byte [] getByteRepresentation() {
		return id;
	}
	
	@Override
	public String toString() {
		return getHex(this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if(!(obj instanceof Id)) return false;
		Id i = (Id)obj;
		
		return Arrays.equals(id, i.getByteRepresentation());
		
	}
	
	public static String getHex( byte [] raw ) {
	    if ( raw == null ) {
	      return null;
	    }
	    final StringBuilder hex = new StringBuilder( 2 * raw.length );
	    for ( final byte b : raw ) {
	      hex.append(HEXES.charAt((b & 0xF0) >> 4))
	         .append(HEXES.charAt((b & 0x0F)));
	    }
	    return hex.toString();
	  }
	
}
