package vsl.core;

import java.io.Serializable;

public class vslBackendEntry implements Serializable {

	/* the ID of this entry in the backend */
	protected vslID id;
 	
	protected Boolean isCreated;

	protected Boolean isLoaded;

	public vslID getID()
	{
		return id;
	}

	public Boolean isCreated()
	{
		return false;
	}

	public Boolean isLoaded()
	{
		return false;
	}

	public vslFuture create()
	{
		return null;
	}

	public vslFuture load()
	{
		return null;
	}

}
