package vsl.test.byteUtils;

import java.util.Comparator;

class ByteComparator implements Comparator<byte[]> {

	/**
	 * Sort byte arrays element-wise.  A smaller array will be first when
	 * compared to a longer array.
	 * e.g. how < howdy
	 */
	public int compare(byte[] b1, byte[] b2) {
		int i = 0;
		while(i < b1.length && i < b2.length) {
			if (b1[i] > b2[i])
			{
				return 1;
			}
			else if (b1[i] < b2[i]) {
				return -1;
			}
		}
		/**
		 * if we got here we're at the end of one of the inputs
		 * so we return the difference of their lengths.
		 * We want to prefer short strings over long strings so we take minus
		 * this difference.
		 */
		return -(b1.length - b2.length);
	}


}

