package vsl.core.data;

import vsl.core.types.vslID;
import vsl.core.types.vslDate;
import vsl.core.types.vslHash;

public class vslVersionHeader extends vslBackendData {

	/**
	 * Seems worrisome we're using too many customized rather than standard
	 * data types here.  Once we have a running system we have to fix this
	 * anyway so maybe we should decide once and for all.
	 */
	public vslID id = null;
	public vslHash hash = null;
	public vslID[] prevID = null;
	public vslDate	createTime = null;
	
}
