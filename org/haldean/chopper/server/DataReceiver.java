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
    public int imgPort;
    
    private Socket dataConnection;
    private BufferedReader data;
    private BufferedWriter output;

    private Socket imgConnection;
    private ObjectInputStream image;

    private LinkedList<Updatable> tied;
    private ImageComponent imageTied;
    private boolean isConnected;
    private boolean stopThread;

    public void initialize(String _serverAddr, int _dataPort, int _imgPort) {
	serverAddr = _serverAddr;
	dataPort = _dataPort;
	imgPort = _imgPort;

	tied = new LinkedList<Updatable>();
	stopThread = false;
	isConnected = false;

	try {
	    if (dataConnection != null)
		dataConnection.close();
	    if (imgConnection != null)
		imgConnection.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    public void tie(Updatable u) {
	tied.add(u);
    }

    public void tieImage(ImageComponent i) {
	imageTied = i;
    }

    private void updateAll(String msg) {
	if (msg.startsWith("IMAGE"))
	    receiveImage(msg);
	for (int i=0; i<tied.size(); i++)
	    tied.get(i).update(msg);
    }

    private void receiveImage(String msg) {
	String[] messageParts = msg.split(":");
	ImageReceiver r = new ImageReceiver(image, new Integer(messageParts[1]), imageTied);
	new Thread(r).start();
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

		Debug.log("Connecting on " + serverAddr + " ports " + dataPort + " and " + imgPort);
		dataConnection = new Socket(serverAddr, dataPort);
		imgConnection = new Socket(serverAddr, imgPort);

		data = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
		output = new BufferedWriter(new OutputStreamWriter(dataConnection.getOutputStream())); 

		image = new ObjectInputStream(imgConnection.getInputStream());
		
		isConnected = true;
		sendln("Can I get in on this party?");

		String in;
		while (! stopThread && (in = data.readLine()) != null)
		    updateAll(in);
		System.out.println("Disconnected");
		sendln("Server disconnected.");

		dataConnection.close();
		imgConnection.close();
		isConnected = false;
	    } catch (IOException e) {
		isConnected = false;
		e.printStackTrace();
		System.err.println("Error initializing sockets: " + e.toString());
		try {
		    Thread.sleep(5000);
		} catch (InterruptedException exception) {
		    return;
		}
	    } 
	}
	isConnected = false;
	stopThread = false;
    }
}
	