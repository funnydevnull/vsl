package vsl.core;

import vsl.core.data.vslIndexRecordBase;
import vsl.core.data.vslIndexCreateRecord;
import vsl.core.data.vslIndexUpdateRecord;
import vsl.core.data.vslIndexDeleteRecord;
import vsl.core.types.vslElKey;
import vsl.core.types.vslRecKey;

import java.util.Collection;
import java.util.Vector;
import java.util.HashMap;

import java.lang.reflect.Type;

import java.io.Serializable;

/**
 * A vslIndexView represents a particular <em>view</em> into a
 * vslIndexElement's history.  Thus it corresponds to a particuar
 * vslIndexRecord in the backend (a create, update or delete record).  Like
 * vslIndexElement is typed with a type D that all the records for a given
 * element must share (as they represent updates of the same object).
 * <br>
 * <em>Internal Usage:</em><br>
 * <br>
 * When populating a vslIndexElement from the backend the package-private
 * constructor of vslIndexView should be used.  This method initializes not
 * only a single view but <em>all views associated with an element</em>.  This
 * constructur actually returns the create records while the other records are
 * stored in a hashmap passed to it so e.g. for an integer based element:
 * <p>
 *
 * HashMap<vslRecKey, vslIndexView<Integer>> lookup = 
 * 				new HashMap<vslRecKey, vslIndexView<Integer>>();
 * vslIndexView<Integer> createRecord = 
 * 				new vslIndexView<Integer>(backendRecords, lookup);
 * // now lookup has all the other records
 * vslIndexView<Integer> someRec = lookup.get(new vslRecKey("XXXXX"));
 * // but they can also be retreived from the createRecord
 * Vector<vslIndexView<Integer>> firstUpdates = createRecord.nextViews();
 *
 * </p>
 * 
 * When creating a new vslIndexElement the first view can be populated using
 * the other constructor of this class.  Update's to an element are made by
 * calling updateByData() or updateByView() on the views which directly preceed
 * the update.
 */
public class vslIndexView<D extends Serializable> {

	// INTERNALLY used to track type of this view
	private final static int TYPE_CREATE = 0;
	private final static int TYPE_UPDATE = 1;
	private final static int TYPE_DELETE = 2;

	private vslIndexRecordBase record = null;

	private Vector<vslIndexView<D>> prevViews = null;
	private Vector<vslIndexView<D>> nextViews = null;

	/**
	 * Whether or not the record associated with this view already exists in the backend.
	 */
	private boolean recInBackend = false;


	// the vslIndexElement to which this view belongs
	private vslIndexElement parentEl = null;

	/**
	 *  Because reflection doesn't work on generic types such as D we are
	 *  forced to store the classOfD which we get as a parameter from the
	 *  constructor and which we use to check consistency of the data entry in
	 *  a record.  Unfortunately there doesn't seem to be a more elegant
	 *  solution.
	 */ 
	private Class classOfD = null;;


	/* ------------------- CONSTRUCTORS ------------------------ */
	
