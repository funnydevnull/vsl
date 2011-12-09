import vsl.core.vsl;
import vsl.core.vslEntry;
import vsl.core.vslVersion;

public class vsl_tester {
	public static void main(String[] args) {
		vsl temp = new vsl();
		vslStringChunker testChunk = new vslStringChunker("testing");
		/*vslDataChunk[] string_chunks = chk.chunks();
		for (int i=0; i<test.length; i++) {
			System.out.print((char) (test[i].mydata())[0]);
		}*/

		// maybe we want the vslEntry to create that initial version out of the data instead, so go the other
		// way
		vslVersion initVersion = new vslVersion(testChunk);
		vslEntry newEntry = new vslEntry(initVersion);
	}
}