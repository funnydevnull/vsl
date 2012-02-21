package vsl.core;

import java.util.Vector;
import java.util.HashMap;

import vsl.core.types.vslID;
import vsl.core.types.vslHash;
import vsl.core.types.vslDate;

import vsl.core.data.vslVersionHeader;
import vsl.core.data.vslChunkHeader;

public class vslVersion {

	/* -------------- PRIVATE MEMBERS ---------------------- */

	private vslID id = null;

	private vslHash versionHash = null;

	// chunks that already are stored in DB
	private Vector<vslChunk> oldChunks = new Vector<vslChunk>();
	// chunks that haven't yet been written to DB
	private Vector<vslChunk> newChunks = new Vector<vslChunk>();

	// creation time
	private vslDate createTime = null;

	/**
	 * An array of versions that this version extends.  This should generally
	 * contain only one element unless this chunk is a merge of one or more
	 * previous versions.
	 */
	private Vector<vslVersion> prev = new Vector<vslVersion>();
	/**
	 * The next versions in the tree.  All these versions should have this
	 * version in their prev list.
	 */
	private Vector<vslVersion> next = new Vector<vslVersion>();

	// whether or not this version has already been stored in the backend
	private boolean newVersion = true;


	/** 
	 * whether or not the chunkHeaders for this version has already beed loaded
	 * from the backend only should be set when newVersion == false.
	 */
	private boolean chunkHeadersLoaded = false;
	/** 
	 * whether or not the data for old and new Chunks assocaited with this
	 * version has already beed loaded from the backend. Only should be set
	 * when newVersion == false.
	 */
	private boolean newChunkDataLoaded = false;
	private boolean oldChunkDataLoaded = false;




	/* ---------------- CONSTRUCTORS -----------------------*/

	/**
	 * Create a Version object corresponding to a new version (not yet in
	 * backend).  This version extends 'oldVer' and with new data 'unsaved' and
	 * old data 'saved'.
	 *
	 * @param	oldVer		The version just before this chunk.  If
	 * 						oldVer.length > 1 then this version is a 
	 * 						merge of two old versions.
	 *
	 * @param	saved		Chunks associated with this entry (and referenced
	 *					 	by this version) that are already saved in the DB.
	 *
	 * @param	unsaved		New chunks that we will create for this version and
	 *						that do not already exist in the backend.
	 */
	public vslVersion(Vector<vslVersion> prev, Vector<? extends vslChunk> oldChunks, 
							Vector<? extends vslChunk> newChunks) 
	{
		if (prev != null)
		{
			this.prev.addAll(prev);
		}
		if (oldChunks != null)
		{
			this.oldChunks.addAll(oldChunks);
		}
		if (newChunks != null)
		{
			this.newChunks.addAll(newChunks);
		}
		createTime = new vslDate();
	}

	/**
	 * Create a version object corresponding to an existing version (in
	 * backend).  This constructor is private since it doesn't ensure the
	 * prev/next versions are populated.  Rather the factory method
	 * versionsFromHeaders should be used.
	 */
	private vslVersion(vslVersionHeader header)
	{
		this.id = header.id;
		this.createTime = header.createTime;
		this.versionHash = header.hash;
		newVersion = false;
	}


	/* --------------- FACTORY METHODS --------------------- */


	/**
	 * Populate an vslVesion hashmap (keyed off the vslID) of existing versions
	 * using their vslVesionHeaders (from the backend).  The main role of this
	 * function is to ensure that the created version objects have propertly
	 * populated "prev" and "next" vectors as the former are essential to data
	 * integrity.  For this reason <em>pre-existing versions should always be
	 * generated using this factory method.</em>.
	 *
	 * @param	headers	A vec of populated version Headers associated with an
	 * entry in the backend.
	 *
	 * @return	A map of version objects, with properlty popualted next/prev
	 * entries, keyed off their id.
	 *
	 */
	public static HashMap<vslID, vslVersion> 
		versionsFromHeaders(Vector<vslVersionHeader> headers)
			throws vslInputException
	{
		HashMap<vslID, vslVersion> versionLookup = new HashMap<vslID, vslVersion>();
		// create all version
		for (vslVersionHeader vh: headers) 
		{
			vslVersion v = new vslVersion(vh);
			versionLookup.put(vh.id, v);
		}
		// we loop again once all versions created to set next/prev
		for (vslVersionHeader vh: headers) 
		{
			vslVersion curVer = versionLookup.get(vh.id);
			Vector<vslVersion> prev = new Vector<vslVersion>();
			// generate the vec of prev versions and simultaneously add
			// curVer to each prev version as a "next"
			for(vslID pid: vh.prevID) {
				vslVersion pv = versionLookup.get(pid);
				if (pv == null)
				{
					vslLog.log(vslLog.ERROR, 
							"Could not find prev version with id [" + pid + 
							"] while populating version [" + vh.id + "]");
					throw new vslInputException(
							"Could not find prev version with id [" + pid + 
							"] while populating version [" + vh.id + "]");
				}	
				prev.add(pv);
				// we set that we're a next version for this 
				pv.addNext(curVer);
			}
			// store the prev version
			curVer.setPrev(prev);
		}
		return versionLookup;
	}


