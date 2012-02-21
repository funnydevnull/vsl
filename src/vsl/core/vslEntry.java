package vsl.core;

import java.util.HashMap;
import java.util.Vector;
import java.util.Iterator;

import vsl.core.types.vslDate;
import vsl.core.types.vslHash;
import vsl.core.types.vslID;

import vsl.core.data.vslBackendData;
import vsl.core.data.vslVersionHeader;

import vsl.core.vslChunk;


/**
 * A vslEntry represents an object, such as a file, or a message, or some other
 * type of data, that we want to version and store in the backend.  An entry
 * can either be created to represent a new object that we will store in the
 * backend (using the constructor vslEntry(vslDataType)) or to represent an
 * existing object in the backend that we want to update (via the construtor
 * vslEntry(vslID)).
 *
 */
public class vslEntry {

	private vslID id;

	/**
	 * We store new versions (not yet in the backend) here until they have a
	 * vslID AND the associated vslVersionHeaders have been stored in the
	 * entry.  Only then do they get shifted to the existingVersions.
	 */
	private Vector<vslVersion> newVersions = new Vector<vslVersion>();

	/**
	 * This is where we store versions that already have a vslID, keyed by the id.
	 * Once a version gets stored and its versionHeader is saved in the entry
	 * it _should_ be moved from newVersions to here.
	 */
	private HashMap<vslID, vslVersion> existingVersions = null;


	/* some helpful values to keep around so we don't have to keep redetermining them. */
	private vslVersion firstVersion = null;
	
	
	
	/* ----------------- Constructors ----------------------*/


	/**
	 * Load an existing entry from the backend using its vslID.
	 *
	 * @throws	vslStorageException		If there is a problem in the backend or with the consistency of the data.
	 *
	 * @throws	vslInputException		If the entryID is invalid or null.
	 */
	public vslEntry(vslID entryID) 
		throws vslStorageException, vslInputException
	{
		if (entryID == null || !entryID.isValid())
		{
			vslLog.log(vslLog.ERROR, "Attempting to initalize entry with null or invalid ID: " + entryID);
			throw new vslInputException("Attempting to initalize entry with null or invalid ID: " + entryID);
		}
		id = entryID;
		load();
	}
	
	/**
	 * Create a brand new entry initialized with the data in newEntry.
	 *
	 * @param	newEntry	A subclass of vslDataType with the chunks to
	 * initialize this entry with.  Only the NewChunks of this vslDataType are
	 * used.
	 */
	public vslEntry(vslDataType newEntry) {
		vslLog.log(vslLog.DEBUG, "Enter: vslEntry(vslDataType)");
		vslVersion newver = new vslVersion(null, null, newEntry.getNewChunks());
		newVersions.add(newver);
		firstVersion = newver;
		vslLog.log(vslLog.DEBUG, "Exit: vslEntry(vslDataType)");
	}


	/**
	 * Add a new version to this entry.
	 *
	 * @param	newData	A subclass of vslDataType containing the previous
	 * version id, old chunks associated with this version and new chunks
	 * associated with this version.
	 */
	public void addVersion(vslDataType newData) 
		throws vslInputException
	{
		Vector<vslID> prevIDs = newData.prevVersions();
		if (prevIDs == null)
		{
			vslLog.log(vslLog.ERROR, 
					"Attempt to populate new version of Entry [" + id +
					"] without specifying old version ids.");
			throw new vslInputException(
					"Attempt to populate new version of Entry [" + id +
					"] without specifying old version ids.");
		}
		Vector<vslVersion> prev = new Vector<vslVersion>();
		for (vslID vID: prevIDs) {
			vslVersion ver = versionFromID(vID);
			if (ver == null)
			{
				vslLog.log(vslLog.ERROR, "Could not find version with id: " + vID);
				throw new vslInputException(
							"Could not find version with id: " + vID);
			}
			prev.add(ver);
		}
		vslVersion newver = new vslVersion(prev, 
				newData.getOldChunks(), newData.getNewChunks());
		newVersions.add(newver);
	}



	/**
	 * Return a version by its version id.  <br>
	 * <br>
	 * <em>NOTE: this version is not necassarily populated.  To populate or
	 * check use vslVersion.loadChunkHeaders() vslVersion.headersLoaded(),
	 * repsectively. </em>
	 *
	 * @param	verID	A valid version ID for a version associated with this entry.
	 *
	 * @return	The version with verison ID 'verID'.
	 *
	 * @throws	vslInputException	If 'verID' is incorrectly formatted or does not match a version in this Entry.
	 */
	public vslVersion getVersion(vslID verID) 
		throws vslInputException
	{
		if (verID == null || !verID.isValid())
		{
			vslLog.log(vslLog.ERROR, "Attempting to retreive version with null or invalid ID: " + verID);
			throw new vslInputException("Attempting to retreive version with null or invalid ID: " + verID);
		}
		vslVersion ver = existingVersions.get(verID);
		if (ver == null)
		{
			vslLog.log(vslLog.ERROR, "Could not find version with ID [" + verID + "] in entry: [" + id +"]");
			throw new vslInputException("Could not find version with ID [" + verID + "] in entry: [" + id +"]");
		}
		return ver;
	}
	
