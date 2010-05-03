package org.haldean.chopper.server;

import java.io.*;
import java.net.*;

public class ImageReceiver implements Runnable {
    ObjectInputStream ostream;
    int len;
    ImageComponent imageComp;
    
    public ImageReceiver(ObjectInputStream _in, Integer _len, ImageComponent _imageComp) {
	super();

	len = _len;
	imageComp = _imageComp;
	ostream = _in;

	if (ostream == null) 
	    Debug.log("ImageReceiver was given a null InputStream");
    }

    public void run() {
	Debug.log("Receiving image length " + len);
	byte[] imageData = new byte[len];

	try {
	    ostream.readFully(imageData);
	    imageComp.setImage(imageData);

	    Debug.log("Image received");
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}