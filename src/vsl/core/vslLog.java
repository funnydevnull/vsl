package vsl.core;

public class vslLog {

	public static final int FATAL = -1;
	public static final int ERROR = 0;
	public static final int WARNING = 1;
	public static final int NORMAL = 2;
	public static final int VERBOSE = 3;
	public static final int DEBUG = 4;


	/**
	 * This class is a singleton and shouldn't be initialized.
	 */
	private vslLog()
	{

	}

	public static void log(int level, String msg)
	{
		System.out.println(msg);
	}


}
