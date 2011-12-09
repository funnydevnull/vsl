import vsl.core.vslData;
import vsl.core.vslDataChunk;

public class vslStringChunker implements vslData {
	private String data;
	
	public vslStringChunker(String s) {
		data = s;
	}
	
	public vslDataChunk[] chunks() {
		// returns an array of data chunks
		// a single chunk is used per character
		vslDataChunk[] retArray;
		int i = 0;
		byte[] byte_string = data.getBytes();
		retArray = new vslDataChunk[byte_string.length];
		while (i<byte_string.length) {
			byte[] container = new byte[1];
			container[0] = byte_string[i];
			// have to pass vslDataChunk a byte array, it's a single element here but that's okay.
			retArray[i] = new vslDataChunk(1,container);
			i++;
		}
		return retArray;
	}
}