package vsl.core;

public class vslVersion {
	private String vslId;
	private vslData my_data;
	
	public vslVersion() {
		// constructor
	}
	
	public vslVersion(vslData initial) {
		my_data = initial;
	}
	
	public String getId() {
		return vslId;
	}
}