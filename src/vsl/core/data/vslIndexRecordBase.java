package vsl.core.data;

import java.io.Serializable;

import vsl.core.types.*;

/**
 * This is the base class for the various types of record that we store in the
 * backend for a vslIndexRecord (create, update, delete records).
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
public class vslIndexRecordBase extends vslBackendData {
	
	/**
	 * Seems worrisome we're using too many customized rather than standard
	 * data types here.  Once we have a running system we have to fix this
	 * anyway so maybe we should decide once and for all.
	 *
	 * Should members be package-private?
	 */
	public vslHash hash = null;
	public vslDate	createTime = null;
	public vslElKey	elKey = null;
	public vslRecKey	recKey = null;
	
}

