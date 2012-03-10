package vsl.core.util;

import java.security.SecureRandom;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class UtilFunctions {


	private static SecureRandom randGen 
		= new SecureRandom(ByteBuffer.allocate(8).putLong(System.nanoTime()).array());

	/**
	 * Attempt to generate a secure hash using the passed object as
	 * <em>part</em> of a seed, along with a timestamp. 
	 * <em>NOTE:</em> Currently we're just using the time as a seed but
	 * eventually we should use the object and maybe some other system data.
	 */
	public static long genSecureHash(Serializable seed) {
		if (seed == null) {
			randGen.setSeed(ByteBuffer.allocate(8).putLong(System.nanoTime()).array());
		}
		else
		{
			randGen.setSeed(ByteBuffer.allocate(8).putLong(System.nanoTime()).array());
		}
		byte[] randBytes = new byte[8];
		randGen.nextBytes(randBytes);
		long hash = ByteBuffer.wrap(randBytes).getLong();
		return hash;
	}

	/**
	 * Attempt to generate a secure hash using the passed object as
	 * <em>part</em> of a seed, along with a timestamp. 
	 * <em>NOTE:</em> Currently we're just using the time as a seed but
	 * eventually we should use the object and maybe some other system data.
	 */
	public static long genSecureHash() {
		return genSecureHash(null);
	}


}
