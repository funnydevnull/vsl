package vsl.core;

import vsl.core.types.vslID;
import vsl.core.data.vslBackendData;

import java.util.Vector;

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

	public Vector<? extends vslBackendData> getEntries()
	{
		return entries;
	}
	
	public void setEntries(Vector<? extends vslBackendData> newEntries)
	{
		entries = newEntries;
	}

	public boolean success()
	{
		return successful;
	}

	public void setSuccess(boolean success)
	{
		successful = success;
	}

	public vslID getNewEntryID()
	{
		return newEntryID;
	}

	public void setNewEntryID(vslID newID)
	{
		newEntryID = newID;
	}

	public void setErrMsg(String msg)
	{
		this.errMsg = msg;
	}
	
	public String getErrMsg()
	{
		return this.errMsg;
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