	/**
	 * Package level constructor.
	 *
	 * This constructor actually acts like a factory method initializing
	 * <em>all</em> records associated with this element (from indexHistory).
	 * The initialized object will be the create record while the other updates
	 * and delete records are returned in the updateRecords hash (keyed by
	 * their record id).  
	 *
	 * <em>Note:  We must use a constructor rather than a static factory method
	 * because we need to know the type D.</em>
	 *
	 *  <em>Note 2: Because reflection doesn't work on generic types such as D
	 *  we are forced to pass the classOfD as a parameter which we use to check
	 *  consistency of the indexHistory (i.e. that all record data is of type
	 *  D).  Unfortunately there doesn't seem to be a more elegant
	 *  solution.</em>
	 *
	 * @param	indexHistory	A vector of <em>all</em> records associated
	 * with this entry, including the create, update and delete records.  The
	 * object initialized by this contructor will be the <em>create</em> object
	 * for this element.
	 *
	 * @param	viewLookup	An empty hashtable which will be filled with
	 * recKey to indexRecord mappings (for all records).  In principle an
	 * external class can recover the other records by calling nextView() on
	 * the objects initialized here but to avoid doing the work twice we
	 * populate this hashMap here once.
	 *
	 * @param	parentEl	The parent element to which this record belongs.
	 *
	 * @throws	vslConsistencyException	If indexHistory contains more than one
	 * vslIndexCreateRecord or if any record contains a data type that is not
	 * the same type (i.e. has a different classOfD) as the create record then
	 * a vslConsistencyException is thrown.  Also if the list of prev in
	 * various entries of the indexHistory do not point to other elements of
	 * the indexHistory or if a record in indexHistory has an elKey that does
	 * not match that of parentEl.
	 */
	vslIndexView(Collection<vslIndexRecordBase> indexHistory,
				HashMap<vslRecKey, vslIndexView<D>> viewLookup,  vslIndexElement parentEl)
			throws vslConsistencyException
	{
		this.parentEl = parentEl;
		boolean foundCreate = false;
		// first loop through and setup create record.  This must be done
		// before creating any other record because we need to fix the classOfD
		// assocaited with this element (this is done in populateFromRecord).
		for (vslIndexRecordBase rec: indexHistory) 
		{
			if (rec.elKey != parentEl.getElementKey())
			{
				String err = "Record elKey [" + rec.elKey + 
					"] does not match parent element key: [" + parentEl.getElementKey() + "]";
				vslLog.log(vslLog.ERROR, err);
				throw new vslConsistencyException(err);
			}
			// when we find the createRecord we populate ourselves using it.
			if (rec instanceof vslIndexCreateRecord) {
				if (foundCreate) {
					vslLog.log(vslLog.ERROR, 
							"Found second create record with recKey [" + 
							rec.recKey + "] for element [" + rec.elKey +"]");
					throw new vslConsistencyException(
							"Found second create record with recKey [" + 
							rec.recKey + "] for element [" + rec.elKey +"]");
				}
				foundCreate = true;
				populateFromRecord(rec, null);
				viewLookup.put(rec.recKey, this);
			}
		}
		for (vslIndexRecordBase rec: indexHistory) 
		{
			if (! (rec instanceof vslIndexCreateRecord) )
			{
				// create the update/delete records using the private constructor.
				// the classOfD should have been set by the populateFromRecord above
				vslIndexView<D> vw = new vslIndexView<D>(rec, classOfD);
				viewLookup.put(rec.recKey, vw);
			}
		}
		// we loop again once all records created to set next/prev
		for (vslIndexRecordBase rec: indexHistory) 
		{
			vslIndexView<D> curView = viewLookup.get(rec.recKey);
			// when we find the createRecord we populate ourselves using it.
			if (rec instanceof vslIndexUpdateRecord) {
				Vector<vslIndexView<D>> prev = new Vector<vslIndexView<D>>();
				if ( ((vslIndexUpdateRecord) rec).prev == null)
				{
					String err = "UpdateRecord reckey [" + rec.recKey +
						"] with NULL prev found in element [" + rec.elKey + "]";
					vslLog.log(vslLog.ERROR, err);
					throw new vslConsistencyException(err);
				}
				// generate the vec of prev versions and simultaneously add
				// curVer to each prev version as a "next"
				for(vslRecKey key: ((vslIndexUpdateRecord) rec).prev) {
					vslIndexView<D> pv = viewLookup.get(key);
					if (pv == null)
					{
						String err = "Could not find prev record with reckey ["
							+ key + "] while populating update record [" + rec.recKey
							+ "] in element [" + rec.elKey + "]";
						vslLog.log(vslLog.ERROR, err);
						throw new vslConsistencyException(err);
					}	
					prev.add(pv);
					// we set that we're a next version for this 
					pv.addNext(curView);
				}
				// store the prev version
				try {
					curView.setPrev(prev);
				}
				catch(vslInputException e)
				{
					// we change the type of the exception because we're
					// getting the data from the db so this is a consistency
					// rathern than input exception
					throw new vslConsistencyException(e);		
				}

			}
			// unfortunatley this code has to replicate the above becaues
			// rec.prev is independently defined for delete/update.
			else if (rec instanceof vslIndexDeleteRecord) {
				Vector<vslIndexView<D>> prev = new Vector<vslIndexView<D>>();
				// generate the vec of prev versions and simultaneously add
				// curVer to each prev version as a "next"
				for(vslRecKey key: ((vslIndexDeleteRecord) rec).prev) {
					vslIndexView<D> pv = viewLookup.get(key);
					if (pv == null)
					{
						String err = "Could not find prev record with reckey ["
							+ key + "] while populating delete record [" + rec.recKey
							+ "] in element [" + rec.elKey + "]";
						vslLog.log(vslLog.ERROR, err);
						throw new vslConsistencyException(err);
					}	
					prev.add(pv);
					// we set that we're a next version for this 
					pv.addNext(curView);
				}
				// store the prev version
				try {
					curView.setPrev(prev);
				}
				catch(vslInputException e)
				{
					// we change the type of the exception because we're
					// getting the data from the db so this is a consistency
					// rathern than input exception
					throw new vslConsistencyException(e);		
				}
			}
		}
	}


