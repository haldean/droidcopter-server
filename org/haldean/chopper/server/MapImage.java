package org.haldean.chopper.server;

import java.util.LinkedList;
import java.net.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.imageio.*;

public class MapImage {
    private LinkedList<Waypoint> pointList;
    private int zoom = 16;

    /* Default size is 600px by 600px */
    private Dimension size = new Dimension(600, 600);

    private URL mapURL;
    private BufferedImage im;

    private boolean useGoogle = true;
    private final String yahooApiKey = 
	"0QJTwVbV34HRXbsIRmXTVqonVwENL0ueW9BPz6E_dcPx6crAGjrG5hQyvZp3uBc-";

    public MapImage() {
	this(null);
    }

    public MapImage(Waypoint w) {
	pointList = new LinkedList<Waypoint>();
	if (w != null) 
	    pointList.add(w);
	updateImage();
    }

    public void purgePath() {
	pointList = new LinkedList<Waypoint>();
	updateImage();
    }

    public void addWaypoint(Waypoint w) {
	/* Numerical constant is the number of Earth degrees subtended by
	 * 10 meters of surface distance */
	if (pointList.size() == 0 || 
	    w.distance(pointList.getLast()) > 0.0000898315) {
	    synchronized (pointList) {
		pointList.add(w);
	    }
	    updateImage();
	}
    }

    public Dimension getSize() {
	return size;
    }

    public void setSize(Dimension d) {
	size = d;
    }

    private void makeURL() {
	synchronized (pointList) {
	    try {
		String url;
	        if (useGoogle) {
		    /* This establishes all of the parameters and the main point */
		    url = "http://maps.google.com/maps/api/staticmap?&size=" +
		        (int) size.getWidth() + "x" + (int) size.getHeight() +
			"&markers=color:red|label:D|" + pointList.getLast().toString() +
			"&markers=color:green|label:S|" + pointList.getFirst().toString() +
			"&maptype=hybrid&sensor=true&path=color:0xFF0000CC|weight:3";
		    
		    /* Write the path data to the URL */
		    for (int i=0; i<pointList.size(); i++)
			url = url + "|" + pointList.get(i).toString();
		} else {
		    url = "http://local.yahooapis.com/MapsService/V1/mapImage?appid=" +
			yahooApiKey + "&latitude=" + pointList.getLast().toString("&longitude=") +
			"&zoom=4&image_height=" + (int) size.getHeight() + 
			"&image_width=" + (int) size.getWidth();
		}
		mapURL = new URL(url);
		Debug.log("New request URL: " + mapURL);
	    } catch (MalformedURLException e) {
		System.err.println("Malformed URL: " + e.toString());
		System.exit(-1);
	    }
	}
    }

    private void updateImage() {
	if (pointList.size() > 0) {
	    makeURL();
	    try {
		if (useGoogle)
		    im = ImageIO.read(mapURL);
		else {
		    BufferedReader result = new BufferedReader(new InputStreamReader(mapURL.openStream()));
		    String s;
		    while ((s = result.readLine()) != null) {
			if (s.contains("Result")) {
			    s = s.substring(s.indexOf(">") + 1, s.indexOf("</"));
			    Debug.log("Yahoo Response: " + s);
			    im = ImageIO.read(new URL(s));
			}
		    }
		    result.close();
		}
	    } catch (IOException e) {
		if (useGoogle)
		    useGoogle = false;
		else {
		    System.err.println("IO Exception: " + e.toString());
		    System.exit(-1);
		}
	    }
	}
    }

    public Image getImage() {
	return im;
    }
}