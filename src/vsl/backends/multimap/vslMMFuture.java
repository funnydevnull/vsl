package vsl.backends.multimap;


import vsl.core.vslFuture;


public class vslMMFuture extends vslFuture {

	/**
	 * Since this is local access we basically return immediatley.
	 */
	public boolean awaitUninterruptedly()
	{
		return ready;
	}

}


