package vsl.test;


import vsl.core.vsl;
import vsl.core.vslEntry;
import vsl.core.vslVersion;
import vsl.core.vslChunk;
import vsl.core.vslDataType;
import vsl.core.vslException;
import vsl.core.vslStorageException;
import vsl.core.types.vslID;

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
			showUsage();
		}
		TestCore1 tester = new TestCore1();
		try{
			if (args[0].equals("store"))
			{
				tester.store(args);
			}
			else if (args[0].equals("printMap"))
			{
				tester.printMap(args);
			}
			else if (args[0].equals("update"))
			{
				// TestCore1 update <config_file> <entry_id> <versionid> <new_data> <chunks>
				tester.update(args);
			}
			else if (args[0].equals("show"))
			{
				// TestCore1 update <dbfile> <entry_id> <new_data> <chunks>
				tester.show(args);
			}
			else
			{
				showUsage();
			}
		} catch (Exception e) {
			System.err.println("Exception caught: " + e.toString());
			e.printStackTrace();
		}
	}

	static void showUsage() {
		System.out.println("TestCore1 must be called with args:\n\n TestCore1 <cmd> <config_file> [cmd args]\n");
		System.out.println("Commands: store, show, update, printMap\n");
		System.out.println("Cmd args:\n" +
			"\n\tstore:\t\t[string]\tA string that seeds the test data." +
			"\n\t\t\t[numChunks]\tNumber of chunks to create\n"+
			"\n\tshow:\t\t[entryID]\t\tThe vslID of an entry to show.\n"+
			"\n\tupdate:\t\t[entryID]\t\tThe vslID of an entry to update."+
			"\n\t\t\t[versionID]\tThe vslID of the version to update.\n" +
			"\n\t\t\t[string]\tA string that seeds the update data.\n"+
			"\n\t\t\t[numChunks]\tNot used yet but must be passed an int here.\n" +
			"\n\tprintMap**:\t[bytesToShow]\t(Number of bytes of each chunk to show.\n"
			+"\n\n**printMap takes the db_file rather than the config file as a second argument.\n");
		System.exit(1);
	}

	void store(String[] args)
		throws vslException
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
		vslDataType<testChunk> data = new vslDataType<testChunk>();
		//testDataType data = new testDataType();
		for (int x = 0; x < nChunks; x++) {
			String inString = new String(base + new Integer(x));
			testChunk chunk = new testChunk(inString);
			testDataExtra extra = new testDataExtra(new String("extra "+new Integer(x)));
			chunk.setDataExtra(extra);
			data.addNewChunk(chunk);
		}
		myVsl.addEntry(data);
		myVsl.debugShow();
		myVsl.save();
	}

	void printMap(String[] args)
		throws Exception
    {	
		int nBytes = 100;
		try {
		    //vslMMBackend db = vslMMBackend.readMap(args[1]);
		    vslMMBackend db = new vslMMBackend(args[1]);
		    db.printMap(nBytes);
		} catch (Exception e) {
		    System.err.println("Usage: TestCore1 read <db_file> [id]");
			e.printStackTrace();
		}
	}
	
	
	void show(String[] args)
		throws Exception
	{
		if (args.length < 3)
		{
			System.err.println("Usage: update <config_file> <entryid>");
			System.exit(1);
		}
	    myVsl = new vsl(args[1]);
		vslID entryID = new vslID();
		entryID.setID(args[2]);
		Vector<vslID> prev = new Vector<vslID>();
		vslEntry entry = myVsl.getEntry(entryID);
		System.out.println("Entry: [" + entry.getID() + "]");
		vslVersion ver = entry.getFirstVersion();
		System.out.println("  |");
		Vector<vslID> allVerIDs  = new Vector<vslID>();
		recurseVersionPrint(ver, 1, allVerIDs);
		vslID vID = null;
		while( (vID = promptForChoice("version", allVerIDs)) != null)  
		{
			ver = entry.getVersion(vID);
			Vector<vslID> chunkIDs = showChunkHeaders(ver);
			vslID cID = null;
			while( (cID = promptForChoice("chunk", chunkIDs)) != null)  {
				vslChunk ch = ver.getChunk(cID, true);
				showChunk(ch, false);
			}
		}
	}
	
	private vslID promptForChoice(String type, Vector<vslID> idList) 
	{
		String input = null;
		System.out.print("\n" + type + ": ");
		showAllIDs(idList);
		System.out.print("Enter number of " + type + " [0-" + (idList.size() -1) + "]: ");
		input = FileChunkingTest.getLine();
		if (input == null || input.equals(""))
		{
			System.exit(1);
		}
		try {
			vslID vID = idList.get(new Integer(input).intValue());
			return vID;
		}
		catch (Exception e) {
			return promptForChoice(type, idList);
		}
	}


	private void recurseVersionPrint(vslVersion ver, int indentMult, Vector<vslID> allVer)
	{
		System.out.print(getIndent(indentMult) + "-->Version [ " + ver.getID() + "]  (child of: [");
		allVer.add(ver.getID());
		for (vslVersion prv: ver.getPrev()) {
			System.out.print(prv.getID() + ", ");
		}
		System.out.println("]");
		if (ver.getNext().size() < 1) {
			return;
		}
		System.out.println(getIndent(indentMult) + "  |");
		for (vslVersion next: ver.getNext()) {
			recurseVersionPrint(next, indentMult+1, allVer);
		}
	}

	private String getIndent(int mult)
	{
		String indent = "  ";
		String ret = "";
		for (int x = 0; x < mult; x++)
		{
			ret += indent + "|";
		}
		return ret;
	}

	private void showAllIDs(Vector<vslID> allIDs) {
		int index = 0;
		for (vslID id: allIDs) {
			System.out.print("[" + index++ + "] " + id + ", ");
		}
		System.out.println();
	}

	private Vector<vslID> showChunkHeaders(vslVersion ver) 
		throws Exception
	{
		Vector<vslID> headerIDs = new Vector<vslID>();
		ver.loadChunkHeaders();
		Vector<vslChunk> newChunks = ver.getNewChunks();
		System.out.println("New Chunks:");
		for (vslChunk c: newChunks) {
			headerIDs.add(c.getID());
			showChunk(c, true);
		}
		Vector<vslChunk> oldChunks = ver.getOldChunks();
		System.out.println("Old Chunks:");
		for (vslChunk c: oldChunks) {
			headerIDs.add(c.getID());
			showChunk(c, true);
		}
		return headerIDs;
	}

	private void showChunk(vslChunk c, boolean headerOnly) {
		int maxDataLen = 20;
		System.out.print("\tid: [" + c.getID() + "], ");
		System.out.print("digest: [" + c.getDigest() + "], ");
		if (! headerOnly ) 
		{
			byte[] cd = c.getData();
			int showLen = Math.min(cd.length, maxDataLen);
			System.out.print("\n\tdata (first " + maxDataLen + " bytes): [" + 
					new String(cd, 0, showLen) + "], ");
		}
		System.out.println();
	}


	void update(String[] args)
		throws Exception
	{
		if (args.length < 6)
		{
			System.err.println("Usage: update <config_file> <entryid> <prevVerID> <base string> <numChunks>");
			System.exit(1);
		}
	    // TestCore1 update <db_file> <entry_id> <new_data> <chunks>
	    // technically this is incorrect since vsl needs a config file
	    myVsl = new vsl(args[1]);
		//vslMMBackend db = vslMMBackend.readMap(args[1]);
		//myVsl.setBackend(db);
		String base = new String(args[4]);
		int nChunks = 5;
		try {
			nChunks = new Integer(args[5]);
		} catch (NumberFormatException nfe) {
			System.err.println("Last argument to store must be a positive integer: " + args[4]);
			System.exit(1);
		}
		if (nChunks < 0) {
			System.err.println("Last argument to store must be a positive integer: " + args[4]);
			System.exit(1);			
		}
		vslDataType<testChunk> data = new vslDataType<testChunk>();
		for (int x = 0; x < nChunks; x++) 
		{
			String inString = new String(base + new Integer(x));
			testChunk chunk = new testChunk(inString);
			testDataExtra extra = new testDataExtra(new String("extra "+new Integer(x)));
			chunk.setDataExtra(extra);
			data.addNewChunk(chunk);
		}
		vslID entryID = new vslID();
		entryID.setID(args[2]);
		vslID prevID = new vslID();
		prevID.setID(args[3]);
		Vector<vslID> prev = new Vector<vslID>();
		prev.add(prevID);
		data.setPrevVersions(prev);
		myVsl.updateEntry(entryID, data);
		myVsl.save();
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
	/*
	class testDataType implements vslDataType {

		String name;
		Vector<vslID> prevIDs = null;
		Vector<testChunk> oldChunks = new Vector<testChunk>();
		Vector<testChunk> newChunks = new Vector<testChunk>();

		public String getName()
		{
			return name;		
		}

		public void setName(String name) 
		{
			this.name = name;
		}

		public void setPrevVersions(Vector<vslID> prevIDs)
		{
			this.prevIDs = prevIDs;
		}

		public Vector<vslID> prevVersions()
		{
			return prevIDs;
		}

		public Vector<? extends vslChunk> getNewChunks()
		{
			return newChunks;
		}
		
		public Vector<? extends vslChunk> getOldChunks()
		{
			return oldChunks;
		}

		public void addNewChunk(testChunk chunk) {
			newChunks.add(chunk);
		}
		
		public void addOldChunk(testChunk chunk) {
			oldChunks.add(chunk);
		}

	}
	*/
}
