package org.haldean.chopper.server;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;
import javax.swing.JOptionPane;

/** A huge singleton class to receive data from the chopper that
 *  operates within its own thread */
public class DataReceiver implements Runnable {
    /* These three statements enforce singularity */
    private static DataReceiver instance = null;

    private DataReceiver() {
	/* Declared private so no one can instantiate it */
    }

    /** Get the instance of the DataReceiver class, creating
     *  one if one does not exist.
     *  @return The DataReceiver instance */
    public static DataReceiver getInstance() {
	if (instance == null) {
	    instance = new DataReceiver();
	}
	return instance;
    }

    /* Begin actual class */

    /* Server addresses and port numbers. Images are transmitted
     * over a separate socket from text, and through a different port */
    public String serverAddr;
    public int dataPort; 
    public int imgPort;
    
    /* The Socket and Reader/Writer pair for textual data */
    private Socket dataConnection;
    private BufferedReader data;
    private BufferedWriter output;

    /* The Socket and Reader for incoming images */
    private Socket imgConnection;
    private ObjectInputStream image;

    /* The objects to be updated on incoming text data */
    private LinkedList<Updatable> tied;
    /* The ImageComponent to pass retrieved images to */
    private ImageComponent imageTied;

    /* Are we connected to the server? */
    private boolean isConnected;
    /* Should we stop the communications thread */
    private boolean stopThread;

    /** Initialize this DataReceiver object, destroying all previous state.
     *  @param _serverAddr The IP address or hostname of the transmitting server
     *  @param _dataPort The port to connect to for textual data
     *  @param _imgPort The port to connect to for images */
    public void initialize(String _serverAddr, int _dataPort, int _imgPort) {
	serverAddr = _serverAddr;
	dataPort = _dataPort;
	imgPort = _imgPort;

	tied = new LinkedList<Updatable>();
	stopThread = false;
	isConnected = false;

	/* Close all open sockets */
	try {
	    if (dataConnection != null)
		dataConnection.close();
	    if (imgConnection != null)
		imgConnection.close();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /** Tie an object to the DataReceiver
     *  @param u Object to update on incoming data */
    public void tie(Updatable u) {
	tied.add(u);
    }

    /** Tie the ImageComponent to the DataReceiver
     *  @param i Component to update with new images */
    public void tieImage(ImageComponent i) {
	imageTied = i;
    }

    /** Update all tied classes
     *  @param msg The received message */
    private void updateAll(String msg) {
	/* If this message means there's an incoming image,
	 * get ready to receive it. */
	if (msg.startsWith("IMAGE"))
	    receiveImage(msg);
	for (int i=0; i<tied.size(); i++)
	    tied.get(i).update(msg);
    }

    /** Create a thread to receive an incoming image
     *  @param msg The image incoming message. This is necessary because it contains the
     *             length of the image to be received */
    private void receiveImage(String msg) {
	try {
	    /* Create a new ObjectInputStream if it doesn't already exist. */
	    if (image == null)
		image = new ObjectInputStream(imgConnection.getInputStream());
	} catch (IOException e) {
	    e.printStackTrace();
	    return;
	}

	/* Create a new ImageReceiver thread that transmits a string
	 * to the phone when it is done receiving. This will cue the phone
	 * to send the next string */
	ImageReceiver r = new ImageReceiver(image, msg, imageTied, new Callback() {
		public void completed() {
		    sendln("IMAGE:RECEIVED");
		}
	    });
	Thread imgThread = new Thread(r);
	imgThread.setName("Image receiver");
	imgThread.start();
    }

    /** @return True if connected to server, false if not */
    public boolean connected() {
	return isConnected;
    }

    /** Tell the DataReceiver to break its connection to the server */
    public void stop() {
	stopThread = true;
    }

    /** This gets called when the host is turning off. */
    public void die() {
	try {
	    sendln("SERVER:CLOSING");
	    stopThread = true;
	} catch (Exception e) {
	    /* Don't do anything -- we're killing the program anyway, and
	     * this is sort of expected to happen anyway. dataConnection
	     * should close after stopThread is set to true */
	}
    }

    /** Send a line to the phone
     *  @param s The string to send to the phone */
    public void sendln(String s) {
	try {
	    output.write(s + "\n");
	    output.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /** Run the DataReceiver thread */
    public void run() {
	Thread.currentThread().setName("Data receiver");
	Debug.log("DataReceiver thread " + Thread.currentThread().getName() + " started");
	/* This loop automatically reestablishes the connections if they die
	 * unless the stopThread flag is set true */
	while (! stopThread) {
	    try {
		/* Throw an exception if we haven't been given a server address */
		if (serverAddr == null)
		    throw new IOException();

		Debug.log("Connecting on " + serverAddr + " ports " + dataPort + " and " + imgPort);
		/* Connect data and imagery */
		dataConnection = new Socket(serverAddr, dataPort);
		imgConnection = new Socket(serverAddr, imgPort);

		/* Create Reader/Writer pair for textual data */
		data = new BufferedReader(new InputStreamReader(dataConnection.getInputStream()));
		output = new BufferedWriter(new OutputStreamWriter(dataConnection.getOutputStream())); 

		Debug.log("Connected");
		
		isConnected = true;
		/* Send a line to the server telling it we've connected */
		sendln("SERVER:HELLO");

		/* Read data in until we're told to stop or we lose the connection */
		String in;
		while (! stopThread && (in = data.readLine()) != null)
		    updateAll(in);
		    
		System.out.println("Disconnected");
		sendln("SERVER:DISCONNECTED");

		/* Close connections. We are no longer connected */
		dataConnection.close();
		imgConnection.close();
		isConnected = false;
	    } catch (IOException e) {
		isConnected = false;
		e.printStackTrace();

		Debug.log("Error initializing sockets: " + e.toString());

		/* If connecting fails, wait 5 seconds before trying again */
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
	