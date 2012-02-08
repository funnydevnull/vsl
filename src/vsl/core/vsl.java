package vsl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import vsl.core.data.vslBackendData;

import vsl.core.types.vslID;


// just for testing -- eventually we should load this configurably
import vsl.backends.multimap.vslMMBackend;

public class vsl {

	private static vslBackend backend=null;
	private List<String> entries;
	//private Logger log;
	
	public vsl(String configFile) 
		throws vslStorageException
	{
		init(configFile);
	}


	/* -------------- API INTERFACES ----------------------- */

	public vslID addEntry(vslDataType newEntry)
		throws vslStorageException
	{
		vslEntry entry = new vslEntry(newEntry);
		entry.store();
		return entry.getID();
	}
	
	public vslID updateEntry(vslID entryId, vslDataType updateData)
		throws vslStorageException
	{
		vslEntry select_entry = new vslEntry(entryId);
		select_entry.addVersion(updateData, null);
		select_entry.store();
		return select_entry.getID();
	}

	/* -------------- PUBLIC UTILITY METHODS ------------------- */

	/* -------------- SETTER ------------------ */

	public void setBackend(vslBackend b) {
		backend = b;
	}

	public void save()
		throws vslStorageException
	{
		backend.close();	
	}

	// temporary debugging function
	public void debugShow()
		throws vslStorageException
	{
		((vslMMBackend)backend).printMap();	
	}


	/* -------------STATIC  Package level utility methods -------- */


	/**
	 * Create a new entry in the backend and populate with the given data.
	 */
	static vslFuture create(vslBackendData data)
		throws vslStorageException
	{
		vslFuture res = backend.create(data);
		return res;
	}
	
	/**
	 * Create a new entry in the backend and populate with the given data set.
	 */
	static vslFuture create(Vector<? extends vslBackendData> data)
		throws vslStorageException
	{
		vslFuture res = backend.create(data);
		return res;
	}

	/**
	 * Add new entries to an existing backend map. 
	 */
	static vslFuture add(vslID id, vslBackendData data)
		throws vslStorageException
	{
		vslFuture res = backend.add(id, data);
		return res;
	}
	
	/**
	 * Add new entries to an existing backend map. 
	 */
	static vslFuture add(vslID id, Vector<? extends vslBackendData> data)
		throws vslStorageException
	{
		vslFuture res = backend.add(id, data);
		return res;
	}
	
	/**
	 * Load entry from existing backend map.
	 */
	static vslFuture load(vslID id)
		throws vslStorageException
	{
		vslFuture entry = backend.getEntry(id);
		return entry;
	}

	/* ------------- PRIVATE UTILITY METHODS -------------- */
	
	private void init(String configFile)
		throws vslStorageException
	{
		// just for testing -- eventually we should load this configurably
		// and the configFile param should be read to get the DB name
		backend = new vslMMBackend(configFile);
	}


	private void storeEntry(vslEntry entry)
		throws vslStorageException
	{
		
	}

}

