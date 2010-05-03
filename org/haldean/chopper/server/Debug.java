package org.haldean.chopper.server;

public class Debug {
    public static boolean enable = false;
    public static Updatable debugOut;

    public static void setDebugOut(Updatable d) {
	debugOut = d;
    }

    public static void log(String s) {
	if (enable)
	    System.err.println(s);
	if (debugOut != null)
	    debugOut.update(s);
    }
}