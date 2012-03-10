package vsl.test;


import vsl.core.vsl;
import vsl.core.vslIndex;
import vsl.core.vslIndexElement;
import vsl.core.vslIndexView;
import vsl.core.vslIndexDataType;
import vsl.core.vslException;
import vsl.core.vslStorageException;
import vsl.core.vslInputException;
import vsl.core.vslConsistencyException;
import vsl.core.types.vslID;
import vsl.core.types.vslElKey;
import vsl.core.types.vslRecKey;

import vsl.core.data.*;

import vsl.backends.multimap.vslMMBackend;

import java.util.Vector;
import java.io.Serializable;

/**
 * A class to test core functionality.
 */
public class TestCoreIndex  {


	private static vsl myVsl = null;

	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			showUsage();
		}
		TestCoreIndex tester = new TestCoreIndex();
		try{
			if (args[0].equals("create"))
			{
				tester.create(args);
			}
			else if (args[0].equals("printMap"))
			{
				tester.printMap(args);
			}
			else if (args[0].equals("add"))
			{
				// TestCore1 update <config_file> <entry_id> <versionid> <new_data> <chunks>
				tester.addElement(args);
			}
			else if (args[0].equals("mod"))
			{
				// TestCore1 update <config_file> <entry_id> <versionid> <new_data> <chunks>
				tester.modElement(args);
			}
			else if (args[0].equals("show"))
			{
				// TestCore1 update <config_file> <entry_id> <versionid> <new_data> <chunks>
				tester.show(args);
			}
			/*
			else if (args[0].equals("show"))
			{
				// TestCore1 update <dbfile> <entry_id> <new_data> <chunks>
				tester.show(args);
			}
			*/
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
		System.out.println("TestCoreIndex must be called with args:\n\n "+
				"TestCoreIndex <cmd> <config_file> [cmd args]\n");
		System.out.println("Commands: create, add, mod, show, printMap\n");
		System.out.println("Cmd args:\n" +
			"\n\tcreate:\t\tcreate a new index with a single element.\n\n" +
			"\t\t\t[data]\tThe string data for the first element in the index.\n" +
			"\n\tadd:\t\tadd a new element to the index.\n\n"+
			"\t\t\t[indexID]\tThe vslID of an index where we add the element.\n"+
			"\t\t\t[data]\t\tThe string data for the first element in the index.\n" +
			"\n\tmod:\t\tupdate/delete elements in an index interactively.\n\n"+
			"\t\t\t[indexID]\tThe vslID of the index which the element is part of.\n"+
			"\n\tshow:\t\tdisplay the structure of an index interactively.\n\n"+
			"\t\t\t[indexID]\tThe vslID of the index which the element is part of.\n"+
			"\n\tprintMap**:\t[bytesToShow]\t(Number of bytes of each chunk to show.\n"+
			"\n\n**printMap takes the db_file rather than the config file as a second argument.\n");
		/*System.out.println("Cmd args:\n" +
			"\n\tcreate:\t\tcreate a new index with a single element.\n\n" +
			"\t\t\t[data]\tThe string data for the first element in the index.\n" +
			"\n\tadd:\t\tadd a new element to the index.\n\n"+
			"\t\t\t[indexID]\tThe vslID of an index where we add the element.\n"+
			"\t\t\t[data]\t\tThe string data for the first element in the index.\n" +
			"\n\tupdate:\t\tupdate an element in the index.\n\n"+
			"\t\t\t[indexID]\tThe vslID of the index which the element is part of.\n"+
			"\t\t\t[elKey]\tThe elementKey of the element to update.\n" +
			"\t\t\t[prevRecKey]\tThe recordKey of the record that should be the previous version.\n"+
			"\t\t\t[data]\t\tA string that seeds the update data.\n"+
			"\n\tprintMap**:\t[bytesToShow]\t(Number of bytes of each chunk to show.\n"+
			"\n\n**printMap takes the db_file rather than the config file as a second argument.\n"+
			"\n\tshow:\t\t[entryID]\tThe vslID of an entry to show.\n");*/
		System.exit(1);
	}

	void create(String[] args)
		throws vslException
	{
		if (args.length < 3)
		{
			System.err.println("Expected additional arguments.");
			showUsage();
		}
		String newElement = args[2];
		myVsl = new vsl(args[1]);
		vslIndex index = new vslIndex();
		vslIndexDataType<String> elData = new vslIndexDataType<String>(newElement);
		index.addElement(elData);
		myVsl.addIndex(index);
		myVsl.debugShow();
		myVsl.save();
	}
	
	
	void addElement(String[] args)
		throws Exception
	{
		if (args.length < 4)
		{
			System.err.println("Usage: add <config_file> <indexID> <string>");
			System.exit(1);
		}
	    // TestCore1 update <db_file> <entry_id> <new_data> <chunks>
	    // technically this is incorrect since vsl needs a config file
	    myVsl = new vsl(args[1]);
		//vslMMBackend db = vslMMBackend.readMap(args[1]);
		//myVsl.setBackend(db);
		vslID id = new vslID();
		id.setID(args[2]);
		vslIndex index = myVsl.getIndex(id, true);
		vslIndexDataType<String> elData = new vslIndexDataType<String>(args[3]);
		index.addElement(elData);
		myVsl.updateIndex(index);
		myVsl.save();
	}


	void modElement(String[] args)
		throws Exception
	{
		if (args.length < 3)
		{
			//System.err.println("Usage: update <config_file> <indexID> <elKey> <prevRecKey> <string>");
			System.err.println("Usage: mod <config_file> <indexID>");
			System.exit(1);
		}
	    // TestCore1 update <db_file> <entry_id> <new_data> <chunks>
	    // technically this is incorrect since vsl needs a config file
	    myVsl = new vsl(args[1]);
		//vslElKey elKey = new vslElKey();
		//elKey.setKey(args[3]);
		vslRecKey prev = new vslRecKey();
		//prev.setKey(args[4]);
		//String data = args[5];
		//vslMMBackend db = vslMMBackend.readMap(args[1]);
		//myVsl.setBackend(db);
		vslID id = new vslID();
		id.setID(args[2]);
		vslIndex index = myVsl.getIndex(id, true);
		Vector<vslElKey> allKeys = showAllElements(index);
		vslElKey elKey = null;
		// prompt returns null on no input and then we quit
		while( (elKey = promptForChoice("element to update", allKeys)) != null)  
		{
			vslIndexElement el = index.getElement(elKey);
			Vector<vslRecKey> allRec = new Vector<vslRecKey>();
			recurseVersionPrint(el.getFirst(), 1, allRec);
			while( (prev = promptForChoice("previous record to update", allRec)) != null)  
			{
				Vector prevVec = new Vector();
				prevVec.add(prev);
				vslIndexDataType<String> elData = null;
				String input = null;
				do {
					System.out.print("Update/Detele [u/d]:");
					input = FileChunkingTest.getLine();
				} while (input != null && ! (input.equals("u") || input.equals("d")));
				if (input.equals("u")) {
					System.out.print("Update data:");
					input = FileChunkingTest.getLine();
					elData = new vslIndexDataType<String>(input);
					index.updateElement(elKey, elData, prevVec);
				}
				else
				{
					index.deleteElement(elKey, prevVec);
				}
				// this might be messy since update is still kinda a hack
				myVsl.updateIndex(index);
				allRec = new Vector<vslRecKey>();
				recurseVersionPrint(el.getFirst(), 1, allRec);
			}
			allKeys = showAllElements(index);
		}
		myVsl.save();
	}


