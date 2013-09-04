package domain;

import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;

/**
 * Class for wrapping the ids used by the underlying network for identifying the
 * nodes
 * 
 * @author Xenofon Foukas
 * 
 */
public class Id {

	private byte[] id;

	/**
	 * Copy constructor
	 * 
	 * @param id
	 *            the id to be copied to the new object
	 */
	public Id(Id id) {
		this.id = id.getByteRepresentation();
	}

	/**
	 * Class constructor
	 * 
	 * @param id
	 *            the byte representation of the new Id
	 */
	public Id(byte[] id) {
		this.id = id.clone();
	}

	/**
	 * Class constructor
	 * 
	 * @param stringId
	 *            the string representation of the new Id
	 */
	public Id(String stringId) {
		this.id = getByteIdFromString(stringId);
	}

	/**
	 * 
	 * @return a byte array containing this Id
	 */
	public byte[] getByteRepresentation() {
		return id;
	}

	@Override
	public String toString() {
		return getStringId(this.id);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof Id))
			return false;
		Id i = (Id) obj;

		return Arrays.equals(id, i.getByteRepresentation());

	}

	/**
	 * Converts a base64 string to a byte array representation of the Id
	 * 
	 * @param stringId
	 *            The base64 representation of the Id
	 * @return A byte array of the Id
	 */
	public static byte[] getByteIdFromString(String stringId) {
		byte[] byteId = Base64.decodeBase64(stringId);
		return byteId;
	}

	/**
	 * Converts a byte array representation of the Id to a base 64 string
	 * 
	 * @param byteId
	 *            The byte array representation of the Id
	 * @return A base64 string representation
	 */
	public static String getStringId(byte[] byteId) {
		String stringId = new String(Base64.encodeBase64(byteId));
		return stringId;
	}

}
