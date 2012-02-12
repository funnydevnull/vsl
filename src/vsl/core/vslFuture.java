package vsl.core;

import vsl.core.types.vslID;
import vsl.core.data.vslBackendData;

import java.util.Vector;

/**
 * vslFutures provide a mechanism to avoid blocking even when the underlying
 * storage layer (e.g. the P2P system) is blocking or slow.  A backend
 * operation simple returns _immediately_ but it returns a vslFuture object
 * which encodes the state of the request.  The vslFuture object can be probed
 * for the status of the request using isReady() or awaitUninterruptedly().
 * Once it is ready the status of the request can be checked via success() and
 * any response in the request can be retreived via e.g. getEntries.
 */
public abstract class vslFuture {

	//should be set to true once the future has been populated
	protected boolean ready = false;

	protected boolean successful;
	protected vslID newEntryID;
	protected String errMsg;
	protected Vector<? extends vslBackendData> entries = null;

	public vslFuture() {
		// implement
	}


	/**
	 * Implmentations should return true when a result is populated in the
	 * future and sleep otherwise.
	 */
	public abstract boolean awaitUninterruptedly();


	/* ------------- RESPONSE PROBE METHODS ---------------------- */
	
	/**
	 * @return	The backend entries requested in the request associated with this future.
	 */
	public Vector<? extends vslBackendData> getEntries()
	{
		return entries;
	}
	

	/**
	 * @return	True if the request associated with this future was successful.
	 */
	public boolean success()
	{
		return successful;
	}

	/**
	 * @return	The ID of the backend entry created in the request associated with this future.
	 */
	public vslID getNewEntryID()
	{
		return newEntryID;
	}

	
	/**
	 * @return	The error message generated in the request associated with this
	 * future.  Usually only set if success returns false;
	 */
	public String getErrMsg()
	{
		return this.errMsg;
	}


	/**
	 * @return	True if the operation is complete.  This is a non-blocking
	 * version of awaitUninterruptedly().
	 */
	public boolean isReady()
	{
		return ready;
	}


	/* ---------------- BACKEND METHODS -------------------- */

	
	/**
	 * Set entries to be returned with this vslFuture.  Only backends should access this method.
	 */
	public void setEntries(Vector<? extends vslBackendData> newEntries)
	{
		entries = newEntries;
	}


	/**
	 * An entry ID for the entry created in the backend request associated with
	 * this future.  Only backends should access this method.
	 */
	public void setNewEntryID(vslID newID)
	{
		newEntryID = newID;
	}

	/**
	 * Error message for the request associated with this future.  Only
	 * backends should access this method.
	 */
	public void setErrMsg(String msg)
	{
		this.errMsg = msg;
	}

	/**
	 * Set the success status of this Future.  Only backends should access this method.
	 */
	public void setSuccess(boolean success)
	{
		successful = success;
	}

	/**
	 * Should be called once the future's data has been populated so waiting
	 * threads can start using the data.
	 */
	public void setReady()
	{
		ready = true;
	}


}
