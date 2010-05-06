package org.haldean.chopper.server;

/** An Updatable to write everything to standard error */
public class EchoUpdatable implements Updatable {
    public void update(String s) {
	System.err.println("Update received: " + s);
    }
}