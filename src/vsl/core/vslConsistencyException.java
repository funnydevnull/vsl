package vsl.core;

/**
 * An exception that gets thrown when inconsistent data is found coming from
 * the backend or popualting some object.  This is a VERY BAD THING (TM).  
 */
public class vslConsistencyException extends vslException {



	public vslConsistencyException(String err)
	{
		super(err);
	}
	
	public vslConsistencyException(Exception e) {
		super(e);
	}

}