	/* ---------------- STORAGE METHODS ---------------------- */

	
	/**
	 * Store the version in the backend.  Should only be called after the
	 * version chunks have been stored otherwise an exception is thrown.  An
	 * exception is also thrown if the version has a vslID indicating that it
	 * has already been stored.
	 *
	 * @throws vslStorageException	If can't store the chunks associated with
	 * this version or the version itself or if the the version has already
	 * been stored.
	 */
	public void store()
		throws vslStorageException
	{
		if (id != null)
		{
			vslLog.log(vslLog.ERROR, "Attempt to store a version with an existing vslID: " + id);
			vslStorageException vse = 
				new vslStorageException(
						"Trying to store already stored version with vslID: " + id);
			throw vse;

		}
		storeNewChunks();
		if (!newChunksStored())
		{
			vslLog.log(vslLog.ERROR, "Attempt to store Version without first storing new chunks.");
			vslStorageException vse = 
				new vslStorageException("Trying to store Versions with unstored chunks.");
			throw vse;
		}
		Vector<vslChunkHeader> chunkHeaders = new Vector<vslChunkHeader>();
		if (oldChunks != null) 
		{
			for (vslChunk chunk: oldChunks)
			{
				vslChunkHeader oldHead = chunk.getChunkHeader();
				oldHead.createdInVersion = false;
				chunkHeaders.add(oldHead);
			}
		}
		if (newChunks != null) 
		{
			for (vslChunk chunk: newChunks)
			{
				vslChunkHeader newHead = chunk.getChunkHeader();
				newHead.createdInVersion = true;
				chunkHeaders.add(newHead);
			}
		}
		vslFuture res = vsl.create(chunkHeaders);
		if(res.awaitUninterruptedly().success())
		{
			id = res.getNewEntryID();
		}
		else
		{
			vslLog.log(0, "Error storing Version: " + res.getErrMsg());
			throw new vslStorageException(res.getErrMsg());
		}
	}


	/**
	 * Store the new chunks associated with this version in the backend.
	 */
	private void storeNewChunks()
		throws vslStorageException
	{
		for (vslChunk chunk: newChunks)
		{
			chunk.store();
		}
	}

	/**
	 * Load the chunkHeaders associated with this vslVersion from the backend.
	 * This method should only be called once for each instance.  If called
	 * again it will do nothing.
	 *
	 * @throws	vslInputException	If the version has not yet been stored in the backend.
	 *
	 * @throws	vslStorageException	If an entry with this version's ID cannot be found in the backend.
	 */
	public void loadChunkHeaders()
		throws vslStorageException, vslInputException
	{
		if (chunkHeadersLoaded) {
			vslLog.log(vslLog.WARNING, "Calling loadChunkHeaders() on loaded version, doing nothing.");
		}
		if (id == null) {
			vslLog.log(vslLog.ERROR, "Calling loadChunkHeaders() on a vslVersion with null ID");
			throw new vslInputException("Calling loadChunkHeaders() on a vslVersion with null ID");
		}
	    Vector<vslChunkHeader> headers = null;	   
	    vslFuture res = vsl.load(id);
	    if(res.awaitUninterruptedly().success()) {	      
			headers = (Vector<vslChunkHeader>) res.getEntries();
	    }
		else
		{
			// failed to load id, raise exception
			vslLog.log(vslLog.ERROR, "failed to load vslVersion with id " + id);
			throw new vslStorageException("Could not find vslVersion with id: " + id);
	    }
		populateChunks(headers);
		// set so we don't try to reload 
		chunkHeadersLoaded = true;
	}



