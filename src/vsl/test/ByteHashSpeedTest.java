package vsl.test;

import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;

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

import java.text.DecimalFormat;

public class ByteHashSpeedTest {

	public static void main(String[] args)
		throws Exception
	{
		if (args.length < 3)
		{
			System.err.println("Expected args: <file> <chunk_size> <token_size>....\n"+
							"\t file = name of file to chunk.\n" +
							"\t chunk_size = size of chunks in bytes.\n "+
							"\t token_size = size of begin tokens in bytes (should be less thank chunk_size). ");
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
		TreeMap<ByteBuffer, byte[]> chunkTreeMap = new TreeMap<ByteBuffer, byte[]>();
		chunk(infile, chunkSize, tokenSize, true, chunkTreeMap);
		System.out.println("Fourth read: searching for matching tokens...");
		reChunk(infile, chunkSize, tokenSize, chunkTreeMap);
		System.out.println("======================================");
		System.out.println("HashMap");
		System.out.println("======================================");
		System.out.println("Five read: storing tokens to hashmap...");
		//lets pre-size our hashmap to a reasonable size
		int estChunks = (int) infile.length()/chunkSize + 10;
		System.out.println("Presizing hashtable to size: " + estChunks);
		HashMap<ByteBuffer, byte[]> chunkHashMap = new HashMap<ByteBuffer, byte[]>(estChunks);
		chunk(infile, chunkSize, tokenSize, true, chunkHashMap);
		System.out.println("Sixth read: searching for matching tokens...");
		reChunk(infile, chunkSize, tokenSize, chunkHashMap);
	}


	static void chunk(File infile, int chunkSize, int tokenSize, 
						boolean createHash, AbstractMap<ByteBuffer, byte[]> chunks)
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
		int debugMax = 3;
		int debugCounter = 3;
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
					if (debugCounter++ < debugMax) {
						System.out.println("");
						System.out.println("Adding key to hashmap:\n=============\n"
								+ new String(beginToken) + "\n============");
						System.out.println("Adding val to hashmap:\n=============\n" 
								+ new String(tmp) + "\n==============");
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
					chunks.put(tokenArray, tmp);
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
		 * after that the file is done.*/
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
				*/
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


}