/*
	void deleteElement(String[] args)
		throws Exception
	{
		if (args.length < 6)
		{
			System.err.println("Usage: delete <config_file> <indexID> <elKey> <prevRecKey>");
			System.exit(1);
		}
	    // TestCore1 update <db_file> <entry_id> <new_data> <chunks>
	    // technically this is incorrect since vsl needs a config file
	    myVsl = new vsl(args[1]);
		vslElKey elKey = new vslElKey();
		elKey.setKey(args[3]);
		vslRecKey prev = new vslRecKey();
		prev.setKey(args[4]);
		String data = args[5];
		//vslMMBackend db = vslMMBackend.readMap(args[1]);
		//myVsl.setBackend(db);
		vslID id = new vslID();
		id.setID(args[2]);
		vslIndex index = myVsl.getIndex(id, true);
		//vslIndexDataType<String> elData = new vslIndexDataType<String>(data);
		Vector prevVec = new Vector();
		prevVec.add(prev);
		index.deleteElement(elKey, prevVec);
		myVsl.updateIndex(index);
		myVsl.save();
	}
*/

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
			System.err.println("Usage: show <config_file> <entryid>");
			System.exit(1);
		}
	    myVsl = new vsl(args[1]);
		vslID entryID = new vslID();
		entryID.setID(args[2]);
		Vector<vslID> prev = new Vector<vslID>();
		vslIndex index = myVsl.getIndex(entryID, true);
		System.out.println("Index: [" + index.getID() + "]");
		Vector<vslElKey> allKeys = showAllElements(index);
		vslElKey elKey = null;
		while( (elKey = promptForChoice("element", allKeys)) != null)  
		{
			vslIndexElement el = index.getElement(elKey);
			Vector<vslRecKey> allRec = new Vector<vslRecKey>();
			recurseVersionPrint(el.getFirst(), 1, allRec);
		}
	}


	
	Vector<vslElKey> showAllElements(vslIndex index)
		throws vslInputException
	{
		Vector<vslIndexElement> els = index.getElements();
		Vector<vslElKey> allKeys = new Vector<vslElKey>();
		for (vslIndexElement el: els) {
			showElement(el);
			allKeys.add(el.getElementKey());
		}
		return allKeys;
	}


	void showElement(vslIndexElement el) 
		throws vslInputException
	{
		System.out.print("--> Element [ " + el.getElementKey() +"] latest: (");
		StringBuffer sb = new StringBuffer();
		getLatestVersions(el.getFirst(), sb);
		System.out.println(sb.toString()  + ")");
	}


	void getLatestVersions(vslIndexView<String> cur, StringBuffer out)
		throws vslInputException
	{
		if (cur.getNextViews() == null) {
			if (cur.isDelete())
			{
				out.append(" DELETED ");
			}
			else
			{
				out.append(" ").append(cur.getData()).append(" ");
			}
		}
		else
		{
			for(vslIndexView<String> nv:  cur.getNextViews() )
			{
				getLatestVersions(nv, out);
			}
		}
		
	}

	private <D> D promptForChoice(String type, Vector<D> idList) 
	{
		String input = null;
		System.out.print("\n" + type + ": ");
		showAllIDs(idList);
		System.out.print("Enter number of " + type + " [0-" + (idList.size() -1) + "]: ");
		input = FileChunkingTest.getLine();
		if (input == null || input.equals(""))
		{
			return null;
			//System.exit(1);
		}
		try {
			D ek = idList.get(new Integer(input).intValue());
			return ek;
		}
		catch (Exception e) {
			return promptForChoice(type, idList);
		}
	}


	private void showAllIDs(Vector allIDs) {
		int index = 0;
		for (Object id: allIDs) {
			System.out.print("[" + index++ + "] " + id + ", ");
		}
		System.out.println();
	}
	
	
	private void recurseVersionPrint(vslIndexView<String> cur, int indentMult, Vector<vslRecKey> allRec)
		throws vslInputException
	{
		System.out.print(getIndent(indentMult) + "--> Record [ " + cur.getRecKey() + "] ");
		if (cur.isCreate()) {
			System.out.print("[CREATE: " + cur.getData() + "]");
		} 
		else if (cur.isUpdate()) {
			System.out.print("[CREATE: " + cur.getData() + "]");
		} 
		else  {
			System.out.print("[DELETE] ");
		} 
		System.out.print(" child of: [");
		allRec.add(cur.getRecKey());
		//allVer.add(ver.getID());
		if (cur.getPrevViews() != null)
		{
			for (vslIndexView<String> prv: cur.getPrevViews()) {
				System.out.print(prv.getRecKey() + ", ");
			}
		}
		System.out.println("]");
		if (cur.getNextViews() == null || cur.getNextViews().size() < 1) {
			return;
		}
		System.out.println(getIndent(indentMult) + "  |");
		for (vslIndexView<String> next: cur.getNextViews()) {
			recurseVersionPrint(next, indentMult+1, allRec);
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


}
