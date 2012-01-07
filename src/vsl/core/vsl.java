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
		//storeEntry(entry)
		return entry.getID();
	}
	
	public vslID updateEntry(vslID entryId, vslDataType updateEntry)
		throws vslStorageException
	{
		vslEntry entry = new vslEntry(entryId);
		//System.out.println("updateEntry()");
		return entry.getID();
	}

	/* -------------- PUBLIC UTILITY METHODS ------------------- */

	/* -------------- SETTER ------------------ */

	public void setBackend(vslBackend b) {
		backend = b;
	}

	/*  THIS SECTION IS INTENTIONALLY LEFT HERE PENDING DISCUSSION WITH SHEER REGARDING
	     HOW TO ATTACH A BACKEND THAT'S INITIALIZED (VERSUS HAVING VSL INITIALIZE IT)
	  public void loadBackend(String file)
	{
		// Hedeer: not sure if we want this behavior but makes sense to me
		// it makes sense and I would rather that in TestCore1 we don't instantiate
		// a backend but get to it through this function (so everything goes through vsl)
		//
		// obviously having a file as an argument is specific to this implementation
		// we should have a general way of loading up an existing backend into the vsl
		// maybe it's more correct to have the backend initialized (the way it is now in TestCore1 read)
		// then connect it to the vsl as need be...if that's the case there should at least 
		// be a backend setter in vsl()
		backend.readMap(file);
		}*/

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

