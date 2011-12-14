package vsl.handlers.FileHandler;

import vsl.core.vslDate;
import vsl.core.vslID;

import java.io.Serializable;
import java.security.MessageDigest;

public class vslFileDataChunk implements Serializable, Comparable<vslFileDataChunk>
	//extends vslDataChunk
{

	private vslID id;

	private int chunkNum = -1;
	private int chunkSize = -1;
	private int tokenSize = -1;

	private byte[] chunkData;


	private byte[] beginToken;
	private byte[] endToken;

	private vslDate timeStamp;



	public vslFileDataChunk(int num, int len, int token)
	{
		chunkNum = num;
		chunkSize = len;
		tokenSize = token;
		chunkData = new byte[chunkSize];
		beginToken = new byte[tokenSize];	
		endToken = new byte[tokenSize];	
	}

	public vslFileDataChunk(int num, int len, int token, byte[] data)
	{
		chunkNum = num;
		chunkSize = len;
		tokenSize = token;
		setData(len, data);
		setTokens();
	}


	public int compareTo(vslFileDataChunk other)
	{
		return this.chunkNum - other.getChunkNum();
	}

	/* beyond here trivial getters/setters */

	/**
	 * Set the data for this chunk.  
	 *
	 * @param 	len		number of bytes set in data.
	 * @param	data	a non-null array of byte data.
	 *
	 * @return 	-1 on error (data null or too long), else 0.
	 */
	public int setData(int len, byte[] data)
	{
		if (data == null)
		{
			//Log error 
			return -1;
		}
		chunkData = new byte[len];
		System.arraycopy(data, 0, chunkData,  0, len);
		return 0;
	}


	private void setTokens()
	{
		if (chunkData != null && tokenSize > 0)
		{
			beginToken = new byte[tokenSize];
			endToken = new byte[tokenSize];
			System.arraycopy(beginToken, 0, chunkData, 0, tokenSize);
			System.arraycopy(endToken, 0, chunkData, 
					chunkData.length - tokenSize, tokenSize);
		}
		else
		{
			//log error
		}
	}

/*
	private byte[] genMD5()
	{
		MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
		//digest.update(chunkData);
		//byte[] hash = digest.digest();
		//return hash;
		return digest.digest(chunkData);
	}
*/

	public byte[] getBeginToken()
	{
		return beginToken;
	}
	
	public byte[] getEndToken()
	{
		return endToken;
	}

	public int getLength()
	{
		return chunkSize;
	}

	public int getChunkNum()
	{
		return chunkNum;
	}

	public byte[] getData()
	{
		return chunkData;
	}
}
