package vsl.test;

import vsl.test.byteUtils.ByteDLL;
import vsl.test.byteUtils.ByteWrapper;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

import java.util.Iterator;
import java.util.Vector;
import java.util.StringTokenizer;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Arrays;

import java.text.DecimalFormat;

public class ByteHashSpeedTest {



	public static void main(String[] args)
		throws Exception
	{
		if (args.length < 1) {
			System.out.println("Must specify commend: <testByte> or <testRechunk>");
			System.exit(-1);
		}
		String[] rest = new String[args.length - 1];
		for (int x = 0; x < rest.length; x++) {
			rest[x] = args[x+1];
		}
		if (args[0].equals("testByte")) {
			testByteDLL(rest);	
		}
		else if (args[0].equals("testRechunk")) {
			testRechunk(rest);	
		}
		else {
			System.out.println("Must specify commend: <testByte> or <testRechunk>");
			System.exit(-1);
		}
	}



	public static void testByteDLL(String[] args) 
		throws Exception
	{
		if (args == null || args.length < 1) {
			System.out.println("Must provide equal length strings to build ByteDLL.\n");
			System.out.println("   testByte str1 str2 str3 ...\n");
			System.out.println("NOTE: unequal length strings will give inconsistent results.");
			System.exit(-1);
		}
		ByteHashSpeedTest tester = new ByteHashSpeedTest();
		Vector<String> strings = new Vector<String>();
		for (String str: args) {
			strings.add(str);
		}
		ByteDLL head = ByteDLL.fromStrings(strings);
		head.printOut();
		System.out.println();
		String searchStr = null;
		BufferedReader terminal = new BufferedReader(new InputStreamReader(System.in));
		while(searchStr == null || ! searchStr.equals(":q")) {
			System.out.print("String to search for (^c or \":q\" to quit): ");
			System.out.flush();
			int inp = 0;
			//while( (inp = System.in.read()) != (int) '\n') {
			//		searchStr.append(inp);
			//}
			searchStr = terminal.readLine();
			List<byte[]> matches = head.matches(searchStr.getBytes(), 0, searchStr.getBytes().length);
			if (matches != null) {
				System.out.print("Found [" + matches.size() + "] matches: ");
				for (byte[] word: matches) {
					System.out.print(new String(word) + " ");
				}
				System.out.println();
			}
		}
	}


	public static void testRechunk(String[] args)
		throws Exception
	{
		if (args.length < 3)
		{
			System.err.println("Expected args for [testRechunk]: <file> <chunk_size> <token_size>....\n\n"+
							"\t file = name of file to chunk.\n" +
							"\t chunk_size = size of chunks in bytes.\n "+
							"\t token_size = size of begin tokens in bytes (should be less thank chunk_size). \n\n" +
							"e.g.  testRechunk myfile 10000 100");
			System.exit(1);
		}
		String filename = args[0];
		int chunkSize = new Integer(args[1]).intValue();
		int tokenSize = new Integer(args[2]).intValue();
		File infile = new File(filename);
		System.out.println("Chunk Size: " + chunkSize);
		System.out.println("Token Size: " + tokenSize);
		System.out.println("");
		System.out.println("======================================");
		System.out.println("Reading");
		System.out.println("======================================");
		System.out.println("First read: Loads file into OS Cache...");
		chunk(infile, chunkSize, -1, false, null);
		System.out.println("Second read: file cached so less disk write overhead...");
		chunk(infile, chunkSize, -1, false, null);
		System.out.println("======================================");
		System.out.println("TreeMap");
		System.out.println("======================================");
		System.out.println("Third read: storing tokens to treemap...");
		//TreeMap<ByteWrapper, byte[]> chunkTreeMap = new TreeMap<ByteWrapper, byte[]>();
		//we're not gonna do treemap for now
		HashMap<ByteWrapper, byte[]> chunkTreeMap = new HashMap<ByteWrapper, byte[]>();
		chunk(infile, chunkSize, tokenSize, true, chunkTreeMap);
		//System.out.println("Fourth read: searching for matching tokens...");
		//reChunk(infile, chunkSize, tokenSize, chunkTreeMap);
		System.out.println("======================================");
		System.out.println("HashMap");
		System.out.println("======================================");
		System.out.println("Fifth read: storing tokens to hashmap...");
		//lets pre-size our hashmap to a reasonable size
		int estChunks = (int) infile.length()/chunkSize + 10;
		System.out.println("Presizing hashtable to size: " + estChunks);
		HashMap<ByteWrapper, byte[]> chunkHashMap = new HashMap<ByteWrapper, byte[]>(estChunks);
		chunk(infile, chunkSize, tokenSize, true, chunkHashMap);
		//System.out.println("Sixth read: searching for matching tokens...");
		//reChunk(infile, chunkSize, tokenSize, chunkHashMap);
		System.out.println("Seventh read: using ByteDLL to match tokens...");
		reChunkByteDLL(infile, chunkSize, tokenSize, chunkHashMap);
	}

