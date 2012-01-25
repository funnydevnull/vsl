package vsl.handlers.FileHandler;

import vsl.core.vslDataType;

import vsl.handlers.FileHandler.vslFileDataChunk;

import java.util.Vector;

public class vslFileDataType implements vslDataType {

	String name;
	Vector<vslFileDataChunk> chunks = new Vector<vslFileDataChunk>();

	public String getName()
	{
		return name;		
	}

	public void setName(String name) 
	{
		this.name = name;
	}

	public Vector<vslFileDataChunk> getNewChunks()
	{
		return chunks;
	}
		
	public Vector<vslFileDataChunk> getOldChunks()
	{
		return null;
	}

	public void addChunk(vslFileDataChunk chunk) {
		chunks.add(chunk);
	}

}

