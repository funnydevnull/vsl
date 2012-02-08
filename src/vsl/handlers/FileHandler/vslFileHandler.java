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

/**
 * 
 *
 * History:
 *
 * 20120126:  Split off helper method setUpTokensAndChunkMap() from reChunkFile(), cleaned up the
 * timing and logging code.
 *
 */

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
	public void unchunk(String outfile, Vector<vslFileDataChunk> chunks)
		throws FileNotFoundException, IOException
	{
		FileOutputStream fl = null;
	 	BufferedOutputStream bf = null;
		fl = new FileOutputStream(outfile);
		bf = new BufferedOutputStream(fl);
		unchunk(bf, chunks);
		bf.flush();
		fl.close();
	}


	/**
	 * Recoonstruct a file from its chunks and dump them in the output stream bf.
	 */
	public void unchunk(BufferedOutputStream bf, Vector<vslFileDataChunk> chunks)
	{
		boolean debug=false;
		Collections.sort(chunks);
		int counter = 0;
		for(vslFileDataChunk chunk: chunks)
		{
			try {
				String chunkCounter = "[" + counter + "]";
				if (debug) {
					bf.write(chunkCounter.getBytes(), 0, chunkCounter.getBytes().length);
				}
				byte[] data = chunk.getData();
				bf.write(data, 0, data.length);
				counter++;
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
	 *
	 * ISSUES:
	 *  - tokenSizes might be unequal -- this is not handled
	 *  - we may have identical tokens.  need to use MMap not hashMap.
	 *  - we need to renumber chunks in oldChunks
	 */
	public Vector<vslFileDataChunk> reChunkFile(String filename, 
												Vector<vslFileDataChunk> oldChunks)
		throws FileNotFoundException, vslException
	{
		vslLog.log(vslLog.DEBUG, "reChunkFile(): enter");
		vslLog.log(vslLog.DEBUG, "reChunkFile(): called with chunkSize["+
				chunkSize+"], tokenSize[" +tokenSize + "]");
		// some performance metrics
		long read_time = 0;
		long token_time = 0;
		long chunk_time = 0;
		long numReads = 0;
		long chunksFound = 0;
		long chunksMatch = 0;
		long fileSize = -1;

		// the current chunk number
		int chunkNum = 0;

		/* a lookup of beginTokens->chunk for the old chunks */
		HashMap<ByteWrapper, vslFileDataChunk> oldChunkMap = 
								new HashMap<ByteWrapper, vslFileDataChunk>(); 
		// generates a ByteDLL with tokens and also sets up a hashmap of
		// tokens->chunks (tokens wrapped in ByteBuffers).
		ByteDLL tokens = setUpTokensAndChunkMap(oldChunks, oldChunkMap);
		
		/* setup to read in the file */
		File infile = new File(filename);
		fileSize = infile.length();
		BufferedInputStream bf = new BufferedInputStream(new FileInputStream(infile));
		
		/* store the old chunks we re-find and new chunks in here 
		 * This is mostly for debugging purposes.
		 */
		Vector<vslFileDataChunk> oldFound = new Vector<vslFileDataChunk>();
		/* we store the new chunks we find here.  this is what we return. */
		Vector<vslFileDataChunk> newChunks = new Vector<vslFileDataChunk>();


		/**
		 * the buffer size is chunkSize+tokenSize so when we get to the end of
		 * it if we still haven't found a token we can just create a new chunk
		 * from the first chunkSize bits while leaving tokenSize to be reread
		 * in case it contains part of a token.
		 */
		int maxBuffer  = chunkSize + tokenSize;

		// going to add read in bytes to here
		int inChar = 0;
		ByteBuffer readBuf = ByteBuffer.allocate(maxBuffer);
		// buffer to read chunk into for comparison
		// we temporarily use this array to store a begin token to key the chunks hashmap off
		byte[] foundTok = new byte[tokenSize];
		// a temporary array to read in old chunks once we've found a match				
		byte[] chunkRead = new byte[chunkSize];

		long begin = System.nanoTime();
		//System.out.print("Progress: [");

		int position = -1;
		while (true)
		{
			try {
				inChar = bf.read();
				/* we got to the end of the file so copy whatever is in the
				 * buffer to a new chunk (had it been an old chunk we'd have
				 * seen a token by now).
				 */
				if (inChar == -1)
				{
					position = readBuf.position();
					if (position > 0) {
						vslFileDataChunk newChunk = 
							 new vslFileDataChunk(chunkNum++, tokenSize, 
								 	readBuf.array(), 0, position);
						vslLog.log(vslLog.DEBUG, 
							"Making NEW CHUNK at ENF OF FILE from 0 to position=" + position 
								+ " at numReads=" + numReads + ",  size[" + 
								+ newChunk.getData().length + "]");
								newChunks.add(newChunk);
					}
					//break out of the while loop
					break;
				}
				long t1 = System.nanoTime();
				readBuf.put((byte)inChar);
				long t2 = System.nanoTime();
				read_time += t2 - t1;
				position = readBuf.position();
				// the beginning of the last putative token
				int offset = position - tokenSize;
				byte[] readArray = readBuf.array();
				// check if the last tokenSize bytes matches a token
				if (position >= tokenSize) 
					//&& ! tokens.prestruct[token[offset] + 128][token[offset+1]+128][token[offset+2]+128]) 
				{
					long t3 = System.nanoTime();
					List<byte[]> matches = tokens.matches(readArray, 
													offset, tokenSize);
					long t4 = System.nanoTime();
					token_time += t4 - t3;
					long t5 = System.nanoTime();
					// got a match so lets check for the full data chunk
					if (matches != null) {
						vslLog.log(vslLog.DEBUG, "Found match: offset[" + offset + 
								"], numReads[" + numReads + "], position[" + position+"]"); 
						chunksFound++;
						// copy the current token into the chunk input
						System.arraycopy(readArray, offset, chunkRead, 0, tokenSize);
						// copy the current token into an array of its own
						System.arraycopy(readArray, offset, foundTok, 0, tokenSize);
						// set a mark but only allow it to be valid for a short read limit
						bf.mark(chunkSize-tokenSize+1);
						// if we find a match we don't reset the stream so we
						// just keep reading from beyond that
						vslFileDataChunk oldChunk = oldChunkMap.get(new ByteWrapper(foundTok));
						//	vslLog.log(vslLog.DEBUG, "Read in [" + readin + 
						//		"] and old chunk minus token is length " 
						//		+ (oldChunk.dataLength() - tokenSize));
						int readin=-1;
						/**
						 * check we've read in the full old chunk and it
						 * matches what we had before note we need to make sure
						 * we readin enough otherwise the end of chunkRead
						 * could be leftover from a previous iteration.
						 */
						// read the rest of the chunk into tmp
						if (oldChunk != null && 
							(readin = bf.read(chunkRead, tokenSize, (int) (oldChunk.dataLength() -tokenSize)))
							 == oldChunk.dataLength() - tokenSize &&
								subEquals(oldChunk.getData(), chunkRead, (int) oldChunk.dataLength()) ) 
						{
							/**
							 * If there was data before we hit the token
							 * (before offset) then we create a new chunk from
							 * whatever was left in the buffer before we found
							 * a token (i.e. up to offset).  vslChunk.setData
							 * copies data out of the array so we should have
							 * to worry about dirtying things.
							 *
							 * Afterwards we reset the buffer.
							 */
							if (offset > 0) {
								vslFileDataChunk newChunk = 
									 new vslFileDataChunk(chunkNum++, tokenSize, 
											 	readArray, 0, offset);
								newChunks.add(newChunk);
								vslLog.log(vslLog.DEBUG, 
										"Making NEW CHUNK from 0 to offset=" + offset 
										+ " at numReads=" + numReads + ",  size[" + 
										+ newChunk.getData().length + "], chunkNum [" + newChunk.getChunkNum()+ "]");
							}
							vslLog.log(vslLog.DEBUG, "Found old chunk in pos: " + chunkNum 
									+ " (old pos [" + oldChunk.getChunkNum()+ "]) at byte [" + numReads + 
									"].  Found [" + oldChunk.dataLength() + "] matching bytes.");
							// we found a match so lets note that
							chunksMatch++;
							// we reset the number on the oldChunk since it may have moved.
							oldChunk.setChunkNum(chunkNum++);
							oldFound.add(oldChunk);
							numReads += readin;
							// reset the buffer, first copying off
							// tokenSize elemnts after offset
							//readBuf = resetBuffer(readBuf, maxBuffer, offset);
							//MISTAKE ABOVE: no need to copy anything we've
							//used everything we've read
							readBuf = ByteBuffer.allocate(maxBuffer);
						}
						else 
						{
							// no match so we rewind the file to the mark
							bf.reset();
						}
						/**
						 * UNHANDLED CASE!!!
						 */
						if (matches.size() > 1) {
							System.err.println("Found [" + matches.size() + 
									"] matching tokens at position [" + position + "]");
						}
					}
					long t6 = System.nanoTime();
					chunk_time += t6 - t5;
				}
				numReads++;
				if ( numReads % (fileSize/20) == 0) 
				{
					//System.out.print(".");
					//System.out.flush();
				}
				/* when we get to the end of the byte buffer we reset it but copy in the last full
				 * token's worth of bytes in case a token crosses one buffer to
				 * another.  Since the first chunkBytes of data definitely
				 * don't match a known chunk we create a new chunk for them.
				 * The last tokenSize bytes may yet be part of a token which is
				 * why we copy them into the new buffer.
				 */
				if (position > maxBuffer - 1) {
					// copy the first chunkSize bytes into a new chunk
					vslFileDataChunk newChunk = 
							 new vslFileDataChunk(chunkNum++, tokenSize, readArray, 0, chunkSize);
					vslLog.log(vslLog.DEBUG, "Making new chunk from full buffer: size[" +
										+ newChunk.getData().length+ "], end position [" + numReads + "]");
					newChunks.add(newChunk);
					// reset the buffer, first copying off tokenSize elemnts after offset
					readBuf = resetBuffer(readBuf, maxBuffer, offset);
				}
			} catch (IOException e) {
				System.out.println("Caught excption reChunking: " + e.toString());
			}
		}
		vslLog.log(vslLog.DEBUG, "Found " + oldFound.size() + " old chunks.");
		oldChunks.clear();
		oldChunks.addAll(oldFound);
		vslLog.log(vslLog.DEBUG, "reChunkFile(): exit");
		return newChunks;
	}


	/* ------------------ PRIVATE HELPERS ------------------------ */

	/**
	 * Reset the bufer first copying the last tokenSize elements, starting at
	 * offset, into the new buffer and return the new buffer.  
	 *
	 * @param	oldBuf	The old buffer we wanna copy out of.
	 * @param	bufSize	The size to make the new buffer.
	 * @param	offset	Where to start reading to copy out a tokenSize chunk
	 * from the old buffer.
	 *
	 * @return 	A new ByteBuffer with the first tokenSize bytes of the new
	 * buffer corresponding to the offset to offset+tokenSize bytes of the old
	 * buffer.
	 */
	private ByteBuffer resetBuffer(ByteBuffer oldBuf, int bufSize, int offset)
	{
		// now we prepare the buffer again, copying in the last token
		byte[] lastTok = new byte[tokenSize];
		//copy last tokenSize elements into array
		System.arraycopy(oldBuf.array(), offset, lastTok, 0, tokenSize);
		ByteBuffer readBuf = ByteBuffer.allocate(bufSize);
		readBuf.put(lastTok);
		return readBuf;
	}

	/**
	 * A helper method that takes the old chunks and extracts their beginTokens
	 * from them as well as placing them in a HashMap keyed off a ByteBuffer
	 * wrapping their beginToken.  This allows us to quickly find which chunk
	 * matches a particular token.
	 */
	private ByteDLL setUpTokensAndChunkMap(Vector<vslFileDataChunk> oldChunks, 
									HashMap<ByteWrapper, vslFileDataChunk> oldChunkMap)
		throws vslException
	{
		vslLog.log(vslLog.DEBUG, "Size of chunkset: " + oldChunks.size());
		// some debug vars
		int debugmax = 3;
		int debugcount = 3;
		// the ByteDLL we will generate to represent the tokens.
		ByteDLL tokens = null;
		/* populate a ByteDLL with the old tokens for fast reading */
		Vector<byte[]> byteTokens = new Vector<byte[]>();
		for(vslFileDataChunk old: oldChunks)
		{
			// tokenSize should already be set in the class
			//if (tokenSize < 0) tokenSize = old.getTokenSize();
			if (debugcount++ < debugmax) 
				vslLog.log(vslLog.DEBUG, "Adding token[" +debugcount + "]: " 
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
					"Exception generated trying to read tokens from oldChunks" +
					" into ByteDLL: " + e.toString());
			vslLog.logException(e);
			throw new vslException(e);
		}
		return tokens;
	}
	

	/**
	 * Compare only the first "len" bytes of a two byte arrays.
	 */
	private boolean subEquals(byte[] a1, byte[] a2, int len) 
	{
		if (a1.length == a2.length && a1.length==len) 
			return Arrays.equals(a1, a2);
		int i=-1;
		for(i = 0; i < len && i < a1.length && i < a2.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		if (i == len) return true;
		return false;
	}

}
