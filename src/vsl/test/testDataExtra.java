package vsl.test;

import vsl.core.data.*;


import java.io.Serializable;


public class testDataExtra extends vslChunkDataExtra implements Serializable {

	String extra = null;

	public testDataExtra(String str)
	{
		extra = str;
	}

	public String toString() {
		return extra;
	}
}

