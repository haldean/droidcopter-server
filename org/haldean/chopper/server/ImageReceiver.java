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
	Debug.log("Receiving image");
	byte[] imageData = new byte[len];

	try{
	    /* Copy buffer contents to byte array */
	    in.readFully(imageData);
	    imageComp.setImage(imageData);
	    in.close();
	    Debug.log("Image received");
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }
}