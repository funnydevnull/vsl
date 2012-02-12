package vsl.core;

import java.util.Properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 *
 * Format is:
 *
 * <subsystem>.<implementation>.<property>
 *
 * Implementation and subsystem names might be case sensitive but ideally
 * paramters should not be.
 *
 * e.g.
 *
 * backends.vslMMBackend.filename
 *
 * handlers.vslFileHandler.chunksize
 * handlers.vslFileHandler.tokensize
 *
 */

public class vslConfig {

	Properties config = new Properties();
		

	/* ---------- CORE CONFIG STRINGS  ----------------- */


	public final static String BACKEND = "core.backend";
	public final static String LOGGER = "core.logger";


	// we don't allow instantiation without passing a filename
	private vslConfig()
	{
		// do nothing
	}

	public vslConfig(String configFile)
		throws vslConfigException
	{
		try {
			File cfile = new File(configFile);
			if (! cfile.exists() )
			{
				throw new vslConfigException("Could not find file: " + configFile);
			}
			FileInputStream conf = new FileInputStream(configFile);
			config.load(conf);
		}
		catch(IOException e)
		{
			// Logging not yet initalized so we don't log
			System.err.println("Error reading config file:");
			e.printStackTrace();
			throw new vslConfigException(e);
		}
	}

	public int getInt(String key) 
		throws vslConfigException 
	{
		String val = getVal(key);
		try {
			return new Integer(val).intValue();
		}
		catch(Exception e)
		{
			vslLog.log(vslLog.ERROR, 
				"Configuration key [" + key + "] returned non-integer value: " + val);
			throw new vslConfigException(e);
		}
	}

	public String getString(String key) 
		throws vslConfigException 
	{
		return getVal(key);
	}

	/* ------------ PRIVATE HELPER METHODS ----------------- */

	private String getVal(String key) 
		throws vslConfigException
	{
		String val = null;
		if (key != null)
		{
			val = config.getProperty(key);
			vslLog.log(vslLog.NORMAL, "Config: [" + key + "]-->[" + val +"]");
		}
		else 
		{
			throw new vslConfigException("null key request from vslConfig.");
		}
		return val;
	}


}
