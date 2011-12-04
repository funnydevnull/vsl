package net.vsl;

import java.util.List;

public class vsl {
	private vslBackend vslBack;
	private List<String> entries;
	//private Logger log;
	
	public vsl() {
		// constructor
	}
	
	public vslEntry getEntry(String vslID) {
		return vslBack.getEntry(vslID);
	}
}

