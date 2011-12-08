package vsl.core;

import java.util.Set;

public class vslEntry {
	private String vslId;
	private Set<String> versions;
	
	public vslEntry() {
		//stub constructor
		versions.clear();
	}
	
	public String getId() {
		return vslId;
	}
}
