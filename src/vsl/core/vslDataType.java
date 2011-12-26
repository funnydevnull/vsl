package vsl.core;

import java.util.Vector;

/**
 * This class should be implemented for every data type supported by vsl.  For
 * instance we should have things like:
 *
 * - vslFileData
 * - vsl
 *
 * Classes like the above are responsible for taking some external data type
 * and chunking it.
 */
public interface vslDataType {
	
	public String getName();

	public Vector<? extends vslChunk> getNewChunks();
	
	public Vector<? extends vslChunk> getOldChunks();

}
