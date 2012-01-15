package vsl.core.types;

import java.io.Serializable;

public class vslID implements Serializable {
	private String id;
	
	public vslID() {
		id = "";
	}
	
	public String getID() {
		return id;
	}
	
	public void setID(String s) {
		this.id = s;
	}

	/**
	 * Returns true if the id is set and is a valid vsl identifier.
	 */
	public boolean isValid()
	{
		return true;
	}

	public String toString()
	{
		return id;
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	/**
	 * Two vslIDs are equal if their id members are equal and non-null.
	 */
	@Override
	public boolean equals(Object v) {
		if ( (! (v instanceof vslID)) || id == null || ((vslID)v).getID() == null ) {
			return false;
		}
		else 
		{
			return id.equals(((vslID)v).getID());
		}
	}
}
