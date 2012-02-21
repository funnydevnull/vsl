package vsl.core.data;

import java.io.Serializable;
import java.nio.ByteBuffer;

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
public class vslChunkHeader extends vslBackendData implements Serializable {

	public vslID	id = null;
	public vslHash hash = null;
	public vslDate createTime = null;
	public vslChunkHeaderExtra extra = null;
	/* whether or not this chunk was created for this version or was old. */
	public boolean createdInVersion = false;

}
