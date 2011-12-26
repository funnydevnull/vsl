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
}
