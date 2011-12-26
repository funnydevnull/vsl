package vsl.core;

import java.io.Serializable;
import java.nio.ByteBuffer;

import vsl.core.types.*;

import vsl.core.data.vslChunkHeader;
import vsl.core.data.vslChunkHeaderExtra;
import vsl.core.data.vslChunkData;
import vsl.core.data.vslChunkDataExtra;

public class vslChunk {

	private vslID		id;
	private vslHash 	hash;
	private vslDate		createTime;
	private vslChunkHeaderExtra headerExtra = null;
	private vslChunkDataExtra dataExtra = null;
	private ByteBuffer 	data = null;
	
	public vslChunk() {
	}
	
	
	public vslChunk(byte[] d) {
		data = ByteBuffer.allocate(d.length);
		data.put(d);
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
		this.data = ByteBuffer.allocate(chunkData.data.length);
		this.data.put(chunkData.data);
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
			chunk.data = data.array();
		}
		return chunk;
	}
	



	/* ------------------ GETTERS/SETTERS ------------------------ */


	public byte[] getData() {
		// for testing purposes right now
		return data.array();
	}
	
	public void setData(byte[] newData) {
		data.put(newData);
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

}