	/**
	 * Load the data for all chunks associated with this vslVersion from the backend.
	 * This method should only be called once for each instance.  If called
	 * again it will do nothing.
	 *
	 * @param	newDataOnly		By default we load the data for all chunks but
	 * if newDataOnly is true then we only load the data for new chunks created
	 * in this version.
	 *
	 * @throws	vslInputException	If the version has not yet been stored in the backend.
	 *
	 * @throws	vslStorageException	If an entry with this version's ID cannot be found in the backend.
	 */
	void loadChunkData(boolean newDataOnly)
		throws vslStorageException, vslInputException
	{
		if (newChunkDataLoaded && newDataOnly) {
				vslLog.log(vslLog.WARNING, 
				"Calling loadChunkData() for new data when the latter already loaded. Doing nothing.");
		}
		if (id == null) {
			vslLog.log(vslLog.ERROR, "Calling loadChunkData() on a vslVersion with null ID");
			throw new vslInputException("Calling loadChunkData() on a vslVersion with null ID");
		}
		if (!chunkHeadersLoaded) {
			loadChunkHeaders();
		}
		if (!newDataOnly) {
			if (oldChunkDataLoaded) {
				vslLog.log(vslLog.WARNING, 
				"Calling loadChunkData() with newDataOnly==false when oldChunkData " +
				" already loaded.  Doing nothing");
			}
			else
			{
				for (vslChunk old: oldChunks) {
					old.load();
				}
			}
		}
		for (vslChunk nc: newChunks) {
			nc.load();
		}
	}

	
	/* ----------------- Utility Methods -------------------- */


	/**
	 * Return a header to store as part of the Entry in the backend that
	 * encodes this version (e.g. ID, previous versions, timestamp and hash).
	 * This header knows nothing about the chunks.
	 */
	vslVersionHeader getHeader()
	{
		vslVersionHeader header = new vslVersionHeader();
		header.id = this.id;
		header.hash = this.versionHash;
		Vector<vslID> prevVer = new Vector<vslID>();
		if (prev != null)
		{
			header.prevID = new vslID[prev.size()];
			int i = 0;
			for (vslVersion ver: prev) 
			{
				header.prevID[i++] = ver.getID();
			}
		}
		header.createTime = this.createTime;
		return header;
	}

	public boolean isNew() {
		return this.newVersion;
	}


	/**
	 * Returns true if all the versions already have vslIDs (i.e. they've been
	 * stored in the backend).
	 */
	private boolean newChunksStored()
	{
		for(vslChunk chunk: newChunks) {
			vslID vID = chunk.getID();
			if (vID == null || !vID.isValid())
			{
				return false;
			}
		}
		return true;
	}


	/**
	 * Populate this versions old and new chunks from a Vector of headers retreived from the backend.
	 *
	 * @param	headers		A vector of vslChunkHeaders representing the data
	 * in the backend associated with this version.
	 */
	private void populateChunks(Vector<vslChunkHeader> headers)
	{
		for (vslChunkHeader ch: headers) 
		{
			vslChunk chunk = new vslChunk(ch);
			if (ch.createdInVersion)
			{
				newChunks.add(chunk);
			}
			else
			{
				oldChunks.add(chunk);
			}
		}
	}



	/* ---------------- GETTERS/SETTERS ------------------- */


	/**
	 * Return the vslID of this vslVersion.
	 *
	 * @return	The vslID of this version.
	 */
	public vslID getID() {
		return id;
	}


	/**
	 * Whether or not the vslChunkHeaders associated with this version are already loaded.
	 *
	 * @return	true if this chunk headers associated with this version have
	 * already been read out of the backend, else false.
	 */
	public boolean headersLoaded()
	{
		return chunkHeadersLoaded;
	}

	/**
	 * Return the versions directly preceding this version.  Note that the
	 * possibility for a version to have several "previous" versions is
	 * intended to facilitate merging.  Generally most versions will only have
	 * a single "previous".
	 *
	 * <em>Note: this returns the actual vector of "prev" versions, not a copy, so it should not be modified.</em>
	 *
	 * @return	A vector of vslVersion's direclty preceeding this one in the version tree.
	 */
	public Vector<vslVersion> getPrev()
	{
		return prev;
	}

