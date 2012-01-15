package vsl.test.byteUtils;

import java.util.Iterator;
import java.util.Vector;
import java.util.LinkedList;
import java.util.List;
import java.util.Collections;
import java.util.Comparator;

import vsl.test.byteUtils.ByteComparator;

/**
 * a byte doubly linked list built from a byte[] array.
 *
 * one direction traverses the sorted byte array (ord) while the other
 * traverses each word (word). 
 *
 * For instance the words: heavy heart given gives hesse
 *
 * yield:
 *
 * [g]-->[i]-->[v]-->[e]-->[n]
 *  |                       |
 *  |                      \/
 *  |                      [s]
 *  |
 * \/
 * [h]-->[e]-->[a]-->[r]-->[t]
 *              |     |
 *              |    \/
 *              |    [v]-->[y]
 *              |
 *             \/
 *             [s]-->[s]-->[e]
 *
 */
public class ByteDLL {

	byte letter;
	//all the words that match this byte sequence
	LinkedList<byte[]> matching = null;
	// move to prev byte in word
	ByteDLL prevInWord = null;
	// move to next byte in word
	ByteDLL nextInWord = null;
	// prev/next letters alphabetically 
	ByteDLL prevLet = null;
	ByteDLL nextLet = null;


	public boolean[][][] prestruct;

	// profiling vars
	
	// total depth down the word-letters
	public long depth=0;
	// total depth down the alphabetical letters
	public long down=0;
	// number of times match fails cause of preselection
	public long prefail=0;
	// total number of calls
	public long num=0;

	/**
	 * Generate a doubly-linked byte array from "input" recursively.  The input
	 * to this constructor must be sorted and all elements must be of the same
	 * length otherwise the output will not be consistent.  The sorting must be
	 * sequential (i.e. based on any ordering but implemented letter by
	 * letter).
	 *
	 * @param	input		A _sorted_ list of byte[] arrays all of the same length!
	 *	
	 * @param	atLetter	The letter corresponding to the current
	 * 						position in the recursive call.  Should be
	 * 						smaller than the length of each word in words.
	 * 
	 * @param	atElement	The letter corresponding to the current position in the recursive call.
	 *
	 * @param	wPrev		A pointer to the previous letter in the current word (null for first letter).
	 *
	 * @param	lPrev		A pointer to the previous letter found
	 *						alphabetically in the same letter sequence.
	 *
	 */
	private ByteDLL(int atLetter, int atElement, List<byte[]> words, ByteDLL wPrev, 
			ByteDLL lPrev, boolean[][][] pre) 
		throws Exception
	{
		this.prevInWord = wPrev;
		this.prevLet = lPrev;
		// if this is the head element lets create a prestruct to search
		if (pre == null) {
			prestruct = new boolean[256][256][256];
		}
		else
		{
			prestruct = pre;
		}
		// get the word at the current element
		byte[] first = words.get(atElement);
		// we've read through all words so we're done
		// NOTE: we assume words are ALL the same length
		if (atLetter > first.length - 1) {
			throw new Exception("atLetter[" + atLetter + ": larger than word [" + first.length + "]");
		}
		letter = first[atLetter];
		// populate the prestruct at the first letter of each new word
		if (atLetter < 3) {
			// convert first three letters of word into indices between 0-255
			// note: bytes values from -128 to 127 so must add offset
			prestruct[first[0] + 128][first[1] + 128][first[2] + 128] = true;
		}
		matching = new LinkedList();
		while(atElement < words.size())
		{
			byte[] next = words.get(atElement);
			if(next[atLetter] == letter)
			{
				atElement++;
				matching.add(next);
			}
			else
			{
				break;
			}
		}
		// next byte in word has no previous letters (by construction since
		// "words" and hence "matching" is sorted) and we're its prev letter
		// in word
		if (atLetter  < first.length - 1) {
			nextInWord = new ByteDLL(atLetter + 1, 0, matching, this, null, prestruct);
		}
		// we already incredmented atElement above
		if (atElement < words.size())
		{
			// next letter start at atElement in the word list
			// it shares the same prevInWord as us but we're its prevLet
			nextLet = new ByteDLL(atLetter, atElement, words, prevInWord, this, prestruct);
		}
	}


