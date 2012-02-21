package vsl.core.data;

import java.io.Serializable;
//import java.nio.ByteBuffer;

import vsl.core.types.*;

/**
 * This is the raw data we store in the backend for a dataChunk.
 *
 * Because serialization is sensitive to any change in a class definition and
 * we want data to be persistent over different implementations we use this
 * class as a simple data structure with public member vars and NO
 * methods.  It is intended simply to facilitate storage via serialization.
 *
 * This class should only ever be modified with great care as it will break
 * any persistant storage across versions.
 *
 */
public class vslChunkData extends vslBackendData {
	
	/**
	 * Seems worrisome we're using too many customized rather than standard
	 * data types here.  Once we have a running system we have to fix this
	 * anyway so maybe we should decide once and for all.
	 *
	 * Should members be package-private?
	 */
	public vslHash hash = null;
	public vslDate	createTime = null;
	/* we track in which version this chunk was created in */
	public vslID createdInVersion = null;
	public vslChunkDataExtra extra = null;
	public byte[] data = null;
	
}
