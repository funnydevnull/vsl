package vsl.core.data;

import java.io.Serializable;
import java.util.List;

import vsl.core.types.*;

/**
 * This record represents the deletion of a vslIndexElement. Since
 * vslIndexElements support branched histories a delete record does not
 * necassarily mean the whole record is deleted but simply that a branch ends. 
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
public class vslIndexDeleteRecord extends vslIndexRecordBase {

	/**
	 * The id of the update/create records that precedes this one.  Multiple
	 * possible prev records allow for merging of branches.
	 */
	public List<vslRecKey> prev = null;
	
}

