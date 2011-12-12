package vsl.test;

import vsl.util.DebuggingObjectOutputStream;

import vsl.util.DebuggingObjectOutputStream;

import org.apache.commons.collections.map.MultiValueMap;

import java.io.Serializable;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.FileInputStream;
import java.io.ObjectInputStream;

import java.util.HashMap;
import java.util.Date;
import java.util.Set;
import java.util.Iterator;
//import java.util.ClassNotFoundException;

public class MMStore extends HashMap implements Serializable
{

	//private static MultiValuedMap map;
	private Date lastUpdate;
	private String mapFile;

	//private HashMap map;

	private static final long serialVersionUID = 57L;

	public MMStore(String file)
	{
		mapFile = file;
		//map = new HashMap();
	}

	public static MMStore readMap(String file)
		throws IOException, ClassNotFoundException
	{
		FileInputStream fis = new FileInputStream(file);
		ObjectInputStream ois = new ObjectInputStream(fis);
		MMStore fmap = (MMStore) ois.readObject();
		ois.close();
		return fmap;
    }

	public void writeMap()
		throws IOException
	{
		FileOutputStream fos = new FileOutputStream(mapFile);
		ObjectOutputStream oos = new ObjectOutputStream(fos);
		//DebuggingObjectOutputStream oos = new DebuggingObjectOutputStream();
		oos.writeObject(this);
		oos.close();
	}


	public void setLastUpdate(Date last)
	{
		lastUpdate = last;
	}
/*
	public void put(Object key, Object val)
	{
		map.put(key, val);
	}


	public Object get(Object key)
	{
		return map.get(key);
	}


	public Set keySet()
	{
		return map.keySet();
	}
	*/

}

