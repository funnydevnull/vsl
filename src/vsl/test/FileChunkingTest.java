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



	public static void main(String[] args)
	{
		if (args.length < 2)
		{
			System.err.println("Expected args: <cmd> <dbfile> ....\n"+
							"\t cmd = create, compare, list, read");
			System.exit(1);
		}
		cmd = args[0];
		dbfile = args[1];
		if (cmd.equals("create"))
		{
			if (args.length < 3)
			{
				System.err.println("Missings args: create <dbfile> <source>");
				System.exit(1);
			}
			source = args[2];
			create();
			System.exit(0);
		} 
		if (cmd.equals("list"))
		{
			list();
			System.exit(0);
		}

		if (cmd.equals("compare"))
		{
			//compare();
			System.exit(0);
		} 
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


	public static void create()
	{
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
			try{
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
