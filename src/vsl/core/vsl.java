package vsl.core;

import java.util.ArrayList;
import java.util.List;

import vsl.backends.vslMMBackend;

public class vsl {
	private vslMMBackend vslBack;
	private List<String> entries;
	//private Logger log;
	
	public vsl() {
		// constructor
		vslBack = new vslMMBackend();
		entries = new ArrayList<String>();
	}
	
	public vslFuture getEntry(vslID id) {
		return vslBack.getEntry(id);
	}
}

