package vsl.core.data;

import java.io.Serializable;
import java.util.List;

import vsl.core.types.*;

/**
 * This record represents an update of a vslIndexElement.  The new value (or
 * some update diff) can be stored in the data object.  Note that although we
 * store "data" as a generic object below, each type of vslIndexElement will
 * have an associated data type (as it is a Generic class) and will only hanlde
 * IndexRecords whose data object matches that type.
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
public class vslIndexUpdateRecord extends vslIndexRecordBase {

	/**
	 * The id of the update/create records that precedes this one.  Multiple
	 * possible prev records allow for merging of branches.
	 */
	public List<vslRecKey> prev = null;
	/**
	 * Note: in the core vslElement is typed (i.e. its a generic) and the data
	 * below is required to be on this type.
	 */
	public Object data = null;
	
}

