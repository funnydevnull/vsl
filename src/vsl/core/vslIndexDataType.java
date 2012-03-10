package vsl.core;

import vsl.core.types.vslElKey;
import vsl.core.types.vslRecKey;
import vsl.core.util.UtilFunctions;

import java.io.Serializable;

/**
 * This class is used to define new data types to be store in index records.
 * When an external interface wants to store data in an element it should
 * implement or use this class and pass an instance of this class to
 * vslIndexElement.<br>
 * <br>
 * Because its a generic this class can often be used without overriding any
 * methods.  It is possible however to override the genElementKey() and
 * genRecordKey() methods associated with a particular data type if desired.
 */
public class vslIndexDataType<D extends Serializable> {

	private D data = null;

	public vslIndexDataType(D data) {
		this.data = data;
	}


	/* ----------------- Overridable methods ------------------ */

	/**
	 * This method is used to generate element keys for elements initialized
	 * with this data type.  In general the default implementation should be
	 * used but if a different kind of key is desired this method can be
	 * overridden.<br>
	 * <br>
	 * To avoid collisions the default implementation uses the data
	 * but combines it with a timestamp to generate a unique hash key.
	 */
	protected vslElKey genElementKey() {
		String keyStr = new Long(UtilFunctions.genSecureHash(data)).toString();
		vslElKey key =  new vslElKey();
		key.setKey(keyStr);
		return key;
	}


	/**
	 * This method is used to generate record keys for records involving updates 
	 * with this data type.  In general the default implementation should be
	 * used but if a different kind of key is desired this method can be
	 * overridden.<br>
	 * <br>
	 * To avoid collisions the default implementation uses the data
	 * but combines it with a timestamp to generate a unique hash key.
	 */
	protected static vslRecKey genRecKey(Serializable obj) {
		String keyStr = null;
		if (obj != null) {
			keyStr = new Long(UtilFunctions.genSecureHash(obj)).toString();
		}
		else
		{
			keyStr = new Long(UtilFunctions.genSecureHash()).toString();
		}
		vslRecKey key =  new vslRecKey();
		key.setKey(keyStr);
		return key;
	}

	/* -------------- Public accessors ------------------- */


	public D getData()
	{
		return data;
	}

}
