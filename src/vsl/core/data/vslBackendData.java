package vsl.core.data;

import java.io.Serializable;


/**
 * This is the parent class that all the data entry types should extend.
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
public class vslBackendData implements Serializable {
	// test casting back to vslBackendData from e.g. vslChunk and make sure
	// versionNum is still around
	public static final int versionNum=0;
}

