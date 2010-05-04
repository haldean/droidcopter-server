package org.haldean.chopper.server;

import java.io.*;
import java.net.*;

public class ImageReceiver implements Runnable {
    ObjectInputStream ostream;
    int len;
    long time;
    ImageComponent imageComp;
    Callback callback;
    
    public ImageReceiver(ObjectInputStream _in, String _header, 
			 ImageComponent _imageComp, Callback _callback) {
	super();

	String fields[] = _header.split(":");
	len = new Integer(fields[1]);
	time = new Long(fields[2]);

	imageComp = _imageComp;
	ostream = _in;
	callback = _callback;

	if (ostream == null) 
	    Debug.log("ImageReceiver was given a null InputStream");
    }

    public void run() {
	Debug.log("Receiving image length " + len);
	byte[] imageData = new byte[len];

	try {
	    ostream.readFully(imageData);
	    callback.completed();

	    imageComp.setImage(imageData);
	    imageComp.setCaptureTime(time);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}