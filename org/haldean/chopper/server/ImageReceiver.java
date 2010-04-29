package org.haldean.chopper.server;

import java.io.*;

public class ImageReceiver implements Runnable {
    ObjectInputStream in;
    int len;
    ImageComponent imageComp;
    
    public ImageReceiver(ObjectInputStream _in, Integer _len, ImageComponent _imageComp) {
	in = _in;
	len = _len;
	imageComp = _imageComp;
    }

    public void run() {
	byte[] imageData = new byte[len];

	try{
	    /* Copy buffer contents to byte array */
	    in.readFully(imageData);
	} catch (IOException e) {
	    e.printStackTrace();
	}

	imageComp.setImage(imageData);
    }
}