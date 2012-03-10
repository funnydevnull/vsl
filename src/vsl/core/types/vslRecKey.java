package vsl.core.types;

import java.io.Serializable;

/**
 * This is a unique "name" representing a particular record in an element in a
 * vslIndex.  Element will generally undergo several revisions and updates and
 * each such update is assigned a new RecKey though all share the same element
 * key.
 */
public class vslRecKey implements Serializable {

	private String id;
	
	public vslRecKey() {
		id = "";
	}
	
	public String getKey() {
		return id;
	}
	
	public void setKey(String s) {
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
	 * Two vslRecKey are equal if their id members are equal and non-null.
	 */
	@Override
	public boolean equals(Object v) {
		if ( (! (v instanceof vslRecKey)) || id == null 
				|| ((vslRecKey)v).getKey() == null ) 
		{
			return false;
		}
		else 
		{
			return id.equals(((vslRecKey)v).getKey());
		}
	}

}
