package vsl.backends;

// vsl packages
import vsl.core.vslBackend;
import vsl.core.vslID;

// other packages 
import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;


public class vslMMBackend implements vslBackend {

	// for now use MultiHashMap implementation as backend
	private MultiMap storage = new MultiHashMap();
	
	public vslMMBackend() {
	}

	/**
	 * Store a new entry passed into the backend and return a Future with the
	 * status of the put.  The future should also allow retreival of the new
	 * entries ID.
	 */
	public vslFuture create(vslBackendEntry entry) {
		//implement
	}

	/**
	 * Add this entry to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslFuture add(vslID id, vslBackendEntry entry) {
		//implement
	}

	/**
	 * Get the set of entries associated with the given vslID.  Note that the
	 * entries will generally be completely unordered.  The entries are returned
	 * asychronously in the vslFuture.
	 */
	public vslFuture getEntry(vslID id) {
		//implement
	}

}
