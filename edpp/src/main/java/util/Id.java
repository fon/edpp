package util;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

public class Id {
	
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
	
	/**
	 * Converts a base64 string to a byte array representation of the Id
	 * @param stringId The base64 representation of the Id
	 * @return A byte array of the Id
	 */
	public static byte [] getByteIdFromString(String stringId) {
		byte [] byteId = Base64.decodeBase64(stringId);
		return byteId;
	}
	
	/**
	 * Converts a byte array representation of the Id to a base 64 string
	 * @param byteId The byte array representation of the Id
	 * @return A base64 string representation
	 */
	public static String getStringId(byte [] byteId) {
		String stringId = new String(Base64.encodeBase64(byteId));
		return stringId;
	}

}
