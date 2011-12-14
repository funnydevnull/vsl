package vsl.test;

import vsl.test.MMStore;

import vsl.handlers.FileHandler.vslFileDataChunk;

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

public class FileChunkingTest {

	//private static int chunkSize = 100*1000;
	//private static int tokenSize = 1000;
	private static int chunkSize = 10*1000;
	private static int tokenSize = 1000;
	private static Vector<vslFileDataChunk> chunks;
	private static MMStore db;

	private static String cmd;
	private static String source;
	private static String dbfile;

	private final static HashMap cmds = new HashMap();


	public static void init()
	{
		cmds.put("create", new Integer(1));
		cmds.put("list", new Integer(2));
		cmds.put("compare", new Integer(3));
		cmds.put("reconstruct", new Integer(4));
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
		dbfile = args[1];
		Integer cmdint = (Integer) cmds.get(cmd);
		switch(cmdint)
		{
			case 1:			create(args); 			break;
			case 2:			list();					break;
			case 3:			unimplemented();		break;
			case 4:			reconstruct(args);		break;

		}
		System.exit(0);
	}

	public static void unimplemented()
	{
		System.err.println("Command '" + cmd + "' not yet implemented.");
		System.exit(1);
	}

	public static void list()
	{
		try {
			db = MMStore.readMap(dbfile);
		} catch (FileNotFoundException e) {
			System.out.println("File " + dbfile + " does not exist.");
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
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
	}


	public static void create(String[] args)
	{
		if (args.length < 3)
		{
			System.err.println("Missings args: create <dbfile> <source>");
			System.exit(1);
		}
		source = args[2];
		BufferedInputStream fl;
		try {
			try {
				db = MMStore.readMap(dbfile);
			} catch (FileNotFoundException e) {
				System.out.println("File " + dbfile + " does not exist.  Will create.");
				db = new MMStore(dbfile);
			}
			fl = new BufferedInputStream(new FileInputStream(source));
			chunks = chunk(fl);
			StringTokenizer st = new StringTokenizer(source, "/");
			String fname = "UNSET";
			while (st.hasMoreTokens()) {
				fname = st.nextToken();
			}
			db.put(fname, chunks);
			db.writeMap();

		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			System.exit(1);
		}
	}

	public static void reconstruct(String[] args)
	{
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
			unchunk(bf, chunks);
			bf.flush();
			fl.close();
		} catch (Exception e) {
			System.err.println("Caught exception: " + e.toString());
			System.exit(1);
		}
	}


	public static void unchunk(BufferedOutputStream bf, Vector<vslFileDataChunk> chunks)
	{
		Collections.sort(chunks);
		for(vslFileDataChunk chunk: chunks)
		{
			try {
				byte[] data = chunk.getData();
				bf.write(data, 0, data.length);
			} catch (IOException e) {
				System.out.println("Caught excption chunking: " + e.toString());
			}
		}
	}

	public static Vector<vslFileDataChunk> chunk(BufferedInputStream bf)
	{
		Vector<vslFileDataChunk> ch = new Vector<vslFileDataChunk>();
		int read = chunkSize;
		int chunkNum = 0;
		byte[] tmp = new byte[chunkSize];
		/* we keep reading so long as we get back chunkSizes, 
		 * after that the file is done.*/
		while (read == chunkSize)
		{
			vslFileDataChunk chunk;
			try {
				read = bf.read(tmp, 0, chunkSize);
				chunk =  new vslFileDataChunk(chunkNum++, read, tokenSize, tmp);
				chunk.setData(read, tmp);
				System.out.println("Got chunk [" + chunkNum + "];  size: " + chunk.getLength());
				String bt = new String(chunk.getBeginToken());
				String et = new String(chunk.getEndToken());
				System.out.println("beginToken: " + bt);
				System.out.println("endToken: " + et  + "\n=====================");
				ch.add(chunk);
			} catch (IOException e) {
				System.out.println("Caught excption chunking: " + e.toString());
			}
		}
		return ch;
	}

}
