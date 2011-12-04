package net.vsl;

import org.apache.commons.collections.MultiHashMap;
import org.apache.commons.collections.MultiMap;

public class vslBackend {
	// for now use MultiHashMap implementation as backend
	private MultiMap storage = new MultiHashMap();
	
	public vslBackend() {
	}

	public vslEntry getEntry(String vslId) {
		// query storage engine for the vslId
		return (vslEntry) storage.get(vslId);
	}
}