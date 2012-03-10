package vsl.core;

import java.io.Serializable;
import java.nio.ByteBuffer;

import vsl.core.types.*;

import vsl.core.data.vslChunkHeader;
import vsl.core.data.vslChunkHeaderExtra;
import vsl.core.data.vslChunkData;
import vsl.core.data.vslChunkDataExtra;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;


import java.util.Vector;

public class vslChunk {

	vslID		id;
	private vslHash 	hash;
	private vslDate		createTime;
	private vslChunkHeaderExtra headerExtra = null;
	private vslChunkDataExtra dataExtra = null;
	private byte[] 	data = null;

	/* should this be a vslHash?? */
	private byte[] chunkDigest;

	/**
	 * Whether or not the chunk's data has been loaded from the backend.
	 */
	private boolean dataLoaded = false;
	
	protected vslChunk() {
	}
	
	
	protected vslChunk(byte[] d) {
		setData(d);
		createTime = new vslDate();
	}
	
	/**
	 * Creates a chunk object representing chunkData stored in VSL's backend.
	 *
	 * @param	chunkHeader	The header item associated with this chunk (need to
	 * 						read off the extra header info).
	 *
	 * @param	chunkData	A vslChunkData element read out of the backend storage system.
	 */
	private vslChunk(vslChunkHeader header, vslChunkData chunkData)
	{
		this.id = header.id;
		this.headerExtra = header.extra;
		fromChunkData(chunkData);
		/*
		this.hash = chunkData.hash;
		this.createTime = chunkData.createTime;
		this.dataExtra = chunkData.extra;
		setData(chunkData.data);
		dataLoaded = true;
		*/
	}


	/**
	 * Creates a chunk object representing chunkData stored in VSL's backend
	 * but with only its header data populated (i.e. hash, create time, etc...
	 * but not the actual data).
	 *
	 * @param	chunkHeader	The header item associated with this chunk (need to
	 * 						read off the extra header info).
	 *
	 */
	vslChunk(vslChunkHeader header)
	{
		this.id = header.id;
		this.headerExtra = header.extra;
		this.hash = header.hash;
		this.createTime = header.createTime;
		dataLoaded = false;
	}
	

	/* -------------- STORAGE INTERFACE ---------------------------- */


	/**
	 * Return a vslChunkHeader representation of this chunk used to store a
	 * "summary" of the chunk in the "version" entry in the backend.
	 */
	vslChunkHeader getChunkHeader()
	{
		vslChunkHeader header = new vslChunkHeader();
		header.id = this.id;
		header.hash = this.hash;
		header.createTime = this.createTime;
		header.extra = this.headerExtra;
		return header;
	}
	
	/**
	 * Store this chunk in the backend and set its id to the value assigned by the backend.
	 *
	 * @throws	vslStorageException	if there's an error storing the chunk.
	 */
	void store()
		throws vslStorageException
	{
		vslFuture res = vsl.create(toChunkData());
		if(res.awaitUninterruptedly().success())
		{
			id = res.getNewEntryID();
		}
		else
		{
			vslLog.log(vslLog.ERROR, "Error storing Chunk: " + res.getErrMsg());
			throw new vslStorageException(res.getErrMsg());
		}
	}


	/**
	 * Load the chunk (i.e. the chunkData) from the backend.  This method should only be called once
	 * for each instance.  If called again it will do nothing.
	 *
	 * @throws	vslInputException	If this chunk does not have a valid id set.
	 *
	 * @throws	vslStorageException	If an entry with this chunks's ID cannot be
	 * found in the backend or if it corresponds to an entry with more than one
	 * data element.
	 */
	void load()
		throws vslStorageException, vslInputException
	{
		if (dataLoaded) {
			vslLog.log(vslLog.WARNING, "Calling vslChunk.load() on loaded chunk, doing nothing.");
		}
		if (id == null || !id.isValid() ) {
			vslLog.log(vslLog.ERROR, "Calling chunk.load() on a vslChunk with null/invalid ID: " + id);
			throw new vslInputException(
					"Calling chunk.load() on a vslChunk with null/invalid ID: " + id);
		}
	    vslFuture res = vsl.load(id);
	    if(res.awaitUninterruptedly().success()) {
			Vector<vslChunkData> dataList = (Vector<vslChunkData>) res.getEntries();
			if (dataList.size() != 1)
			{
				// failed to load id, raise exception
				vslLog.log(vslLog.ERROR, 
						"Backend has more or less than one item in this ID!. Failing.");
				throw new vslStorageException( 
						"Backend has more or less than one item in this ID!. Failing.");
			}
			fromChunkData(dataList.get(0));
	    }
		else
		{
			// failed to load id, raise exception
			vslLog.log(vslLog.ERROR, "failed to load vslChunk with id " + id);
			throw new vslStorageException("Could not find vslChunk with id: " + id);
	    }
		// set so we don't try to reload 
		dataLoaded = true;
	}

	

