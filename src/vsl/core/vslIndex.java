package vsl.core;

import java.util.Collection;
import java.util.Vector;
import java.util.HashMap;

import java.io.Serializable;

import vsl.core.types.vslElKey;
import vsl.core.types.vslRecKey;
import vsl.core.types.vslID;

import vsl.core.data.vslIndexRecordBase;

public class vslIndex {

	private vslID id = null;

	private HashMap<vslElKey, vslIndexElement> elements = 
								new HashMap<vslElKey, vslIndexElement>();

	/**
	 */
	public vslIndex()
	{
	}

	/**
	 * Load the vslIndex vslID id from the backend.
	 */
	vslIndex(vslID id, boolean blocking)
		throws vslStorageException, vslConsistencyException
	{
		this.id = id;
		load();
	}


	/**
	 * Return the element corresponding to the given vslElKey.
	 */
	public vslIndexElement getElement(vslElKey key) {
		return elements.get(key);
	}

	/**
	 * Return a list of all elements in this index.
	 */
	public Vector<vslIndexElement> getElements() {
		return new Vector(elements.values());

	}

	/**
	 * Add a new element to this index and initialize it with the data in
	 * <em>initData</em>.
	 */
	public <D extends Serializable> vslElKey addElement(vslIndexDataType<D> initData) {
		vslIndexElement<D> newEl = new vslIndexElement<D>(initData);
		vslElKey key = newEl.getElementKey();
		elements.put(key, newEl);
		return key;
	}

	/**
	 * Update an element associated with this record.
	 *
	 * @throws	vslInputException	If key does not match an element in the
	 * record or if prev contains a recKey that does not match a record of the
	 * element.   
	 */
	public vslRecKey updateElement(vslElKey key, vslIndexDataType newData,
							Collection<vslRecKey> prevKeys)
		throws vslInputException
	{
		vslIndexElement el = elements.get(key);
		vslLog.log(vslLog.DEBUG, "num elements: " + elements.values().size());
		if (el == null) {
			String err = "Attempt to update nonexistent element elKey=[" + key + 
					"] in index=[" + id + "]";
			vslLog.log(vslLog.ERROR, err);
			throw new vslInputException(err);
		}
		return el.updateElement(newData, prevKeys);
	}


	public vslRecKey deleteElement(vslElKey key, Vector<vslRecKey> prevKeys) 
		throws vslInputException
	{
		vslIndexElement el = elements.get(key);
		if (el == null) {
			String err = "Attempt to delete nonexistent element elKey=[" + key + 
					"] in index=[" + id + "]";
			vslLog.log(vslLog.ERROR, err);
			throw new vslInputException(err);
		}
		return el.deleteElement(prevKeys);
	}
	
	/**
	 * NOT IMPLEMENTED YET: use load() and store() for now until we get backend support.
	 */
	public vslFuture sync(boolean blocking) {
		return null;
	}

	public boolean inSync() {
		return true;
	}
	
	//getSnapShot(date)
	//getLatest()


	/* --------------- PRIVATE METHODS -------------------- */



	/**
	 * Load this index from the backend.
	 */
	void load()
		throws vslStorageException, vslConsistencyException
	{
	    Vector<vslIndexRecordBase> records = null;	   
	    vslFuture res = vsl.load(id);
	    if(res.awaitUninterruptedly().success()) {	      
			records = (Vector<vslIndexRecordBase>) res.getEntries();
			loadElementsFromRecords(records);
	    }
		else
		{
			// failed to load id, raise exception
			vslLog.log(vslLog.ERROR, "failed to load vslIndex with id " + id);
			throw new vslStorageException("Could not find vslIndex with id: " + id);
	    }
	}


	/**
	 * Store this entry in the backend returning "true" if successful.
	 */
	void store()
		throws vslStorageException
	{
		Collection<vslIndexView> views = getUnstoredRecords();
		Vector<vslIndexRecordBase> recs = new Vector<vslIndexRecordBase>();
		for(vslIndexView vw: views) 
		{
			recs.add(vw.getRecord());
		}
		vslFuture res = null;
		// If we don't already have an id in the backend then use create() otherwise
		// use add()
		if (id == null) 
		{
			vslLog.log(vslLog.DEBUG, "About to store [" + recs.size() + 
				"] records in new Index.");
			res = vsl.create(recs);
			if(res.awaitUninterruptedly().success()) {
			       id = res.getNewEntryID();
			} else {
				vslLog.log(vslLog.ERROR, "Error creating new vslIndex: " + res.getErrMsg());
				throw new vslStorageException(
						"Error creating new vslIndex: " + res.getErrMsg());
		    }
		} else {	
			vslLog.log(vslLog.DEBUG, "About to store [" + recs.size() + 
					"] in existing index with Index id: " + id); 
			res = vsl.add(id, recs);
			if(!res.awaitUninterruptedly().success()) {
				vslLog.log(vslLog.ERROR, "Error updating vslIndex [" + id + "]: " + 
						res.getErrMsg());
				throw new vslStorageException("Error updating vslIndex [" + id + "]: " + 
						res.getErrMsg());
		    }
		}
		// set status as save in records
		for (vslIndexView vw: views)
		{
			vw.setRecInBackend();
		}
	}



	/* ------------ Private Methods ---------------------- */



	/**
	 * Use <em>records</em> to generate elements and populate the
	 * <em>elements</em> HashMap of this Index.  This is used when loading the
	 * index from the backend.
	 *
	 * @param	records		The list of records associated with this index in
	 * the backend (i.e. encoding <em>all</em> elements in this index).  
	 *
	 * @throws	vslConsistencyException	If indexHistory contains more than one
	 * vslIndexCreateRecord or if any record contains a data type that is not D
	 * then a vslConsistencyException is thrown.  Also if the list of prev in
	 * various entries of the indexHistory do not point to other elements of
	 * the indexHistory or if a record in indexHistory has an elKey that does
	 * not match that of this element.
	 *
	 */
	private void loadElementsFromRecords(Collection<vslIndexRecordBase> records) 
		throws vslConsistencyException
	{
		HashMap<vslElKey, Vector<vslIndexRecordBase>> elMap = 
						new HashMap<vslElKey, Vector<vslIndexRecordBase>>();
		for (vslIndexRecordBase rec: records) {
			Vector<vslIndexRecordBase> curEl = elMap.get(rec.elKey);
			if (curEl == null) {
				curEl = new Vector<vslIndexRecordBase>();
				elMap.put(rec.elKey, curEl);
			}
			curEl.add(rec);
		}
		for (vslElKey key: elMap.keySet()) {
			vslIndexElement el = new vslIndexElement(key, elMap.get(key));
			elements.put(key, el);
		}

	}

	/**
	 * Return the unsaved records of all elements under this index.  This is
	 * used when storing the index in the backend.
	 */
	private Collection<vslIndexView> getUnstoredRecords()
	{
		Vector<vslIndexView> unsaved = new Vector<vslIndexView>();
		for (vslIndexElement el: elements.values())
		{
			unsaved.addAll(el.getUnstoredRecords());
		}
		return unsaved;
	}


	/* ------------------- Getters/Setters ----------------------- */



	public vslID getID()
	{
		return id;
	}

}
