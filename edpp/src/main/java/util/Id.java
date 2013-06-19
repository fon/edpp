package util;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

public class Id {
	
    static final String HEXES = "0123456789ABCDEF";


	private byte [] id;
	
	public Id(Id id) {
		this.id = id.getByteRepresentation();
	}
	
	public Id(byte [] id) {
		this.id = id.clone();
	}
	
	public Id(String stringId) {
		this.id = getByteIdFromString(stringId);
	}
	
	public byte [] getByteRepresentation() {
		return id;
	}
	
	@Override
	public String toString() {
		return getStringId(this.id);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj==this) return true;
		if(!(obj instanceof Id)) return false;
		Id i = (Id)obj;
		
		return Arrays.equals(id, i.getByteRepresentation());
		
	}
	
	public static byte [] getByteIdFromString(String stringId) {
		byte [] byteId = Base64.decodeBase64(stringId);
		return byteId;
	}	
	public static String getStringId(byte [] byteId) {
		String stringId = new String(Base64.encodeBase64(byteId));
		return stringId;
	}

}
