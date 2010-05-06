package org.haldean.chopper.server;

/** A class to write debug information to an updatable,
 *  and optionally to write it to standard error */
public class Debug {
    public static boolean enable = false;
    private static Updatable debugOut;

    /** Set the component to update with debug messages
     *  @param d The component to update when a debug message is received */
    public static void setDebugOut(Updatable d) {
	debugOut = d;
    }

    /** Log a debug message
     *  @param s The message to debug */
    public static void log(String s) {
	/* Send it to standard error if enabled */
	if (enable)
	    System.err.println(s);
	/* Send it to the updatable if set */
	if (debugOut != null)
	    debugOut.update(s);
    }
}