	/**
	 * Package level constructor.
	 *
	 * This constructor is used when creating a new element from a first
	 * record.  Thus this element does not yet exist in the backend.
	 *
	 * @param	parentEl	The parent element to which this record belongs.
	 *
	 * @param	dataHolder	A data Holder populated with the data for this new
	 * record as well as methods for generating the record and element keys.
	 *
	 */
	vslIndexView(vslIndexElement parentEl, vslIndexDataType<D> dataHolder)
	{
		this.parentEl = parentEl;
		this.classOfD = dataHolder.getData().getClass();
		// generate the record for this view
		try {
			generateRecord(TYPE_CREATE, parentEl, dataHolder, null);
		}
		catch (vslInputException e)
		{
			vslLog.log(vslLog.ERROR, 
				"caught vslInputException generating record for create view: PLEASE REPORT THIS BUG.");
		}	
		// we track that this record hasn't been created yet
		recInBackend = false;
	}


	/**
	 * Initialize a new update or delete record.
	 *
	 * @param	isDelete	Should be true if this is a delete record and false
	 * if this is an update record.
	 *
	 * @param	parentEl	The parent element for this view.
	 *
	 * @param	dataHolder	A typed contained for the data of this view/record
	 * if this is an update.  If this is a delete record this should be null
	 * (as its not used).
	 *
	 * @param	prevs		A list of views/records that directly preceed this
	 * view in the element history.
	 *
	 * @throws	vslInputException	If prevs contains a delete record as the
	 * latter should always be the last record on any branch.
	 */
	vslIndexView(boolean isDelete, vslIndexElement<D> parentEl, vslIndexDataType<D> dataHolder, 
			Vector<vslIndexView<D>> prevs)
		throws vslInputException
	{
		this.parentEl = parentEl;
		if (isDelete) {
			generateRecord(TYPE_DELETE, parentEl, null, prevs);
		}
		else
		{
			this.classOfD = dataHolder.getData().getClass();
			generateRecord(TYPE_UPDATE, parentEl, dataHolder, prevs);
		}
		// we track that this record hasn't been created yet
		recInBackend = false;

	}



	/**
	 * Initialize an update/delete view from its backend Record.  This should
	 * never be used with create views as they are called via the other two
	 * constructors. If <em>rec</em> is a update record and its data record is
	 * not a subclass of D then we throw a vslDataConsistency exception.
	 *
	 * @param	classOfD	The Class of the data associated with this element.
	 * This should never be null if this is a create record otherwise it should
	 * be set to the classOfD of the create record of the associated element.
	 *
	 * @throws	vslConsistencyException	if rec instanceof
	 * vslIndexCreate/UpdateRecord and rec.data is not instanceof D.
	 *
	 */
	private vslIndexView(vslIndexRecordBase rec, Class classOfD) 
		throws vslConsistencyException
	{
		populateFromRecord(rec, classOfD);
	}



	/* --------------------- API METHODS ------------------------ */


	/**
	 * Get the views that directly preceed this one.  The reason that more
	 * than one view can preceed another is to allow for merges (a view with
	 * multiple prevs is effectively a merge of these previous versions).
	 *
	 * @return	A vector of vslIndexView's preceeding this one or null if this
	 * is the first view (i.e. isCreate() == true).
	 */
	public Vector<vslIndexView<D>> getPrevViews() {
		return prevViews;
	}

	/**
	 * Get the views that directly follow this one.  More than one next means
	 * we have a branch.
	 *
	 * @return	A vector of vslIndexViews following this one or null if this
	 * is the last view (end of a branch).
	 */
	public Vector<vslIndexView<D>> getNextViews() {
		return nextViews;
	}


	/* -------------------- PACKAGE METHODS ----------------- */


	/**
	 * Add an update to this view, i.e. a next view, from the data in
	 * updateData.  This method will createa  new vslIndexView corresponding to
	 * the update, appropriately set its prevs to include this current view and
	 * then set the next of this current to to include the new record.  The new
	 * record will then be returned so it can be used (via the
	 * addUpdateByRecord method) to correctly set the nextViews of any other
	 * views which are its immediate predecessors.
	 *
	 * @param	updateData		this object contains the data associated with
	 * this record.  we also use updateData.genRecKey() to set the key for the
	 * new record.
	 *
	 *
	vslIndexView<D> addUpdateByData(vslIndexDataType<D> updateData) 
	{
		vslIndexUpdateRecord update  = new vslIndexUpdateRecord();
		update.setParentElement(parentEl);
		this.classOfD = dataHolder.getData().getClass();
		record.elKey = parentEl.getElementKey();
		((vslIndexCreateRecord)record).data = dataHolder.getData();
		record.recKey = dataHolder.genRecKey();
		// we track that this record hasn't been created yet
		recInBackend = false;
		
	}

	void addUpdateByRecord(vslIndexView<D> updateRecord)
	{

	}
	*/

	
	/* ---------------- PRIVATE METHODS ---------------------- */

