package vsl.test;

//import vsl.test.MMStore;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import java.util.HashMap;
import java.util.Date;
import java.util.Iterator;
//import java.util.ClassNotFoundException;

public class SerializingTest
{

	private static MMStore map;
	private static Date lastUpdate;

	private static String mapFile = "mymap";

	public static void main(String[] args)
	{
		if (args.length > 0)
		{
			if (args[0].equals("read"))
			{
				try{
					map = MMStore.readMap(mapFile);
					//System.out.println("Read in map of size: " + map.totalSize());
					System.out.println("Read in map of size: " + map.size());
					Iterator iter = map.keySet().iterator();
					while(iter.hasNext())
					{
						Object key = iter.next();
						System.out.println("[" + key + "]-->[" 
								+ map.get(key) + "]");
					}
				} catch (Exception e) {
					System.err.println("Caught exception: " + e.toString());
				}
				System.exit(0);
				
			}
			if (args[0].equals("put") && args.length > 2)
			{
				System.out.println("Reading hashtable from file: " + mapFile);
				try{
					map = MMStore.readMap(mapFile);
					//readMap();
				} catch (FileNotFoundException e)
				{
					System.out.println("File not found, will create new file.");
					map = new MMStore(mapFile);
				}
				catch (Exception e) {
					System.err.println("Caught exception: " + e.toString());
					System.exit(1);
				}
				System.out.println("Putting [" + args[1] 
						+ "]-->[" + args[2] +"]");
				map.put(args[1], args[2]);
				try{
					map.writeMap();
					System.out.println("Map has " + map.size() + " entries");
				} catch (IOException e) {
					System.err.println("Caught exception: " + e.toString());
				}
			}
			else
			{
				System.out.println("Must pass 'name' 'value' pair to put");
			}
		}
		else
		{
			System.out.println(
					"Usage: \tSerializingTest put <key> <val> \n" +
					"\t SerializingTest read"); 
		}

	}

	/*

	public static void readMap()
		throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(mapFile);
		ObjectInputStream ois = new ObjectInputStream(fis);
		lastUpdate = (Date) ois.readObject();
		map = (HashMap) ois.readObject();

		ois.close();
	}

	public static void writeMap()
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(mapFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		oos.writeObject(new Date());
		oos.writeObject(map);

		oos.close();
	}
	*/

}
