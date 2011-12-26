package vsl.core;

import java.util.Vector;

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
	 * Create a version object corresponding to an existing version (in backend).
	 */
	public vslVersion(Vector<vslVersion> prev, vslID id)
	{
		this.prev.addAll(prev);
		this.id = id;
	}

	
	/* ---------------- STORAGE METHODS ---------------------- */

	
	/**
	 * Store the version in the backend.  Should only be called after the
	 * version chunks have been stored otherwise an exception is thrown.  An
	 * exception is also thrown if the version has a vslID indicating that it
	 * has already been stored.
	 */
	public void store()
		throws vslStorageException
	{
		storeNewChunks();
		if (!newChunksStored())
		{
			vslLog.log(0, "Attempt to store Version without first storing new chunks.");
			vslStorageException vse = 
				new vslStorageException("Trying to store Versions with unstored chunks.");
			throw vse;
		}
		if (id != null)
		{
			vslLog.log(0, "Attempt to store a version with an existing vslID: " + id);
			vslStorageException vse = 
				new vslStorageException(
						"Trying to store already stored version with vslID: " + id);
			throw vse;

		}
		Vector<vslChunkHeader> chunkHeaders = new Vector<vslChunkHeader>();
		if (oldChunks != null) 
		{
			for (vslChunk chunk: oldChunks)
			{
				chunkHeaders.add(chunk.getChunkHeader());
			}
		}
		if (newChunks != null) 
		{
			for (vslChunk chunk: newChunks)
			{
				chunkHeaders.add(chunk.getChunkHeader());
			}
		}
		vslFuture res = vsl.create(chunkHeaders);
		if(res.awaitUninterruptedly())
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


	/* ----------------- Utility Methods -------------------- */

	/**
	 * Return a header to store as part of the Entry in the backend that
	 * encodes this version (e.g. ID, previous versions, timestamp and hash).
	 * This header knows nothing about the chunks.
	 */
	public vslVersionHeader getHeader()
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





	/* ---------------- GETTERS/SETTERS ------------------- */



	public vslID getID() {
		return id;
	}

}
