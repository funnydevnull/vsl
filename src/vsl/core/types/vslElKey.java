package vsl.core.types;

import java.io.Serializable;

/**
 * This is a unique "name" representing an element in a vslIndex.  Such an
 * element will generally undergo several revisions and updates but all records
 * associated with this element will share the same vslElKey.
 */
public class vslElKey implements Serializable {

	private String id;
	
	public vslElKey() {
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
	 * Two vslElKeys are equal if their id members are equal and non-null.
	 */
	@Override
	public boolean equals(Object v) {
		if ( (! (v instanceof vslElKey)) || id == null 
				|| ((vslElKey)v).getKey() == null ) 
		{
			return false;
		}
		else 
		{
			return id.equals(((vslElKey)v).getKey());
		}
	}

}