	/**
	 * This method chunks the file and, optionally, stores the beginTokens of the chunks into a hashmap
	 */
	static void chunk(File infile, int chunkSize, int tokenSize, 
						boolean createHash, AbstractMap<ByteWrapper, byte[]> chunks)
		throws Exception
	{
		byte[] tmp = new byte[chunkSize];
		// initialized to null or to the tokenSize if positive
		byte[] beginToken = (tokenSize > 0) ? new byte[tokenSize] : null;
		// we need this because primitive arrays don't deep has but LinkedList's do.
		//LinkedList tokenArray = (tokenSize >0) ? new LinkedList() : null;
		ByteBuffer tokenArray = (tokenSize >0) ? ByteBuffer.allocate(tokenSize) : null;
		BufferedInputStream bf = new BufferedInputStream(new FileInputStream(infile));
		/* we keep reading so long as we get back chunkSizes, 
		 * after that the file is done.*/
		int read = chunkSize;
		long begin = System.nanoTime();
		long copy_time = 0;
		long put_time = 0;
		int numReads = 0;
		int debugMax = 6;
		int debugCounter = 6;
		int hashKey = 0;
		while (read == chunkSize)
		{
			try {
				//read = bf.read(tmp, 0, chunkSize);
				read = bf.read(tmp);
				if (tokenSize > 0)
				{
					System.arraycopy(tmp, 0, beginToken, 0, tokenSize);
				}
				if (createHash) {
					if (chunks == null || beginToken == null)
					{
						throw new Exception(
								"Chunk() function called with incorrect arguments: tokenSize [" 
								+ new Integer(tokenSize));
					}
					if (debugCounter < debugMax) {
						System.out.println("");
						System.out.println("Adding key to hashmap:\n=============\n"
								+ new String(beginToken) + "\n============");
						//System.out.println("Adding val to hashmap:\n=============\n" 
						//		+ new String(tmp) + "\n==============");
						System.out.println("Bytes left to read: " + bf.available());
						System.out.println("");
					}
					long t1 = System.nanoTime();
					tokenArray.clear();
					//for (int i =0; i < tokenSize; i++) tokenArray.add(beginToken[i]);
					tokenArray.put(beginToken);
					tokenArray.rewind();
					long t2 = System.nanoTime();
					// need to worry about overhead of Arrays.asList method
					if (debugCounter++ < debugMax) {
						System.out.println("");
						System.out.println("Using keys:\n=============\n"
								+ new String(tokenArray.array()) + "\n============");
						System.out.println("");
					}
					ByteWrapper bw = new ByteWrapper(tokenArray.array());
					if (bw.hashCode() == hashKey) {
						System.out.println("Doubled hashkey!");
						System.exit(-1);
					}
					hashKey = bw.hashCode();
					chunks.put(bw, tmp);
					tmp = new byte[chunkSize];
					/*
					String t = new String(tokenArray.array());
					byte[] tr = t.getBytes();
					for(int i = 0; i < tokenArray.array().length; i++) {
						if (tokenArray.array()[i] != tr[i]) {
							System.out.println("mismatch: [" + tr[i] + "] != [" + 
									tokenArray.array()[i] + "]");
							System.out.println("mismatch: [" + (char) tr[i] + "] != [" + 
									(char) tokenArray.array()[i] + "]");
							System.exit(-1);
						}
					}*/
					tokenArray = ByteBuffer.allocate(tokenSize);
					long t3 = System.nanoTime();
					copy_time += t2 - t1;
					put_time += t3 - t2;
				}
				numReads++;
			} catch (IOException e) {
				System.out.println("Caught excption chunking: " + e.toString());
			}
		}
		long end = System.nanoTime();
		System.out.println("Read [" + new Integer(numReads++) + 
					"] blocks of size [" + new Integer(chunkSize) +"]");
		long diff = end-begin;
		double sec = diff/1000000000;
		// see javadoc for this class for hint on fromatting argument
		DecimalFormat scientific = new DecimalFormat("0.#####E0");
		if (createHash) {
			System.out.println("Created Hashtable of size: " + chunks.size());
		}
		System.out.println("Total time taken: " + new Long(diff)  
				+ " nanoseconds (" + scientific.format(sec) + " seconds)");
		if (createHash) {
			long copy_percent = (100* copy_time)/diff;
			long put_percent = (100* put_time)/diff;
			System.out.println("Copy time: [" + new Long(copy_time)  
					+ " nanoseconds, " + copy_percent + "%], \t Put time [" + new
					Long(put_time)  + " nanoseconds, " + put_percent+"%]");
		}
		System.out.println("");
	}


