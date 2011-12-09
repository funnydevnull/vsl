package vsl.core;

import java.util.Set;

public class vslEntry {
	private vslID entryId;
	private Set<String> versions;
	
	public vslEntry() {
		//stub constructor
		versions.clear();
	}
	
	public vslEntry(vslVersion initial) {
		// we want to do what here with this data?
	}

	public String getId() {
		return entryId.getId();
	}
}
