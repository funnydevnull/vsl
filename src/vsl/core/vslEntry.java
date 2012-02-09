package vsl.core;

import java.util.Vector;
import java.util.Iterator;

import vsl.core.types.vslDate;
import vsl.core.types.vslHash;
import vsl.core.types.vslID;

import vsl.core.data.vslBackendData;
import vsl.core.data.vslVersionHeader;

import vsl.core.vslChunk;

public class vslEntry {

	private vslID id;

	private Vector<vslVersion> versions = new Vector<vslVersion>();
	

	/* ----------------- Constructors ----------------------*/


	/**
	 * Load an existing entry from the backend using its vslID.
	 */
	public vslEntry(vslID entryID) 
		throws vslStorageException
	{
		id = entryID;
		load();
	}
	
	
	public vslEntry(vslDataType newEntry) {
		vslLog.log(vslLog.DEBUG, "Enter: vslEntry(vslDataType)");
		vslVersion newver = new vslVersion(null, null, newEntry.getNewChunks());
		versions.add(newver);
		vslLog.log(vslLog.DEBUG, "Exit: vslEntry(vslDataType)");
	}


	public void addVersion(vslDataType newEntry, Vector<vslVersion> prev) {
		vslVersion newver = new vslVersion(prev, null, newEntry.getNewChunks());
		versions.add(newver);
	}

	/* ------------- STORAGE INTERFACE ----------------- */     

	/**
	 * Load this entry from the backend.
	 */
	void load()
		throws vslStorageException
	{
	    Vector temp = null;	   
	    vslFuture res = vsl.load(id);
	    if (!res.success()) {
			// failed to load id, raise exception
			vslLog.log(vslLog.ERROR, "failed to load id " + id);
			return;
	    }		
	    if(res.awaitUninterruptedly()) {	      
			temp = res.getEntries();
	    }
	    Iterator vh_itr = temp.iterator();
	    while(vh_itr.hasNext()) {
		vslVersionHeader vh = (vslVersionHeader) vh_itr.next();
		Vector<vslVersion> prev = new Vector<vslVersion>();
		// use null prev for now...
		vslVersion v = new vslVersion(prev, vh.id);
		// load up data for that version from the backend
		versions.add(v);
	    }
	}


	/**
	 * Store this entry in the backend returning "true" if successful.
	 */
	void store()
		throws vslStorageException
	{
		Vector<vslVersionHeader> versionHeaders = new Vector<vslVersionHeader>();
		versionHeaders = storeNewVersions();
		if (!versionsStored())
		{
			vslLog.log(0, "Attempt to store Entry without first storing versions.");
			vslStorageException vse = 
				new vslStorageException("Trying to store Entry with unstored Versions.");
			throw vse;
		}
		/*Vector<vslVersionHeader> versionHeaders = new Vector<vslVersionHeader>();
		for(vslVersion ver: versions)
		{
			versionHeaders.add(ver.getHeader());
			}*/
		// If we don't already have an id in the backend then use create() otherwise
		// use add()
		if (id == null) {
			vslFuture res = vsl.create(versionHeaders);
			if(res.awaitUninterruptedly()) {
			       id = res.getNewEntryID();
			} else {
				vslLog.log(0, "Error storing Entry: " + res.getErrMsg());
				throw new vslStorageException(res.getErrMsg());
		       }
		} else {	
			vsl.add(id, versionHeaders);
		}
	}


	/**
	 * Stores all the data assocated with any new version as well as the version info.
	 * returns a vector of versionHeaders for new versions
	 */
	private Vector<vslVersionHeader> storeNewVersions()
		throws vslStorageException
	{
	        Vector<vslVersionHeader> vh = new Vector<vslVersionHeader>();
		for (vslVersion ver: versions)
		{
		    if (ver.getID() == null)
			{
				ver.store();
				vh.add(ver.getHeader());
			}
		}
	        return vh;
	}


	/* --------------- PRIVATE Utility methods -----------------*/


	/**
	 * Returns true if all the versions already have vslIDs (i.e. they've been
	 * stored in the backend).
	 */
	private boolean versionsStored()
	{
		for(vslVersion ver: versions) {
			vslID vID = ver.getID();
			if (vID == null || !vID.isValid())
			{
				return false;
			}
		}
		return true;
	}



	/**
	 * Created new Entry's data using the chunks from the passed data
	 * object.  This will create only a single version with the passed data.
	 */
	private void newEntryFromData(vslDataType newData)
		throws vslException
	{
		vslLog.log(vslLog.DEBUG, "Enter: newEntryFromData()");
		// we are creating new entry so this should be empty
		if (!versions.isEmpty())
		{
			vslLog.log(vslLog.ERROR, 
					"Attempting to initilize entry which already has versions.");
			throw new vslException(
					"Attempting to initialize vslEntry which already has versions.");
		}
		vslVersion newver = new vslVersion(null, null, newData.getNewChunks());
		versions.add(newver);
		vslLog.log(vslLog.DEBUG, "Exit: newEntryFromData()");
	}





	/* ------ Only getters/setters past here -------------- */

	public vslID getID() {
		return id;
	}

}
