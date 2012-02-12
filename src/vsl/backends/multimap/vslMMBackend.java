package vsl.backends.multimap;

// vsl packages
//import org.apache.commons.collections.MultiHashMap;
//import org.apache.commons.collections.MultiMap;

//import vsl.debug.DebuggingObjectOutputStream;
import vsl.debug.DebuggingObjectOutputStream2;

import vsl.core.vsl;
import vsl.core.vslLog;
import vsl.core.vslBackend;
import vsl.core.vslFuture;
import vsl.core.vslStorageException;
import vsl.core.vslConfigException;

import vsl.core.data.vslBackendData;
import vsl.core.util.vslBackendDataUtils;
import vsl.core.types.vslID;

import java.util.HashMap;
import java.util.Vector;
import java.util.Date;
import java.util.Random;

import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.File;


public class vslMMBackend implements vslBackend, Serializable {

	/* --------------- CONFIG PARAMS ---------------------- */

	private final static String DBFILE = "backends.multimap.vslMMBackend.dbfile";


	// for now use MultiHashMap implementation as backend
	private HashMap<vslID, Vector<? extends vslBackendData> > storage = null;
	private Random randGen;
	private String dbname = null;

	/**
	 */
	public vslMMBackend() 
		throws vslConfigException, vslStorageException
	{
		dbname = vsl.getConfig().getString(DBFILE);
		init();
	}
	
	
	/**
	 * Used for debugging by creating a vslMMBackend outside of vsl.
	 */
	public vslMMBackend(String dbname) 
		throws vslConfigException, vslStorageException
	{
		this.dbname = dbname;
		init();
	}


	public void init()
		throws vslStorageException
	{
		Date now = new Date();
		randGen = new Random(now.getTime());
		// initialize the DB from the file or from scratch
		if (dbname != null && new File(dbname).exists()) {
			readMap();
		}
		else
		{
			storage = new HashMap<vslID, Vector<? extends vslBackendData> >();
		}
	}

	public void close()
		throws vslStorageException
	{
		try {
			writeMap();
			//debugWriteMap();
		} catch (vslStorageException e) {
			vslStorageException vse = new vslStorageException(
					"IO Exception Writing MMap to file: " + e.toString());
			e.printStackTrace();
			vslLog.log(vslLog.ERROR, e.getStackTrace().toString());
			throw vse;
		}
	}

	/**
	 * Store a new entry passed into the backend and return a Future with the
	 * status of the put.  The future should also allow retreival of the new
	 * entries ID.
	 */
	public vslFuture create(vslBackendData entry) 
		throws vslStorageException
	{
		Vector<vslBackendData> vec = new Vector<vslBackendData>();
		vec.add(entry);
		vslID newID = genID();
		vslLog.log(vslLog.DEBUG, "Creating new entry with id: " + newID);
		storage.put(newID, vec);
		vslMMFuture ret = new vslMMFuture();
		ret.setNewEntryID(newID);
		ret.setSuccess(true);
		ret.setReady();
		return ret;
	}

	/**
	 * Add this entry to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslMMFuture add(vslID id, vslBackendData entry) 
		throws vslStorageException
	{
		Vector<vslBackendData> vec = (Vector<vslBackendData>) storage.get(id);
		vslMMFuture ret = new vslMMFuture();
		if (vec == null)
		{
			vslLog.log(0, "No entry found");
			ret.setSuccess(false);
			ret.setReady();
			return ret;
		}
		vslLog.log(vslLog.DEBUG, "Adding entry to id: " + id);
		vec.add(entry);
		ret.setSuccess(true);
		ret.setReady();
		return ret;
	}
	
	/**
	 * Store the new entries passed into the backend under the same key and
	 * return a Future with the status of the put.  The future should also
	 * allow retreival of the new entry's ID.
	 */
	public vslFuture create(Vector<? extends vslBackendData> entries) 
		throws vslStorageException
	{
		vslID newID = genID();
		storage.put(newID, entries);
		vslMMFuture ret = new vslMMFuture();
		ret.setNewEntryID(newID);
		ret.setSuccess(true);
		ret.setReady();
		return ret;
	}


