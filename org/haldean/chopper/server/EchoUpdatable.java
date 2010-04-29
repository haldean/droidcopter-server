package org.haldean.chopper.server;

public class EchoUpdatable implements Updatable {
    public void update(String s) {
	System.err.println("Update received: " + s);
    }
}