	public static ByteDLL fromStrings(Vector<String> strings)
		throws Exception
	{
		Collections.sort(strings);
		Vector bytes = new Vector();
		for (String arg: strings)
		{
			bytes.add(arg.getBytes());
		}
		ByteDLL head = new ByteDLL(0, 0, bytes, null, null, null);
		return head;
	}

	public static ByteDLL fromBytes(List<byte[]> bytes)
		throws Exception
	{
		ByteComparator bc = new ByteComparator();
		System.out.println("About to sort [" + bytes.size() +  "] elements of size ["
							+ bytes.get(0).length + "]");
		Collections.sort(bytes, bc);
		System.out.println("Sorted [" + bytes.size() + "] elements, putting into ByteDLL.");
		ByteDLL head = new ByteDLL(0, 0, bytes, null, null, null);
		return head;
	}

	public void printOut()
	{
		System.out.print("[" + (char) letter + " {");
		for (byte[] word: matching) {
			System.out.print(" " + new String(word) + " ");
		}
		System.out.print("} ]");
		if(nextInWord != null) {
			System.out.print("-->");
			nextInWord.printOut();
		}
		ByteDLL prev = prevInWord;
		StringBuffer whiteSpace = new StringBuffer();
		while (prev != null)
		{
			whiteSpace.append("      ");
			prev = prev.prevInWord;
		}
		if(nextLet != null)
		{
			System.out.println("\n" + whiteSpace + " |\n" + whiteSpace + "\\/");
			System.out.print(whiteSpace);
			nextLet.printOut();
		}
	}


	/**
	 * Return the elements matching a particular byte token.  The method
	 * attempts to be as efficient as possible in matching the token to
	 * elements of this ByteDLL.
	 *
	 * We trade safety checking for speed.  In particular token must have token.length > 3 but we do not check this!
	 *
	 * 
	 *
	 * @param	token	A byte array of min length 3 which is matched against
	 * the byte arrays encoded in this ByteDLL.  To allow efficient external
	 * memory use the search can be made for a subarray of token by specifying
	 * an offset and a length.  For efficiency the internal implementaiton
	 * _requires_ that token.length > offset + 3 and this is _not_ safety
	 * checked.
	 *
	 * 	@param	offset	Where to start looking in token.  Note this is assumed
	 * 	to satisfy token.length > offset+3 and failing this will generate a
	 * 	null pointer exception.
	 *
	 * 	@param	len		How many bytes to search through in token (after offset).  This should
	 *				 	be less than the length of the bytes in	the ByteDLL.
	 *
	 * @return	A list of byte array's matching the query or null if nothing found.
	 */
	public List<byte[]> matches(byte[] token, int offset, int len)
		throws IndexOutOfBoundsException
	{
		num++;
		// we have a quick pre-selection to check if first three chars of token match anything
		if (!prestruct[token[offset] + 128][token[offset+1]+128][token[offset+2]+128]) {
			prefail++;
			return null;
		}
		int endlen = offset + len - 1;
		if (endlen + 1> token.length) {
			throw new IndexOutOfBoundsException("ByteDLL.matches() called with offset + len [" +
					offset + " + " + len + "] > token.length [" + token.length +"].");
		}
		// to go fast we avoid recursive method calls but do things by hand!
		ByteDLL cur = this;
		int curElement = offset;
		while (cur != null) {
			// we've matched to the end of the token so return the matching of the cur element.
			byte curLet = token[curElement];
			if (curLet == cur.letter) {
				// we match the last letter in the token so return success
				if (curElement == endlen) {
					return cur.matching;
				}
				// word still goes on so we need to keep matching
				if (cur.nextInWord != null) {
					curElement++;
					depth++;
					cur = cur.nextInWord;
					continue;
					//return nextInWord.matches(token, curElement+1)
				}
				else
				{
					/* 
					 * NOTE: as usual we assume all words are the same length;
					 */
					//we're done with the word and we matched all the way to the end 
					//so we return the matches
					return cur.matching;
				}
			}
			// the current letter didn't match this letter so we pass it on to the next letter
			// or report failure
			if (cur.nextLet != null) {
				down++;
				cur = cur.nextLet;
				continue;
				//return nextLet.matches(token, curElement);
			}
			else 
			{
				//got to last possible letter -- fail
				return null;
			}
		}
		return null;
	}



}


