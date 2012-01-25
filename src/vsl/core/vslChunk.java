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


public class vslChunk {

	vslID		id;
	private vslHash 	hash;
	private vslDate		createTime;
	private vslChunkHeaderExtra headerExtra = null;
	private vslChunkDataExtra dataExtra = null;
	private byte[] 	data = null;

	/* should this be a vslHash?? */
	private byte[] chunkDigest;
	
	public vslChunk() {
	}
	
	
	public vslChunk(byte[] d) {
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
	vslChunk(vslChunkHeader header, vslChunkData chunkData)
	{
		this.id = header.id;
		this.hash = chunkData.hash;
		this.createTime = chunkData.createTime;
		this.dataExtra = chunkData.extra;
		setData(chunkData.data);
	}
	

	/* -------------- STORAGE INTERFACE ---------------------------- */


	/**
	 * Return a vslChunkHeader representation of this chunk used to store a
	 * "summary" of the chunk in the "version" entry in the backend.
	 */
	public vslChunkHeader getChunkHeader()
	{
		vslChunkHeader header = new vslChunkHeader();
		header.id = this.id;
		header.hash = this.hash;
		header.createTime = this.createTime;
		header.extra = this.headerExtra;
		return header;
	}
	

	void store()
		throws vslStorageException
	{
		vslFuture res = vsl.create(toChunkData());
		if(res.awaitUninterruptedly())
		{
			id = res.getNewEntryID();
		}
		else
		{
			vslLog.log(0, "Error storing Chunk: " + res.getErrMsg());
			throw new vslStorageException(res.getErrMsg());
		}
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
