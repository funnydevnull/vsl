package vsl.core;

import vsl.core.types.vslID;
import vsl.core.data.vslBackendData;

import java.util.Collection;

public interface vslBackend {
	
	public void init() throws vslStorageException;
	//public void sync();
	public void close() throws vslStorageException;

	/**
	 * Store a new entry passed into the backend and return a Future with the
	 * status of the put.  The future should also allow retreival of the new
	 * entry's ID.
	 */
	public vslFuture create(vslBackendData entry) throws vslStorageException;

	/**
	 * Add this entry to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslFuture add(vslID id, vslBackendData entry) throws vslStorageException;
	
	/**
	 * Store the new entries passed into the backend under the same key and
	 * return a Future with the status of the put.  The future should also
	 * allow retreival of the new entry's ID.
	 */
	public vslFuture create(Collection<? extends vslBackendData> entries) 
		throws vslStorageException;

	/**
	 * Add entries to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslFuture add(vslID id, Collection<? extends vslBackendData> entries) 
				throws vslStorageException;

	/**
	 * Get the set of entries associated with the given vslID.  Note that the
	 * entries will generally be completely unordered.  The entries are returned
	 * asychronously in the vslFuture.
	 */
	public vslFuture getEntry(vslID id) throws vslStorageException;

}
