package vsl.test;


import vsl.core.vsl;
import vsl.core.vslChunk;
import vsl.core.vslDataType;
import vsl.core.vslStorageException;

import vsl.core.data.*;

import vsl.backends.multimap.vslMMBackend;

import java.util.Vector;
import java.io.Serializable;

/**
 * A class to test core functionality.
 */
public class TestCore1  {


	private static vsl myVsl = null;

	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.out.println("Must specify db file:\n TestCore1 <cmd> <dbfile> [opt args]");
			System.exit(1);
		}
		TestCore1 tester = new TestCore1();
		try{
			if (args[0].equals("store"))
			{
				tester.store(args);
			}
			if (args[0].equals("read"))
			{
				tester.read(args);
			}
		} catch (Exception e) {
			System.err.println("Exception caught: " + e.toString());
			e.printStackTrace();
		}
	}

	void store(String[] args)
		throws vslStorageException
	{
		String base = "hey ";
		int nChunks = 5;
		// Initialize a new vsl
		// passing the name of the db file
		myVsl = new vsl(args[1]);
		if (args.length > 2)
		{
			base = args[2];
		}
		if (args.length > 3)
		{
			try {
				nChunks = new Integer(args[3]);
			} catch (NumberFormatException nfe) {
				System.err.println("Last argument to store must be a positive integer: " + args[3]);
				System.exit(1);
			}
			if (nChunks < 0)
			{
				System.err.println("Last argument to store must be a positive integer: " + args[3]);
				System.exit(1);
			}
		}
		testDataType data = new testDataType();
		for (int x = 0; x < nChunks; x++) {
			String inString = new String(base + new Integer(x));
			testChunk chunk = new testChunk(inString);
			testDataExtra extra = new testDataExtra(new String("extra "+new Integer(x)));
			chunk.setDataExtra(extra);
			data.addChunk(chunk);
		}
		myVsl.addEntry(data);
		myVsl.debugShow();
		myVsl.save();
	}


	void read(String[] args)
		throws Exception
	{
		vslMMBackend db = vslMMBackend.readMap(args[1]);
		db.printMap();
	}

	/*  -------------- Inner Classes ---------------- */

	class testChunk extends vslChunk {

		public testChunk(String data) {
			super(data.getBytes());	
		}

		public String toString() {
			return new String(getData());
		}
	}

	class testDataType implements vslDataType {

		String name;
		Vector<testChunk> chunks = new Vector<testChunk>();

		public String getName()
		{
			return name;		
		}

		public void setName(String name) 
		{
			this.name = name;
		}

		public Vector<? extends vslChunk> getNewChunks()
		{
			return chunks;
		}
		
		public Vector<? extends vslChunk> getOldChunks()
		{
			return null;
		}

		public void addChunk(testChunk chunk) {
			chunks.add(chunk);
		}

	}

}
