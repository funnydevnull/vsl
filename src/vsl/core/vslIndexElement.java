package vsl.core;

import vsl.core.types.vslElKey;
import vsl.core.types.vslRecKey;
import vsl.core.data.vslIndexRecordBase;

import java.util.Collection;
import java.util.Vector;
import java.util.HashMap;

import java.io.Serializable;

/**
 * A vslIndexElement represents <em>the full history</em> of an entry in an
 * index in the backend.  Thus it contains all records associated with that
 * entry.   A particular index record is associated with a data type D.  All
 * records in this index in the backend are presumed to store only data of the
 * class D.
 *
 * To query the history of a vslIndexElement vslIndexView's are used.  Each
 * view represents a particular version of the index.
 */
public class vslIndexElement<D extends Serializable> {

	private vslElKey elKey = null;

	private vslIndexView<D> createRecord = null;

	/**
	 * A lookup of <em>all</em> records associated with this Element (including
	 * the createRecord).
	 * */
	private HashMap<vslRecKey, vslIndexView<D>> recLookup = 
					new HashMap<vslRecKey, vslIndexView<D>>();

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
	 * A constructor to create a new index element from some data passed in the
	 * dataHolder class.
	 *
	 * @param	dataHolder	A container for the data in this class as well
	 * providing methods to generate element and record keys.  This class may
	 * be overriden to provide different key generation algorithms.
	 *
	 */
	vslIndexElement(vslIndexDataType<D> dataHolder) 
	{
		// we generate an element key using the holder so that 
		this.elKey = dataHolder.genElementKey();
		this.classOfD = dataHolder.getData().getClass();
		createRecord = new vslIndexView<D>(this, dataHolder);
		recLookup.put(createRecord.getRecKey(), createRecord);
	}

	/**
	 * Initialize this IndexElement using this vector of vslRecords from the backend.
	 *
	 * If the backend data is inconsistent then a vslConsistencyException is thrown (see below).
	 *
	 * @param	elKey	The key for the element.
	 *
	 * @param	indexHistory	The vslIndexRecord's encoding the history of
	 * this element.  All entries in this collection should have elKey set to
	 * the same value which should match the elKey argument.
	 *
	 * @param	classOfD	The class representing what all data objects in the
	 * records should be (i.e. the data record for create/update records).
	 * This should match the type D of this generic but because of how
	 * generic's work we need to pass this explicitly.
	 *
	 * @throws	vslConsistencyException	If indexHistory contains more than one
	 * vslIndexCreateRecord or if any record contains a data type that is not D
	 * then a vslConsistencyException is thrown.  Also if the list of prev in
	 * various entries of the indexHistory do not point to other elements of
	 * the indexHistory or if a record in indexHistory has an elKey that does
	 * not match that of this element.
	 */
	vslIndexElement(vslElKey elKey, Collection<vslIndexRecordBase> indexHistory)
		throws vslConsistencyException
	{
		this.elKey = elKey;
		// this generate the create record but also populates recLookup with
		// all records
		createRecord = new vslIndexView<D>(indexHistory, recLookup, this);
		this.classOfD = createRecord.getClassOfD();
	}


	/* --------------------- API METHODS ------------------------ */


	/**
	 * The element key associated with a record is generated when the record is
	 * created.  It is not a backend storage key but rather just a way to
	 * identify a record within an index (indices have backend vslID's
	 * associated with them).  All records associated with an element should
	 * have the same element key (but they have differenet record keys).
	 *
	 * @return	The element key associated with this record.  This should never
	 * be null as all constructors set this value.
	 */
	public vslElKey getElementKey() {
		return elKey;
	}

	/**
	 * This method returns the first record associated with this element.  Each
	 * element in an index is an ordered graph of records encoding the elements
	 * history.  This method returns the create record.
	 *
	 */
	public vslIndexView<D> getFirst() {
		return createRecord;
	}

