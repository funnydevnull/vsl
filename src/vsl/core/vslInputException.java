package vsl.core;

/**
 * An exception that gets thrown when the core system is passed inconsistent or
 * unexpected input.  Examples include:
 *
 * - invalid vslIDs
 * - inconsistenly populated vslDataTypes
 */
public class vslInputException extends vslException {



	public vslInputException(String err)
	{
		super(err);
	}
	
	public vslInputException(Exception e) {
		super(e);
	}

}

