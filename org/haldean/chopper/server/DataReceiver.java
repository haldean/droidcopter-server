package org.haldean.chopper.server;

import java.net.*;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;

/* A singleton class to receive data from the chopper */
public class DataReceiver implements Runnable {
    private static DataReceiver instance = null;

    private DataReceiver() {
	/* Used to enforce singularity */
    }

    public static DataReceiver getInstance() {
	if (instance == null) {
	    instance = new DataReceiver();
	}
	return instance;
    }

    /* Begin actual class */

    public String serverAddr;
    public int dataPort; 
    
    private Socket dataConnection;
    private BufferedReader data;
    private BufferedWriter output;

    private LinkedList<Updatable> tied;
    private boolean isConnected;
    private boolean stopThread;

    public void initialize(String _serverAddr, int _dataPort) {
	serverAddr = _serverAddr;
	dataPort = _dataPort;

	tied = new LinkedList<Updatable>();
	stopThread = false;
	isConnected = false;

	try {
	    if (dataConnection != null)
		dataConnection.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void tie(Updatable u) {
	tied.add(u);
    }

    private void updateAll(String msg) {
	for (int i=0; i<tied.size(); i++)
	    tied.get(i).update(msg);
    }

    public boolean connected() {
	return isConnected;
    }

    public void stop() {
	stopThread = true;
    }

    public void die() {
	try {
	    sendln("Server is dying.");
	    stopThread = true;
	} catch (Exception e) {
	    /* Don't do anything -- we're killing the program anyway, and
	     * this is sort of expected to happen anyway. dataConnection
	     * should close after stopThread is set to true */
	}
    }

    public void sendln(String s) {
	try {
	    output.write(s + "\n");
	    output.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void run() {
	Debug.log("DataReceiver thread " + Thread.currentThread().getName() + " started");
	while (! stopThread) {
	    try {
		if (serverAddr == null)
		    throw new IOException();

		dataConnection = new Socket(serverAddr, dataPort);
		data = new BufferedReader( new InputStreamReader(dataConnection.getInputStream()));
		output = new BufferedWriter(new OutputStreamWriter(dataConnection.getOutputStream())); 
		
		isConnected = true;
		sendln("Can I get in on this party?");

		String in;
		while (! stopThread && (in = data.readLine()) != null)
		    updateAll(in);
		System.out.println("Disconnected");
		sendln("Server disconnected.");

		dataConnection.close();
		isConnected = false;
	    } catch (IOException e) {
		isConnected = false;
		System.err.println("Error initializing sockets: " + e.toString());
		JOptionPane.showMessageDialog(null, "Error: Could not connect to control server. " +
					      "Will sleep 2 seconds then retry", "Error", JOptionPane.ERROR_MESSAGE);
		try {
		    Thread.sleep(2000);
		} catch (InterruptedException exception) {
		    return;
		}
	    } 
	}
	isConnected = false;
	stopThread = false;
    }
}
	