	/**
	 * Add entries to the backend under the given key.  The backend is a
	 * generalized MultiMap so this entry should be appended to the set
	 * associated with this key.  The backend makes no garauntee about the order
	 * of entries appended.
	 */
	public vslFuture add(vslID id, Vector<? extends vslBackendData> entries) 
				throws vslStorageException
	{
		Vector<vslBackendData> vec = (Vector<vslBackendData>)storage.get(id);
		vslMMFuture ret = new vslMMFuture();
		if (vec == null)
		{
			vslLog.log(0, "No entry found");
			ret.setSuccess(false);
			ret.setReady();
			return ret;
		}
		vec.addAll(entries);
		ret.setSuccess(true);
		ret.setReady();
		return ret;
	}


	/**
	 * Get the set of entries associated with the given vslID.  Note that the
	 * entries will generally be completely unordered.  The entries are returned
	 * asychronously in the vslFuture.
	 */
	public vslFuture getEntry(vslID id) 
		throws vslStorageException
	{
		Vector<? extends vslBackendData> vec = storage.get(id);
		vslMMFuture ret = new vslMMFuture();
		if (vec == null)
		{
			vslLog.log(0, "No entry found");
			ret.setSuccess(false);
			ret.setReady();
			return ret;
		}
		ret.setEntries(vec);
		ret.setSuccess(true);
		ret.setReady();
		return ret;
	}



	/* --------------------- Public Utility Functions ------------------- */



	public void printMap()
	{
		printMap(100);
	}

	public void printMap(int bytesToShow)
	{
		if (bytesToShow < 0) {
			bytesToShow = 100;
		}
		System.out.println("Size: " + storage.keySet().size());
		for(vslID id: storage.keySet())
		{
			System.out.println("===============================");
			System.out.println("Key: [" + id + "]");
			for(vslBackendData data: storage.get(id))
			{
				System.out.println("Val : [" + 
						vslBackendDataUtils.backendDataToString(bytesToShow, data) + "]");
			}
			System.out.println("===============================");
		}
	}
	
	
	public void debugWriteMap()
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(dbname);
		//ObjectOutputStream oos = new ObjectOutputStream(fos);
		DebuggingObjectOutputStream2 oos = new DebuggingObjectOutputStream2(fos);
		try {
			oos.writeObject(this);
		} catch (Exception e) {
		  throw new RuntimeException(
     		 "Serialization error. Path to bad object: " 
          	+ oos.getStack(), e);
		}
		oos.close();
	}


	/* --------------- Private read/write DB functions ------------------- */

	private void readMap()
		throws vslStorageException
	{
		try {
			FileInputStream fis = new FileInputStream(dbname);
			ObjectInputStream ois = new ObjectInputStream(fis);
			//vslMMBackend fmap = (vslMMBackend) ois.readObject();
			storage = (HashMap<vslID, Vector<? extends vslBackendData>>) ois.readObject();
			ois.close();
		} 
		catch (Exception e)
		{
			vslLog.log(vslLog.ERROR, "Error trying to read vslMMBackend database: " + 
					e.toString());
			throw new vslStorageException(e);
		}
    }

	private void writeMap()
		throws vslStorageException
	{
		try {
			FileOutputStream fos = new FileOutputStream(dbname);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(storage);
			oos.close();
		} 
		catch (Exception e)
		{
			vslLog.log(vslLog.ERROR, "Error trying to read vslMMBackend database: " + 
					e.toString());
			throw new vslStorageException(e);
		}
	}
	

	/* --------------- PRIVATE UTILITY METHODS ------------ */

	private vslID genID()
	{
		Integer seed = new Integer(randGen.nextInt());
		vslID id = new vslID();
		id.setID(seed.toString());
		return id;
	}


}
