package vsl.test.byteUtils;

import java.util.Arrays;


public class ByteWrapper {

	public byte[] data;

	public ByteWrapper(byte[] b) {
		this.data = b;
	}

	@Override
	public int hashCode()
	{
		return Arrays.hashCode(data);
	}

	@Override
	public boolean equals(Object b)
	{
		if ( ! (b instanceof ByteWrapper)) {
			return false;
		}
		else
		{
			return Arrays.equals(this.data, ( (ByteWrapper)b).data);
		}
	}

}

