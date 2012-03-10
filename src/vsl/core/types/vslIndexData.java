package vsl.core.types;

/**
 * This a generic data type represnting the data in an index element.
 * Such objects can be stored in indexRecords which form the history of an
 * index element.  
 */
public class vslIndexData<D> {

	private D data = null;
	
	public vslIndexData(D data) {
		this.data = data;
	}
	
	public D getData() {
		return data;
	}
	
}
