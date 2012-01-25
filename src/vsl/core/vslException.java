package vsl.core;

public class vslException extends Exception {

	private Exception original;

	public vslException(String err)
	{
		super(err);
	}

	public vslException(Exception e) {
		super(e.toString());
		original = e;
	}

}
