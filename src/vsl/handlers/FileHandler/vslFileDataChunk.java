package vsl.handlers.FileHandler;

import vsl.core.types.vslDate;
import vsl.core.types.vslID;
import vsl.core.vslChunk;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class vslFileDataChunk extends vslChunk implements Comparable<vslFileDataChunk>
	//implements Serializable, Comparable<vslFileDataChunk>
{

	vslFileChunkExtra extra = new vslFileChunkExtra();
	



	public vslFileDataChunk(int num, int token)
	{
		extra.chunkNum = num;
		//extra.chunkSize = len;
		extra.tokenSize = token;
		//chunkData = new byte[chunkSize];
		extra.beginToken = new byte[extra.tokenSize];	
		extra.endToken = new byte[extra.tokenSize];	
	}

	public vslFileDataChunk(int num, int token, byte[] data, int offset, int len)
	{
		extra.chunkNum = num;
		//chunkSize = len;
		extra.tokenSize = token;
		setData(data);
		setTokens();
	}


	public int compareTo(vslFileDataChunk other)
	{
		return extra.chunkNum - other.getChunkNum();
	}

	/* beyond here trivial getters/setters */


	/* ----------------- PRIVATE -------------------- */

	private void setTokens()
	{
		if (getData() != null && extra.tokenSize > 0)
		{
			extra.beginToken = new byte[extra.tokenSize];
			extra.endToken = new byte[extra.tokenSize];
			System.arraycopy(extra.beginToken, 0, getData(), 0, extra.tokenSize);
			System.arraycopy(extra.endToken, 0, getData(), 
					getData().length - extra.tokenSize, extra.tokenSize);
		}
		else
		{
			//log error
		}
	}


	/* ----------------- GETTERS/SETTERS -------------------- */

	public int getTokenSize()
	{
		return extra.tokenSize;
	}

	public byte[] getBeginToken()
	{
		return extra.beginToken;
	}
	
	public byte[] getEndToken()
	{
		return extra.endToken;
	}


	public int getChunkNum()
	{
		return extra.chunkNum;
	}




	/**
	 * FROM BEFORE IMPLEMENTING vslChunk:
	 *
	 *
	 
	private vslID id;

	private byte[] chunkData;

	private byte[] beginToken;
	private byte[] endToken;

	private vslDate timeStamp;
	
	private byte[] chunkDigest;
	

	public int getLength()
	{
		return chunkSize;
	}
	
	public byte[] getData()
	{
		return chunkData;
	}

	/**
	 * Set the data for this chunk.  
	 *
	 * @param 	len		number of bytes set in data.
	 * @param	data	a non-null array of byte data.
	 *
	 * @return 	-1 on error (data null or too long), else 0.
	 *
	public int setData(int len, byte[] data)
	{
		if (data == null)
		{
			//Log error 
			return -1;
		}
		chunkData = new byte[len];
		System.arraycopy(data, 0, chunkData,  0, len);
		genDigest();
		return 0;
	}
	
	private void genDigest()
	{
		try {
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			//digest.update(chunkData);
			//byte[] hash = digest.digest();
			//return hash;
			chunkDigest = digest.digest(getData());
		} catch (NoSuchAlgorithmException e) {
			// Log error!!
			System.err.println("vslFileDataChunk: " + e.toString());
		}
	}

	
	public byte[] getDigest()
	{
		return chunkDigest;
	}

	 /*
	 */

}