	/**
	 * Update the element by adding a new record encoded in dataHolder that is
	 * an update of <em>prev</em>.  As usual the reason for multiple possible
	 * prevs is to allow for merges.
	 *
	 * @param	dataHolder	A holder encoding the new data for the update.  The
	 * vslRecKey for the update will be generated using dataHolder.genRecKey().
	 *
	 * @param	prevs	A vector of record keys corresponding to records that
	 * are supposed to <em>directly</em> preceed this update.  Excepting merges
	 * this should usually be length one.
	 *
	 * @return	The vslRecKey of the newly created record.
	 *
	 * @throws	vslInputException	If an record in prev does not correspond to
	 * an actual record of this element.
	 */
	public vslRecKey updateElement(vslIndexDataType<D> dataHolder, Collection<vslRecKey> prev) 
		throws vslInputException
	{
		Vector<vslIndexView<D>> prevViews = new Vector<vslIndexView<D>>();
		vslIndexView<D> keyView = null;
		for (vslRecKey key: prev) {
			keyView = recLookup.get(key);
			if (keyView == null) {
				String err = "Invalid prev record [" + key + "] passed to element [" + elKey + 
					"]; could not find record in element recLookup.";
				vslLog.log(vslLog.ERROR, err);
				throw new vslInputException(err);
			}
			prevViews.add(keyView);
		}
		// this initializes the view and correctly updates the nextView records
		// of all the elements of prev
		vslIndexView<D> updateView = new vslIndexView<D>(false, this, dataHolder, prevViews);
		// store the new record in the lookup
		recLookup.put(updateView.getRecKey(), updateView);
		return updateView.getRecKey();
	}

	/**
	 * Delete the element by adding a new delete record with the given
	 * <em>prev</em>.  As usual the reason for multiple possible prevs is to
	 * allow for merges.  Adding a delete record does not really delete the
	 * element but just marks the end of a branch.  If there are still
	 * undeleted branches the element continues to exist and of course it is
	 * always possible to do an update from a previous record.
	 *
	 * @param	prevs	A vector of record keys corresponding to records that
	 * are supposed to <em>directly</em> preceed this update.  Excepting merges
	 * this should usually be length one.
	 *
	 * @return	The vslRecKey of the newly created record.
	 *
	 * @throws	vslInputException	If an record in prev does not correspond to
	 * an actual record of this element.
	 */
	public vslRecKey deleteElement(Collection<vslRecKey> prev) 
		throws vslInputException
	{
		Vector<vslIndexView<D>> prevViews = new Vector<vslIndexView<D>>();
		vslIndexView<D> keyView = null;
		for (vslRecKey key: prev) {
			keyView = recLookup.get(key);
			if (keyView == null) {
				String err = "Invalid prev record [" + key + "] passed to element [" + elKey + 
					"]; could not find record in element recLookup.";
				vslLog.log(vslLog.ERROR, err);
				throw new vslInputException(err);
			}
			prevViews.add(keyView);
		}
		// this initializes the view and correctly updates the nextView records
		// of all the elements of prev
		vslIndexView<D> deleteView = new vslIndexView<D>(true, this, null, prevViews);
		// store the new record in the lookup
		recLookup.put(deleteView.getRecKey(), deleteView);
		return deleteView.getRecKey();
	}


	/* -------------------- PACKAGE METHODS ----------------- */



	/**
	 * Returns a vector of all the vslIndexRecord's associated with this
	 * element's history.
	 */
	Vector<vslIndexRecordBase> getAllRecords() {
		return new Vector(recLookup.values());
	}


	/**
	 * Returns records that have not yet been stored to the backend. This is
	 * used in saving the index.
	 */
	Vector<vslIndexView> getUnstoredRecords() {
		Vector<vslIndexView> newRecs = new Vector<vslIndexView>();
		for (vslIndexView view: recLookup.values()) {
			vslLog.log(vslLog.DEBUG, 
				"found record with 'recInBackend': " + view.recInBackend());
			if (! view.recInBackend() )
			{
				newRecs.add(view);
			}
		}
		return newRecs;
	}



}
