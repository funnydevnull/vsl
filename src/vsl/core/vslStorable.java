package vsl.core;

/**
 * All subclasses of this class can be stored in the VSL backend using methods
 * in vsl such as vsl.create(), vsl.update(), etc...
 */
public class vslStorable {
	
	protected vslID		id;

	public abstract Vector<vslBackendData> getBackendData();

	public vslID getID()
	{
		return id;
	}
	
	public void setID(vslID id)
	{
		this.id = id;
	}


}
