package vsl.core;


public interface vslData {
	// interface for conversion between data and chunks
	
	// shouldn't we have chunking be parameterized with chunk sizes?
	// this seems a bit much to return
	public vslDataChunk[] chunks();
}