	/**
	 * Add an element to the nextViews of this view (note: the set of nextViews
	 * are stored in the backend since they can change with time).
	 */
	private void addNext(vslIndexView<D> next) {
		if (nextViews == null) {
			nextViews = new Vector<vslIndexView<D>>();
		}
		nextViews.add(next);
	}

	/**
	 * Set the prevViews vector of this views and also set the
	 * <em>record</em> to reflect this.  This should only be done when
	 * populating the view from a backend record or when first creating a view
	 * as once the prevs are set and stored in the backend they never change.
	 *
	 * @throws	vslInputException	If prevVec contains a delete record as a
	 * delete record should always be the last record on a branch.
	 */
	private void setPrev(Vector<vslIndexView<D>> prevVec) 
		throws vslInputException
	{
		if (record instanceof vslIndexUpdateRecord) {
			vslIndexUpdateRecord update = (vslIndexUpdateRecord) record;
			if (update.prev == null) {
				Vector<vslRecKey> prevKeys = new Vector<vslRecKey>();
				for (vslIndexView<D> prev: prevVec) {
					// throw exception if prev record is a delete record
					if (prev.isDelete()) {
						String err = 
							"Attempt to add an Update with a prev record that is a delete record [" 
							+ prev.getRecKey() +"]";
						vslLog.log(vslLog.ERROR, err);
						throw new vslInputException(err);
					}
					prevKeys.add(prev.getRecKey());
				}
				update.prev = prevKeys;
			}
		}
		else if (record instanceof vslIndexDeleteRecord) {
			vslIndexDeleteRecord del = (vslIndexDeleteRecord) record;
			if (del.prev == null) {
				Vector<vslRecKey> prevKeys = new Vector<vslRecKey>();
				for (vslIndexView<D> prev: prevVec) {
					// throw exception if prev record is a delete record
					if (prev.isDelete()) {
						String err = 
							"Attempt to add a Delete with a prev record that is a delete record [" 
							+ prev.getRecKey() +"]";
						vslLog.log(vslLog.ERROR, err);
						throw new vslInputException(err);
					}
					prevKeys.add(prev.getRecKey());
				}
				del.prev = prevKeys;
			}
		}
		this.prevViews = prevVec;
	}

	private void setParentElement(vslIndexElement parentEl) {
		this.parentEl = parentEl;
	}

	/**
	 * <em>NOTE</em> IMPLEMENTATION ISSUE: need to check how we can compare the
	 * data inside of rec with the generic type D.  Its not clear how best to
	 * do this.
	 *
	 * Populate this IndexElement using  the vslRecords rec.  Since we're
	 * populating this view from an existing record we set recInBackend = true
	 * when this method is called. If <em>rec</em> is an update record
	 * and its data record is not a subclass of D then we throw a
	 * vslDataConsistency exception.
	 *
	 * This is a helper method used by both constructors to populate the
	 * instance from the backend record.
	 *
	 * @param	rec		A backend record corresponding to a create, update or delete.
	 *
	 * @param	classOfD	The class of the data object associated with this
	 * element.  This is used to set classOfD.  For updateRecords we also check
	 * that their data type corresponds to this or we throw an exception (we
	 * don't do this for creates since they <em>define</em> the class of D).
	 *
	 * @throws	vslConsistencyException	if rec instanceof
	 * vslIndexUpdateRecord and rec.data is not instanceof classOfD.
	 *
	 */
	private void populateFromRecord (vslIndexRecordBase rec, Class classOfD) 
		throws vslConsistencyException
	{
		//Type t = getClass().getGenericSuperclass();
		//Class c = D.getClass();
		/*
		if ( rec instanceof vslIndexCreateRecord && 
				! classOfD.isInstance( ((vslIndexCreateRecord)rec).data ) )
		{
			String err = 
				"Attemping to initialize vslIndexView for createRecord of type [" 
					+ classOfD.getName() + "] using data of type [" 
					+ rec.getClass().getName() + "]";
			vslLog.log(vslLog.ERROR, err);
			throw new vslConsistencyException(err);
		}*/
		// if classOfD is null then this should be a Create record so we get
		// its classOfD from whatever's in the data.
		if ( rec instanceof vslIndexCreateRecord )
		{
			this.classOfD = ((vslIndexCreateRecord) rec).data.getClass();
		}
		else if ( rec instanceof vslIndexUpdateRecord ) 
		{
			if ( ! ( ((vslIndexUpdateRecord) rec).data.getClass() == classOfD ) )
			{
				String err = 
					"Attemping to initialize vslIndexView for updateRecord of type [" 
						+ (classOfD != null ? classOfD.getName():"null") 
						+ "] using data of type [" + rec.getClass().getName() + "]";
				vslLog.log(vslLog.ERROR, err);
				throw new vslConsistencyException(err);
			}
			this.classOfD = classOfD;
		}
		recInBackend=true;
		this.record = rec;
	}

