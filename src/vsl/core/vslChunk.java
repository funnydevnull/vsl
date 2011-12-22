package vsl.core;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class vslDataChunk implements Serializable {
	private ByteBuffer data;
	
	public vslDataChunk() {
		data = null;
	}
	
	public vslDataChunk(int length) {
		// allocate memory for the byte buffer
		data = ByteBuffer.allocate(length);
	}
	
	public vslDataChunk(int length, byte[] d) {
		data = ByteBuffer.allocate(length);
		data.put(d);
	}
	
	public byte[] mydata() {
		// for testing purposes right now
		return data.array();
	}
}