	static void reChunkByteDLL(File infile, int chunkSize, int tokenSize,
			AbstractMap<ByteWrapper, byte[]> chunks)
		throws Exception
	{
		if (tokenSize < 1) throw new Exception("TokenSize < 1 in reChunk");
		int debugcount = 6;
		int debugmax = 6;
		// the max buffer size before clearing the buffer
		int maxBuffer  = 300 * tokenSize;
		long fileSize = infile.length();
		long list_time = 0;
		long get_time = 0;
		long numReads = 0;
		long chunksFound = 0;
		long chunksMatch = 0;
		int debugMax = 3;
		int debugCounter = 3;
		/* we keep reading so long as we get back chunkSizes, 
		 * after that the file is done.*/
		// going to add read in bytes to here
		int read = 0;

		
		Vector<byte[]> byteTokens = new Vector<byte[]>();
		ByteDLL tokens = null;
		ByteBuffer readBuf = (maxBuffer > 0) ? ByteBuffer.allocate(maxBuffer) : null;
		FileInputStream fs = new FileInputStream(infile);
		BufferedInputStream bf = new BufferedInputStream(fs);
		// buffer to read chunk into for comparison
		// we temporarily use this array to store a begin token to key the chunks hashmap off
		byte[] foundTok = new byte[tokenSize];

		System.out.println("Size of chunkset: " + chunks.size());
		for(ByteWrapper key: chunks.keySet())
		{
			if (debugcount++ < debugmax) 
				System.out.println("Adding token[" +debugcount + "]: " + new String(key.data));
			//byteTokens.add(iter.next().array());
			byteTokens.add(key.data);
		}
		tokens = ByteDLL.fromBytes(byteTokens);
		long begin = System.nanoTime();
		System.out.print("Progress: [");
		System.out.flush();
		while (read != -1)
		{
			try {
				read = bf.read();
				long t1 = System.nanoTime();
				readBuf.put((byte)read);
				int position = readBuf.position();
				int offset = position - tokenSize;
				byte[] readArray = readBuf.array();
				if (position >= tokenSize) {
						//&& ! tokens.prestruct[token[offset] + 128][token[offset+1]+128][token[offset+2]+128]) {
					List<byte[]> matches = tokens.matches(readArray, 
													position - tokenSize, tokenSize);
					// got a match so lets check for the full data chunk
					if (matches != null) {
						chunksFound++;
						byte[] chunkRead = new byte[chunkSize];
						// copy the current token into the chunk input
						System.arraycopy(readArray, position-tokenSize, chunkRead, 0, tokenSize);
						// copy the current token into an array of its own
						System.arraycopy(readArray, position-tokenSize, foundTok, 0, tokenSize);
						// set a mark but only allow it to be valid for a short read limit
						bf.mark(chunkSize-tokenSize+1);
						// read the rest of the chunk into tmp
						bf.read(chunkRead, tokenSize, chunkSize-tokenSize);
						// if we find a match we don't reset the stream so we just keep reading from beyond that
						byte[] oldChunk = chunks.get(new ByteWrapper(foundTok));
						/*if (oldChunk != null)
						{
							byte[] init = new byte[30];
							System.arraycopy(chunkRead, 0, init, 0, 30);
							System.out.println("Read: [" + new String(init) + "]");
							System.arraycopy(oldChunk, 0, init, 0, 30);
							System.out.println("Old:  [" + new String(init) + "]");
						}*/
						if (oldChunk != null && Arrays.equals(oldChunk, chunkRead) ) {
							// we found a match so we note that and keep scanning the file from here
							chunksMatch++;
						} else {
							// no match so we rewind the file to the mark
							bf.reset();
						}
						if (matches.size() > 1) {
							System.out.println("Found [" + matches.size() + 
									"] matching tokens at position [" + position + "]");
						}
					}
					//tokenArray.remove();
					// point our lastTok to the last elements of the buffer
					//lastTok = ByteBuffer.wrap(tokenArray.array(), position - tokenSize, tokenSize);
				}
				long t2 = System.nanoTime();
				//if (lastTok != null && chunks.get(lastTok) != null) chunksFound++;
				long t3 = System.nanoTime();
				list_time += t2 - t1;
				get_time += t3 - t2;
				numReads++;
				if ( numReads % (fileSize/20) == 0) System.out.print(".");
				System.out.flush();
				// when we get to the end of the byte buffer we reset it but copy in the last full
				// token's worth of bytes in case a token crosses one buffer to another
				if (position > maxBuffer - 1) {
					byte[] lastTok = new byte[tokenSize];
					//copy last tokenSize elements into array
					System.arraycopy(readBuf.array(), position-tokenSize, lastTok, 0, tokenSize);
					readBuf = ByteBuffer.allocate(maxBuffer);
					readBuf.put(lastTok);
				}
			} catch (IOException e) {
				System.out.println("Caught excption reChunking: " + e.toString());
			}
		}
		System.out.println("done]");
		long end = System.nanoTime();
		System.out.println("Read [" + new Long(numReads++) + "] bytes.");
		long diff = end-begin;
		double sec = diff/1000000000;
		// see javadoc for this class for hint on fromatting argument
		DecimalFormat scientific = new DecimalFormat("0.#####E0");
		System.out.println("Number of matching beginTokens: " + chunksFound);
		System.out.println("Number of fully matching chunks: " + chunksMatch);
		System.out.println("Total time taken: " + new Long(diff)  
				+ " nanoseconds (" + scientific.format(sec) + " seconds)");
		long list_percent = (100* list_time)/diff;
		long get_percent = (100* get_time)/diff;
		System.out.println("List manipulation time: [" + new Long(list_time)  
				+ " nanoseconds, " + list_percent + "%], \t Get time [" + new Long(get_time)  
				+ " nanoseconds, " + get_percent+"%]");
		long avg_depth = tokens.depth/tokens.num;
		long avg_down = tokens.down/tokens.num;
		System.out.println("Average letter depth down ByteDLL: [" + new Long(avg_depth)  
				+"], \t Avg length down alphabetical word list [" + new Long(avg_down) +"]");
		long prefail_percent = (100*tokens.prefail)/tokens.num;
		System.out.println("Prefail percentage: " + prefail_percent + "%");
		System.out.println("");
	}


/*
	static void reChunk(File infile, int chunkSize, int tokenSize,
			AbstractMap<ByteBuffer, byte[]> chunks)
		throws Exception
	{
		if (tokenSize < 1) throw new Exception("TokenSize < 1 in reChunk");
		// the max buffer size before clearing the buffer
		int maxBuffer  = 300 * tokenSize;
		long fileSize = infile.length();
		long list_time = 0;
		long get_time = 0;
		long numReads = 0;
		long chunksFound = 0;
		int debugMax = 3;
		int debugCounter = 3;
		// we need this because primitive arrays don't deep has but LinkedList's do.
		//LinkedList tokenArray = (tokenSize >0) ? new LinkedList() : null;
		ByteBuffer tokenArray = (maxBuffer > 0) ? ByteBuffer.allocate(maxBuffer) : null;
		FileInputStream fs = new FileInputStream(infile);
		BufferedInputStream bf = new BufferedInputStream(fs);
		/* we keep reading so long as we get back chunkSizes, 
		 * after that the file is done.*
		int read = 0;
		long begin = System.nanoTime();
		System.out.print("Progress: [");
		ByteBuffer lastTok = null;
		while (read != -1)
		{
			try {
				//read = bf.read(tmp, 0, chunkSize);
				read = bf.read();
				long t1 = System.nanoTime();
				//tokenArray.add((byte)read);
				tokenArray.put((byte)read);
				int position = tokenArray.position();
				if (position > tokenSize) {
					//tokenArray.remove();
					// point our lastTok to the last elements of the buffer
					lastTok = ByteBuffer.wrap(tokenArray.array(), position - tokenSize, tokenSize);
				}
				long t2 = System.nanoTime();
				if (lastTok != null && chunks.get(lastTok) != null) chunksFound++;
				long t3 = System.nanoTime();
				list_time += t2 - t1;
				get_time += t3 - t2;
				numReads++;
				if ( numReads % (fileSize/20) == 0) System.out.print(".");
				System.out.flush();
				if (position > maxBuffer - 1) {
					byte[] tmp = new byte[tokenSize];
					//copy last tokenSize elements into array
					System.arraycopy(tokenArray.array(), position-tokenSize, tmp, 0, tokenSize);
					tokenArray = ByteBuffer.allocate(maxBuffer);
					tokenArray.put(tmp);
				}
				/*
				long t1 = System.nanoTime();
				MappedByteBuffer mb = fs.map(FileChannel.MapMode.READ_ONLY, 0, tokenSize);
				long t2 = System.nanoTime();
				if (chunks.get(tokenArray) != null) chunksFound++;
				long t3 = System.nanoTime();
				list_time += t2 - t1;
				get_time += t3 - t2;
				numReads++;
				if ( numReads % (fileSize/20) == 0) System.out.print(".");
				System.out.flush();
				*
			} catch (IOException e) {
				System.out.println("Caught excption reChunking: " + e.toString());
			}
		}
		System.out.println("done]");
		long end = System.nanoTime();
		System.out.println("Read [" + new Long(numReads++) + "] bytes.");
		long diff = end-begin;
		double sec = diff/1000000000;
		// see javadoc for this class for hint on fromatting argument
		DecimalFormat scientific = new DecimalFormat("0.#####E0");
		System.out.println("Number of matching chunks: " + chunksFound);
		System.out.println("Total time taken: " + new Long(diff)  
				+ " nanoseconds (" + scientific.format(sec) + " seconds)");
		long list_percent = (100* list_time)/diff;
		long get_percent = (100* get_time)/diff;
		System.out.println("List manipulation time: [" + new Long(list_time)  
				+ " nanoseconds, " + list_percent + "%], \t Get time [" + new Long(get_time)  + " nanoseconds, " 
				+ get_percent+"%]");
		System.out.println("");
	}
*/




}