	/**
	 * Return a vslChunkData representation of this chunk that can be stored to
	 * the backend.
	 */
	private vslChunkData toChunkData()
	{
		vslChunkData chunk = new vslChunkData();
		chunk.hash = this.hash;
		chunk.createTime = this.createTime;
		chunk.extra = this.dataExtra;
		if (data != null)
		{
			chunk.data = data;
		}
		return chunk;
	}
	

	/**
	 * Populate this chunk using the data in chunkData.
	 *
	 * @param	chunkData	A vslChunkData containing a backend entry
	 * corresponding to a fully populated chunk.
	 */
	private void fromChunkData(vslChunkData chunkData)
	{
		this.hash = chunkData.hash;
		this.createTime = chunkData.createTime;
		this.dataExtra = chunkData.extra;
		setData(chunkData.data);
		dataLoaded = true;
	}

	/* ---------------- OVERRIDABLE METHODS ----------------------- */

	/**
	 * Generate and store the digest associated with this chunk.  Particular
	 * implementations are free to generate different types of digests.
	 *
	 */
	protected void genDigest()
		throws vslException
	{
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			//digest.update(chunkData);
			//byte[] hash = digest.digest();
			//return hash;
			chunkDigest = digest.digest(getData());
		} catch (NoSuchAlgorithmException e) {
			// Log error!!
			vslLog.log(vslLog.ERROR, "Could not find algorithm to generate digest: " + 
					e.toString());
			throw new vslException(e);
		}
	}




	/* ------------------ GETTERS/SETTERS ------------------------ */


	public byte[] getData() {
		// for testing purposes right now
		return data;
	}
	
	/**
	 * Set the data of this chunk to 'newData'.  
	 *
	 * @return	0 on success else -1 if newData is null.
	 */
	public int setData(byte[] newData) {
		if (newData == null) {
			return -1;
		}
		return setData(newData, 0, newData.length);
	}

	/**
	 * Copy 'len' bytes of data, starting at 'offset', from 'newData' into the
	 * data element of this chunk.  Data should always be set using this
	 * method, even internally in vslChunk.  This will allow us to do post/pre
	 * data setting cleaning up.
	 *
	 * @return	0 on success else -1 if newData is null or too short.
	 */
	public int setData(byte[] newData, int offset, int len) {
		if (newData == null || newData.length < offset + len) {
			return -1;
		}
		data = new byte[len];
		System.arraycopy(newData, offset, data, 0, len);
		return 0;
	}


	public vslChunkDataExtra getDataExtra() {
		// for testing purposes right now
		return dataExtra;
	}
	
	public void setDataExtra(vslChunkDataExtra extra) {
		dataExtra = extra;
	}
	/**
	 * Whether or not the vslChunkData associated with this chunk is already loaded.
	 *
	 * @return	true if the chunk data associated with this chunk has
	 * already been read out of the backend, else false.
	 */
	public boolean dataLoaded()
	{
		return dataLoaded;
	}

	public vslID getID() {
		return id;
	}


	/**
	 * @return 	The length of the byte array associated with the chunk data
	 * (not including metadata) or zero if there is no data.
	 */
	public long dataLength() {
		if (data != null) return data.length;
		return 0;
	}


	/**
	 * @return	A digest/hash associated with this chunk.
	 */
	public byte[] getDigest()
	{
		return chunkDigest;
	}

}
