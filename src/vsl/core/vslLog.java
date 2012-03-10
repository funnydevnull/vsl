package vsl.core;


import java.util.HashMap;



public class vslLog {


	/* ----------- CONFIG STRINGS ------------- */

	public static final String LOGFILE = "logger.vslLog.logfile";
	public static final String LOGLEVEL = "logger.vslLog.level";

	/* ------------ LOGLEVELS ----------------- */

	public static final int FATAL = -1;
	public static final int ERROR = 0;
	public static final int WARNING = 1;
	public static final int INFO = 3;
	public static final int NORMAL = 4;
	public static final int VERBOSE = 5;
	public static final int DEBUG = 6;
	public static final int PERF = 7;



	private static HashMap<String, Integer> levels = null;

	private static int logLevel = WARNING;

	/**
	 * This class is a singleton and shouldn't be initialized.
	 */
	private vslLog()
	{

	}


	static void init(vslConfig config)
		throws vslConfigException
	{
		levels = new HashMap<String, Integer>();
		levels.put("fatal", new Integer(FATAL));
		levels.put("error", new Integer(ERROR));
		levels.put("warning", new Integer(WARNING));
		levels.put("info", new Integer(INFO));
		levels.put("normal", new Integer(NORMAL));
		levels.put("verbose", new Integer(VERBOSE));
		levels.put("debug", new Integer(DEBUG));
		levels.put("perf", new Integer(PERF));
		String debugLevel = config.getString(LOGLEVEL);
		Integer logInt =  null;
		if (debugLevel != null 
				&& (logInt = levels.get(debugLevel.toLowerCase())) != null )
		{
			logLevel = logInt.intValue();
			log(INFO, "Logging at log-level: " + logLevel);
		}
		// default to normal
		else
		{
			logLevel = 	NORMAL;
			log(INFO, "No log level set: logging at level NOMRAL.");
		}
	}

	
	private static String getLocationString()
	{
   		StackTraceElement st = Thread.currentThread().getStackTrace()[3];
		String className = st.getFileName();
		className = className.substring(0, className.length() - 5);
		return new String("[" + className + ":" + st.getLineNumber() + "]:  ");
	}

	public static void log(int level, String msg)
	{
		if (level <= logLevel) {
			System.out.println(getLocationString() + msg);
		}
	}
	
	

	public static void logException(Exception e)
	{
		e.printStackTrace();
	}


}