	/**
	 * Populate's the <em>record</em> of this view from the data passed.
	 *
	 * This is a helper method used by the non-backend constructors to populate
	 * the internal record object from a vslIndexDataType passed by some
	 * external interface.
	 *
	 * Not only does it populate the record but it sets the prev of this View
	 * correctly and adds this view to the <em>nextViews</em> of all the views
	 * in <em>prevs</em>.
	 *
	 * @param	recType		What kind of view this is going to be.  Should be
	 * one of TYPE_CREATE, TYPE_UPDATE, or TYPE_DELETE.
	 *
	 * @param	parentEl	The parent element for this record.
	 *
	 * @param	dataHolder	A container for the data associated with this
	 * record.  Also used to genrate the recordKey via
	 * vslIndexDataType.genRecKey() method.
	 *
	 * @param	prevs	The Views for the records directly preceeding this one
	 * (for update, delete records) or null if this is a create view.  These
	 * prevs will have their <em>nextViews</em> correctly updated to include
	 * this View.
	 *
	 * @throws	vslInputException	If prevs contains a delete record as the
	 * latter should always be the last record on any branch.
	 */
	private void generateRecord(int recType, vslIndexElement parentEl,
			vslIndexDataType<D> dataHolder, Vector<vslIndexView<D>> prevs)
		throws vslInputException
	{
		switch (recType) {
			case TYPE_CREATE:	
				record = new vslIndexCreateRecord();
				( (vslIndexCreateRecord) record).data = dataHolder.getData();
				break;
			case TYPE_UPDATE:
				record = new vslIndexUpdateRecord();
				( (vslIndexUpdateRecord) record).data = dataHolder.getData();
				break;
			case TYPE_DELETE:
				record = new vslIndexDeleteRecord();
				break;
		}
		// populate the prevs and set us to be one of their next records.
		if (prevs != null) {
			setPrev(prevs);
			for (vslIndexView<D> view : prevs) {
				view.addNext(this);
			}
		}
		record.elKey = parentEl.getElementKey();
		if (dataHolder != null) 
		{
			record.recKey = dataHolder.genRecKey(dataHolder.getData());
		}
		else
		{
			record.recKey = vslIndexDataType.genRecKey(null);
		}
	}



	/* ------------------ Getters/setters ----------------------- */

	public vslRecKey getRecKey() {
		// should not happen -- record should be initialized
		if (record == null) {
			vslLog.log(vslLog.ERROR, 
					"called getRecKey() on vslIndexView with null record.  Should not happen!");
		}
		return record.recKey;
	}

	/**
	 * This returns true if this view corresponds to a record that already
	 * exists in the backend and false if it corresponds to a newly created
	 * record (in this session) that still has yet to be stored in the backend.
	 */
	boolean recInBackend() {
		return recInBackend;
	}

	/**
	 * Note that this record has been stored in the backend.  This is used
	 * after the whole index is stored.
	 */
	void setRecInBackend() {
		recInBackend = true;
	}

	/**
	 * Returns the record underlying this view.  Note this record may not
	 * correspond to an actual entry in the backend as it may not yet have been
	 * stored (i.e. a record is created as part of a new view even before the
	 * view/record are stored).
	 */
	vslIndexRecordBase getRecord() {
		return record;
	}	

	Class getClassOfD() {
		return classOfD;
	}

	public boolean isDelete() {
		return (record instanceof vslIndexDeleteRecord);
	}

	public boolean isUpdate() {
		return (record instanceof vslIndexUpdateRecord);
	}

	public boolean isCreate() {
		return (record instanceof vslIndexCreateRecord);
	}

	/**
	 * 
	 * @throws	vslInputException	If this is a delete record.	
	 */
	public D getData() 
		throws vslInputException
	{
		if (record instanceof vslIndexCreateRecord) {
			return (D) ( (vslIndexCreateRecord) record).data;
		}
		else if (record instanceof vslIndexUpdateRecord) {
			return (D) ( (vslIndexUpdateRecord) record).data;
		}
		throw new vslInputException("Calling getData() on a delete record.");
	}
}

