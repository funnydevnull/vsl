package vsl.handlers.FileHandler;

import vsl.core.vslException;
import vsl.core.vslLog;

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

public class vslFileHandler {
	
	
	private int chunkSize = -1;
	private int tokenSize = -1;


	public vslFileHandler(int chunkSize, int tokenSize) {
		this.chunkSize = chunkSize;
		this.tokenSize = tokenSize;
	}




	/* ------------------ FileChunking methods ------------------------*/

	/**
	 * These methods should eventually be moved to the vslFileHander as well as some
	 * of the internal logic of the methods above.
	 */


	/**
	 * Recoonstruct a file from its chunks and dump them in the output stream bf.
	 */
	public void unchunk(BufferedOutputStream bf, Vector<vslFileDataChunk> chunks)
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

	/**
	 * Chunk the named file using the chunklength and tokenlength defined as static
	 * fields in this class.
	 */
	public Vector<vslFileDataChunk> chunkFile(String filename)
		throws FileNotFoundException
	{
		BufferedInputStream bf = new BufferedInputStream(new FileInputStream(filename));
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
				//BUG HERE: should be reading at a counter times chunkSize
				read = bf.read(tmp, 0, chunkSize);
				chunk =  new vslFileDataChunk(chunkNum++, tokenSize, tmp, 0, read);
				//chunk.setData(read, tmp);
				String digest = (chunk.getDigest() != null) ? new String(chunk.getDigest()) : ""; 
				System.out.println("Got chunk [" + chunkNum + "];  size: " 
									+ chunk.dataLength() + ", digest: [" + digest + "]");
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



	/**
	 * IMPLEMENTATION INCOMPLETE!!
	 *
	 * Chunk a file in a way that tries to maximize the overlap with an old chunking
	 * by using the tokens on those chunks to try to define our new chunks.
	 *
	 *
	 * NOTE: Still UNIMPLEMENTED
	 */
	public Vector<vslFileDataChunk> reChunkFile(String filename, 
												Vector<vslFileDataChunk> oldChunks)
		throws FileNotFoundException, vslException
	{
		// some debug vars
		int debugmax = 3;
		int debugcount = 3;
		// some performance metrics
		long list_time = 0;
		long get_time = 0;
		long numReads = 0;
		long chunksFound = 0;
		long chunksMatch = 0;
		long fileSize = -1;

		// the size of the begin tokens.  these are assumed to all be the same
		int tokenSize=-1;
		/* populate a ByteDLL with the old tokens for fast reading */
		Vector<byte[]> byteTokens = new Vector<byte[]>();
		/* a lookup of beginTokens->chunk for the old chunks */
		HashMap<ByteWrapper, vslFileDataChunk> oldChunkMap = 
								new HashMap<ByteWrapper, vslFileDataChunk>(); 
		ByteDLL tokens = null;
		System.out.println("Size of chunkset: " + oldChunks.size());
		for(vslFileDataChunk old: oldChunks)
		{
			if (tokenSize < 0) tokenSize = old.getTokenSize();
			if (debugcount++ < debugmax) 
				System.out.println("Adding token[" +debugcount + "]: " 
						+ new String(old.getBeginToken()));
			byteTokens.add(old.getBeginToken());
			/* store a lookup from the beginToken to the chunk
			 * this is used to check for the chunk in the file if we find the beginToken.
			 */
			oldChunkMap.put(new ByteWrapper(old.getBeginToken()), old);
		}
		try {
			tokens = ByteDLL.fromBytes(byteTokens);
		} catch (Exception e) {
			vslLog.log(vslLog.ERROR, 
					"Exception generated trying to read file tokens for file [" 
					+ filename + "] into ByteDLL: " + e.toString());
			vslLog.logException(e);
			throw new vslException(e);
		}

		/* setup to read in the file */
		File infile = new File(filename);
		fileSize = infile.length();
		BufferedInputStream bf = new BufferedInputStream(new FileInputStream(infile));
		
		/* store the old chunks we refind and new chunks in here */
		Vector<vslFileDataChunk> oldFound = new Vector<vslFileDataChunk>();
		Vector<vslFileDataChunk> newChunks = new Vector<vslFileDataChunk>();

		/*
		int read = chunkSize;
		int chunkNum = 0;
		byte[] tmp = new byte[chunkSize];
		*/

		// we scan for chunks by reading into a buffer and then scanning the buffer.  
		int maxBuffer  = 300 * tokenSize;

		// going to add read in bytes to here
		int read = 0;
		ByteBuffer readBuf = (maxBuffer > 0) ? ByteBuffer.allocate(maxBuffer) : null;
		// buffer to read chunk into for comparison
		// we temporarily use this array to store a begin token to key the chunks hashmap off
		byte[] foundTok = new byte[tokenSize];

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
						vslFileDataChunk oldChunk = oldChunkMap.get(new ByteWrapper(foundTok));
						/*if (oldChunk != null)
						{
							byte[] init = new byte[30];
							System.arraycopy(chunkRead, 0, init, 0, 30);
							System.out.println("Read: [" + new String(init) + "]");
							System.arraycopy(oldChunk, 0, init, 0, 30);
							System.out.println("Old:  [" + new String(init) + "]");
						}*/
						if (oldChunk != null && Arrays.equals(oldChunk.getData(), chunkRead) ) {
							// we found a match so we note that and keep scanning the file from here
							chunksMatch++;
							oldFound.add(oldChunk);
						} else {
							// no match so we rewind the file to the mark
							bf.reset();
						}
						/**
						 * UNHANDLED CASE!!!
						 */
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

		// OLD
		/* we keep reading so long as we get back chunkSizes, 
		 * after that the file is done.*
		while (read == chunkSize)
		{
			vslFileDataChunk chunk;
			try {
				read = bf.read(tmp, 0, chunkSize);
				chunk =  new vslFileDataChunk(chunkNum++, tokenSize, tmp, 0, read);
				//chunk.setData(read, tmp);
				System.out.println("Got chunk [" + chunkNum + "];  size: " 
									+ chunk.dataLength() + ", digest: ["
									+ new String(chunk.getDigest()) + "]");
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
		*/
		return oldChunks;
	}


	


}
