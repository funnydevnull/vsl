package vsl.test;

//import vsl.test.MMStore;
import vsl.core.vsl;
import vsl.core.vslStorageException;

import vsl.handlers.FileHandler.vslFileHandler;
import vsl.handlers.FileHandler.vslFileDataType;
import vsl.handlers.FileHandler.vslFileDataChunk;
import vsl.handlers.FileHandler.byteUtils.ByteDLL;
import vsl.handlers.FileHandler.byteUtils.ByteWrapper;

import java.io.File;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;


import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;


import java.nio.ByteBuffer;

/**
 * A test class for prototyping file chunking.  Work in progress....
 *
 * TODO:
 * - Seperate out cmdline logic from chunking backend. 
 *   ==> chunking code should go to vslFileHandler.
 * - Move generic methods to vslFileHandler and call them from here.
 *   - chunkFile
 *   - unchunk
 *   - compare
 *  - After re-writing this class should mostly have "UI" logic-- part cmd line
 *    args and call backend.
 */
public class FileChunkingTest {

	//private static int chunkSize = 100*1000;
	//private static int tokenSize = 1000;
	//private static int chunkSize = 10*1000;
	//private static int tokenSize = 1000;
	private static int chunkSize = 500;
	private static int tokenSize = 50;
	private static MMStore db;

	private static String cmd;
	private static String source;
	//private static String dbfile;

	private final static HashMap cmds = new HashMap();


	private static vsl core;


	public static void init()
	{
		cmds.put("create", new Integer(1));
		cmds.put("list", new Integer(2));
		cmds.put("compare", new Integer(3));
		cmds.put("reconstruct", new Integer(4));
	}

	public static void initVSL(String dbfile)
		throws vslStorageException
	{
		core = new vsl(dbfile);
	}


	public static void main(String[] args)
	{
		init();
		if (args.length < 2)
		{
			System.err.println("Expected args: <cmd> <dbfile> ....\n"+
							"\t cmd = create, compare, list, read");
			System.exit(1);
		}
		cmd = args[0].toLowerCase();
		String dbfile = args[1];
		try {
			initVSL(dbfile);
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
		Integer cmdint = (Integer) cmds.get(cmd);
		switch(cmdint)
		{
			case 1:			create(args); 			break;
			/*
			case 2:			list();					break;
			case 3:			unimplemented();		break;
			case 4:			reconstruct(args);		break;
			*/
		}
		System.exit(0);
	}


	/* --------------- UI METHODS ------------------- */


	/**
	 * methods called to implement UI requests.
	 */

	public static void unimplemented()
	{
		System.err.println("Command '" + cmd + "' not yet implemented.");
		System.exit(1);
	}

	/**
	 * List content of db file.
	 *
	public static void list()
	{
		try {
			db = MMStore.readMap(dbfile);
		} catch (FileNotFoundException e) {
			System.out.println("File " + dbfile + " does not exist.");
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
		Iterator iter = db.keySet().iterator();
		System.out.println("Files:");
		while(iter.hasNext())
		{
			Object key = iter.next();
			System.out.println("[" + key + "]:  Chunks: [" 
						+ ((Vector)db.get(key)).size() + "]");
		}
	}*/

	
	/**
	 * Populate db entry from a file passed as second arg.
	 */
	public static void create(String[] args)
	{
		Vector<vslFileDataChunk> chunks = null;
		if (args.length < 3)
		{
			System.err.println("Missings args: create <dbfile> <source>");
			System.exit(1);
		}
		source = args[2];
		try {
			/*
			try {
				db = MMStore.readMap(dbfile);
			} catch (FileNotFoundException e) {
				System.out.println("File " + dbfile + " does not exist.  Will create.");
				db = new MMStore(dbfile);
			}
			*/
			vslFileHandler handler = new vslFileHandler(chunkSize, tokenSize);
			chunks = handler.chunkFile(source);
			vslFileDataType fileData = new vslFileDataType();
			StringTokenizer st = new StringTokenizer(source, "/");
			String fname = "UNSET";
			while (st.hasMoreTokens()) {
				fname = st.nextToken();
			}
			fileData.setName(fname);
			for (vslFileDataChunk chunk: chunks) {
				fileData.addChunk(chunk);
			}
			//db.put(fname, chunks);
			//db.writeMap();
			core.addEntry(fileData);
			core.debugShow();
			core.save();

		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}

	/**
	 * Dump db entry to a file passed as third cmd line arg.
	 *
	public static void reconstruct(String[] args)
	{
		Vector<vslFileDataChunk> chunks = null;
		FileOutputStream fl = null;
	 	BufferedOutputStream bf = null;
		if (args.length < 4)
		{
			System.err.println("Missings args: reconstruct <dbfile> <dbentry> <output_file>");
			System.exit(1);
		}
		source = args[2];
		String outfile = args[3];
		try {
			fl = new FileOutputStream(outfile);
			bf = new BufferedOutputStream(fl);
		}
		catch(FileNotFoundException e)
		{
			System.err.println("Cannot write to file '" + outfile + "'");
			System.err.println("Excption: " + e.toString());
			System.exit(1);
		}
		try {
			try {
				db = MMStore.readMap(dbfile);
			} catch (FileNotFoundException e) {
				System.err.println("Database " + dbfile + " does not exist. Exiting.");
				System.exit(1);
			}
			chunks = (Vector<vslFileDataChunk>) db.get(source);
			if (chunks == null)
			{
				System.err.println("No entry corresponding to '" + source 
						+ "' found in dbfile '" + dbfile + "'. Exiting.");
				System.exit(1);
			}
			vslFileHandler handler = new vslFileHandler(chunkSize, tokenSize);
			handler.unchunk(bf, chunks);
			bf.flush();
			fl.close();
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}
	*/

	/**
	 *  read off args and compare a file in DB with a new version passed as just 
	 *  a filename.
	 *
	 *  TODO:
	 *  - write comparison code
	 *  - Split method: read args + input/chunk file
	 *  				compare chunks in a seperate method
	 *
	public static void compare(String[] args)
	{
		Vector<vslFileDataChunk> oldChunks = null;
		Vector<vslFileDataChunk> newChunks = null;
		if (args.length < 4)
		{
			System.err.println("Missings args: compare <dbfile> <dbentry> <new_version>");
			System.exit(1);
		}
		source = args[2];
		String newver = args[3];
		vslFileHandler handler = new vslFileHandler(chunkSize, tokenSize);
		try {
			newChunks = handler.chunkFile(newver);	
		}
		catch(FileNotFoundException e)
		{
			System.err.println("Cannot read from file '" + newver + "'");
			System.err.println("Excption: " + e.toString());
			System.exit(1);
		}
		try {
			try {
				db = MMStore.readMap(dbfile);
			} catch (FileNotFoundException e) {
				System.err.println("Database " + dbfile + " does not exist. Exiting.");
				System.exit(1);
			}
			oldChunks = (Vector<vslFileDataChunk>) db.get(source);
			if (oldChunks == null)
			{
				System.err.println("No entry corresponding to '" + source 
						+ "' found in dbfile '" + dbfile + "'. Exiting.");
				System.exit(1);
			}
			System.out.println("old chunks: " + oldChunks);
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			e.printStackTrace();
			System.exit(1);
		}
	}
	*/

}
