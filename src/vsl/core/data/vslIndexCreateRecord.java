package vsl.core.data;

import java.io.Serializable;

import vsl.core.types.*;

/**
 * This record represents the first entry for a vslIndexElement (when it is
 * first created).  Note that although we store "data" as a generic object
 * below, each type of vslIndexElement will have an associated data type (as it
 * is a Generic class) and will only hanlde IndexRecords whose data object
 * matches that type.
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
public class vslIndexCreateRecord extends vslIndexRecordBase {

	/**
	 * Note: in the core vslElement is typed (i.e. its a generic) and the data
	 * below is required to be on this type.
	 */
	public Serializable data = null;
	
}

