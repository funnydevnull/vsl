package vsl.core;


public interface vslBackend {
	
	/**
	 * Store a new entry passed into the backend and return a Future with the
	 * status of the put.  The future should also allow retreival of the new
	 * entries ID.
	 */
	public vslFuture create(vslBackendEntry entry);

	/**
	 * Add this entry to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslFuture add(vslID id, vslBackendEntry entry);

	/**
	 * Get the set of entries associated with the given vslID.  Note that the
	 * entries will generally be completely unordered.  The entries are returned
	 * asychronously in the vslFuture.
	 */
	public vslFuture getEntry(vslID id);

}
