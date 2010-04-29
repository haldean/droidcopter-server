package org.haldean.chopper.server;

public class Debug {
    public static boolean enable = false;
    public static void log(String s) {
	if (enable)
	    System.err.println(s);
    }
}