	/**
	 * Return the first version.  This can be used to iterate down the version
	 * tree via calls to vslVersion.getNext() and getPrev().<br>
	 * <br>
	 * <em>NOTE: this version is not necassarily populated.  To populate or
	 * check use vslVersion.loadChunkHeaders() vslVersion.headersLoaded(),
	 * repsectively. </em>
	 *
	 * @return	The first version associated with this entry.
	 *
	 * @throws	vslConsistencyException	If the entry data is inconsistent and has more than one first version.
	 */
	public vslVersion getFirstVersion()
		throws vslConsistencyException
	{
		if (firstVersion != null) return firstVersion;
		// if the existing HashMap is non-empty then the version should be in there
		if (existingVersions != null) {
			for (vslVersion ev: existingVersions.values()) 
			{
				// a version with a null previous should be the first version
				if (ev.getPrev() == null || ev.getPrev().size() == 0 ) {
					// if firstVersion != null then we found a first version
					// already and the version list is corrupted somhow so
					// throw an exception
					if (firstVersion != null) {
						vslLog.log(vslLog.ERROR, 
						"Data Inconsistency: Entry [" + id + "] has more than one version with null prev: [" +
								firstVersion.getID() + "], [" + ev.getID() + "]");
						throw new vslConsistencyException(
						"Data Inconsistency: Entry [" + id + "] has more than one version with null prev: [" +
								firstVersion.getID() + "], [" + ev.getID() + "]");
					}
					firstVersion = ev;
				}
			}
			if (firstVersion == null) {
				vslLog.log(vslLog.ERROR, 
					"Could not find firstVersion in existing entry: [" + id + "].  This should not happen!");
				throw new vslConsistencyException(
					"Could not find firstVersion in existing entry: [" + id + "].  This should not happen!");
			}
			return firstVersion;
		}
		// if there's no existing version then in principle we should have a
		// firstVersion set already in the constructor so we throw an error
		// here
		else
		{
			vslLog.log(vslLog.ERROR, 
					"Could not find firstVersion in newly created entry.  This should not happen!");
			throw new vslConsistencyException(
					"Could not find firstVersion in newly created entry.  This should not happen!");
		}
	}


	/* ------------- STORAGE INTERFACE ----------------- */     


	/**
	 * Load this entry from the backend.
	 */
	void load()
		throws vslStorageException, vslInputException
	{
	    Vector<vslVersionHeader> headers = null;	   
	    vslFuture res = vsl.load(id);
	    if(res.awaitUninterruptedly().success()) {	      
			headers = (Vector<vslVersionHeader>) res.getEntries();
	    }
		else
		{
			// failed to load id, raise exception
			vslLog.log(vslLog.ERROR, "failed to load id " + id);
			throw new vslStorageException("Could not find Entry with id: " + id);
	    }
		existingVersions = vslVersion.versionsFromHeaders(headers);
	}


	/**
	 * Store this entry in the backend returning "true" if successful.
	 */
	void store()
		throws vslStorageException
	{
		Vector<vslVersionHeader> versionHeaders = new Vector<vslVersionHeader>();
		versionHeaders = storeNewVersions();
		// make sure versions all got stored
		if (!versionsStored())
		{
			vslLog.log(vslLog.ERROR, "Attempt to store Entry without first storing versions.");
			vslStorageException vse = 
				new vslStorageException("Trying to store Entry with unstored Versions.");
			throw vse;
		}
		vslFuture res = null;
		// If we don't already have an id in the backend then use create() otherwise
		// use add()
		if (id == null) 
		{
			res = vsl.create(versionHeaders);
			if(res.awaitUninterruptedly().success()) {
			       id = res.getNewEntryID();
			} else {
				vslLog.log(vslLog.ERROR, "Error creating new Entry: " + res.getErrMsg());
				throw new vslStorageException(
						"Error creating new entry: " + res.getErrMsg());
		    }
		} else {	
			res = vsl.add(id, versionHeaders);
			if(!res.awaitUninterruptedly().success()) {
				vslLog.log(vslLog.ERROR, "Error updating Entry [" + id + "]: " + 
						res.getErrMsg());
				throw new vslStorageException("Error updating Entry [" + id + "]: " + 
						res.getErrMsg());
		    }
		}
		// since this entry might have just been setup we might not yet have an
		// existingVersions hashtable
		if (existingVersions == null) {
			existingVersions = new HashMap<vslID, vslVersion>();
		}
		// if we got here we should be successful
		// so we move all the newVersions to the existingVersions
		for (vslVersion v: newVersions)
		{
			// we already checked above that IDs are valid and not-null
			existingVersions.put(v.getID(), v);
		}
		newVersions.clear();
	}


	/**
	 * Stores all the data assocated with any new version as well as the
	 * version info.  Note after being stored versions are still kept in
	 * newVersions until the the versionHeaders themselves are stored.
	 *
	 * @return	A vector of versionHeaders for new versions.
	 */
	private Vector<vslVersionHeader> storeNewVersions()
		throws vslStorageException
	{
		Vector<vslVersionHeader> vh = new Vector<vslVersionHeader>();
		for (vslVersion ver: newVersions)
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
		for(vslVersion ver: newVersions) {
			vslID vID = ver.getID();
			if (vID == null || !vID.isValid())
			{
				return false;
			}
		}
		return true;
	}


	/**
	 * Returns an entry from the version list keyed off its vsl id.
	 */
	private vslVersion versionFromID(vslID id)
	{
		if (existingVersions != null) {
			return existingVersions.get(id);
		}
		else {
			return null;
		}
		/*
		vslVersion ver = null;
		// first check if we've cached the result
		if ( (ver == versionLookup.get(id)) != null) return ver;
		for(vslVersion ver: versions) {
			vslID vID = ver.getID();
			if (vID == null && vID.equals(id) )
			{
				// store the result for next time
				versionLookup.put(id, ver);
				return ver;
			}
		}
		return null;
		*/
	}



	/* ------ Only getters/setters past here -------------- */

	public vslID getID() {
		return id;
	}

}
