package vsl.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;

import vsl.core.data.vslBackendData;

import vsl.core.types.vslID;


// just for testing -- eventually we should load this configurably
import vsl.backends.multimap.vslMMBackend;

public class vsl {

	private static vslBackend backend=null;

	private static vslConfig config = null;

	//private List<String> entries;
	//private Logger log;
	
	public vsl(String configFile) 
		throws vslException
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
	
    public vslEntry updateEntry(vslID entryId, vslDataType updateData)
		throws vslStorageException, vslInputException
	{
		vslEntry select_entry = new vslEntry(entryId);
		select_entry.addVersion(updateData);
		select_entry.store();
		return select_entry;
	}

    public vslEntry getEntry(vslID entryId)
		throws vslStorageException, vslInputException
	{
		vslEntry select_entry = new vslEntry(entryId);
		select_entry.load();
		return select_entry;
	}
	
	public vslID addIndex(vslIndex index)
		throws vslStorageException
	{
		index.store();
		return index.getID();
	}
	
    public void updateIndex(vslIndex index)
		throws vslStorageException, vslInputException, vslConsistencyException
	{
		index.store();
		// this won't really work ... we need a reload method!!
		//index.load();
	}

    public vslIndex getIndex(vslID entryId, boolean blocking)
		throws vslStorageException, vslInputException, vslConsistencyException
	{
		vslIndex index = new vslIndex(entryId, blocking);
		return index;
	}

	public static vslConfig getConfig() {
		return config;
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
	static vslFuture create(Collection<? extends vslBackendData> data)
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
	static vslFuture add(vslID id, Collection<? extends vslBackendData> data)
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
		throws vslException
	{
		config = new vslConfig(configFile);
		// init logging first
		vslLog.init(config);

		// init the backend
		Class backendClass = null;
		try {
			String backendStr = config.getString(config.BACKEND);
			vslLog.log(vslLog.INFO, "Using backend: " + backendStr);
			backendClass =  Class.forName(backendStr);
			backend = (vslBackend) backendClass.newInstance();
		}
		catch (Exception e)
		{
			vslLog.log(vslLog.ERROR, 
					"Could not generate backend from class name: " +	
					config.getString(config.BACKEND));	
			throw new vslConfigException(e);
		}

		// just for testing -- eventually we should load this configurably
		// and the configFile param should be read to get the DB name
		//backend = new vslMMBackend(configFile);
	}


	//private void storeEntry(vslEntry entry)
	//	throws vslStorageException
	//{
		
	//}

}