	/**
	 * Return the versions directly following this version.  The ability for a
	 * version to be followed by multiple versions allows for branching.
	 *
	 * <em>Note: this returns the actual vector of "next" versions, not a copy, so it should not be modified.</em>
	 *
	 * @return	A vector of vslVersion's direclty following this one in the version tree.
	 */
	public Vector<vslVersion> getNext()
	{
		return next;
	}


	/**
	 * Return a chunk by its id.  <br>
	 * <br>
	 * <em>NOTE: this chunk is not necassarily populated (with its data).  To populate or
	 * check use vslChunk.load() and vslChunk.dataLoaded(),
	 * repsectively. </em>
	 *
	 * @param	chunkID	A valid chunk ID for a chunk associated with this version.
	 *
	 * @param	loadData	If true the returned chunk is garaunteed to have its chunkData loaded.
	 *
	 * @return	The chunk with chunk ID 'chunkID'.
	 *
	 * @throws	vslInputException	If 'chunkID' is incorrectly formatted or does
	 * not match a chunk in this Version.
	 *
	 * @throws	vslStorageException	If 'loadData' is true and an exception is generated loading chunks data.
	 */
	public vslChunk getChunk(vslID chunkID, boolean loadData) 
		throws vslInputException, vslStorageException
	{
		if (chunkID == null || !chunkID.isValid())
		{
			vslLog.log(vslLog.ERROR, "Attempting to retreive chunk with null or invalid ID: " + chunkID);
			throw new vslInputException("Attempting to retreive chunk with null or invalid ID: " + chunkID);
		}
		for (vslChunk oc: oldChunks) {
			if (chunkID.equals(oc.getID())) {
				if (loadData && !oc.dataLoaded()) {
					oc.load();
				}
				return oc;
			}
		}
		for (vslChunk nc: newChunks) {
			if (chunkID.equals(nc.getID())) {
				if (loadData && !nc.dataLoaded()) {
					nc.load();
				}
				return nc;
			}
		}
		vslLog.log(vslLog.ERROR, "Could not find chunk with ID [" + chunkID + "] in version: [" + id +"]");
		throw new vslInputException("Could not find chunk with ID [" + chunkID + "] in version: [" + id +"]");
	}

	/**
	 * Return the oldChunks associated with this version.  This is the actual
	 * oldChunks data associated with this Version, not a copy, so it should
	 * not be modified. <br>
	 * <br>
	 * <em>NOTE: the chunks are not necassarily populated (with their data).  To populate or
	 * check use vslChunk.load() and vslChunk.dataLoaded(),
	 * repsectively. </em>
	 *
	 * @return	A vector of vslChunks associated with this version.
	 *
	 */
	public Vector<vslChunk> getOldChunks() 
	{
		return oldChunks;
	}
	
	/**
	 * Return the newChunks associated with this version.  This is the actual
	 * newChunks data associated with this Version, not a copy, so it should
	 * not be modified. <br>
	 * <br>
	 * <em>NOTE: the chunks are not necassarily populated (with their data).  To populate or
	 * check use vslChunk.load() and vslChunk.dataLoaded(),
	 * repsectively. </em>
	 *
	 * @return	A vector of vslChunks associated with this version.
	 *
	 */
	public Vector<vslChunk> getNewChunks() 
	{
		return newChunks;
	}

	/** 
	 * Add a verison following this one.  If this method is called more than
	 * once with the same nextVer it does nothing.
	 *
	 * @param	nextVer	A version that follows this one. 
	 */
	private void addNext(vslVersion nextVer)
	{
		for(vslVersion v: next) {
			if (nextVer != null && nextVer.equals(v))
			{
				vslLog.log(vslLog.WARNING, 
						"vslVersion.addNext() called more than once for same nextVer: [" +
						v + "]-->[" + nextVer + "]");
				return;
			}
		}
		next.add(nextVer);
	}





	/* ---------------- PRIVATE GETTERS/SETTERS --------------- */

	/** 
	 * This setter should only be called internally when popualted the
	 * vslVersion from the backend.  To create a new version with a vector of
	 * prev versions the public constructor should be used instead.
	 */
	private void setPrev(Vector<vslVersion> prev)
	{
		this.prev = prev;
	}



}
