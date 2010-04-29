package org.haldean.chopper.server;

import java.awt.*;
import javax.swing.*;

/** A component that shows a path overlaid on a Google map */
public class LocationComponent extends JComponent {
    MapImage im;

    public LocationComponent() {
	im = new MapImage();
    }

    public LocationComponent(Waypoint w) {
	im = new MapImage(w);
    }

    /* Paints the image in the MapImage onto the Graphics
     * object. If no waypoints have been added, the image is
     * null, so we check for this case */
    public void paint(Graphics g) {
	if (im.getImage() != null) 
	    g.drawImage(im.getImage(), 0, 0, null);
    }

    /* The preferred size of the component is just the 
     * size of the image */
    public Dimension getPreferredSize() {
	return im.getSize();
    }

    public void setSize(Dimension d) {
	im.setSize(d);
    }

    /* Add a waypoint to the path. Forces a repaint of the image */
    public void addWaypoint(Waypoint w) {
	im.addWaypoint(w);
	repaint();
    }

    public void purgePath() {
	im.purgePath();
	repaint();
    }

    /* Test LocationComponent */
    public static void main(String[] args) throws Exception {
	JFrame f = new JFrame("Test LocationComponent");
	/* Declared as final so it can be manipulated after
	 * the frame has been made visible */
	final LocationComponent lc = new LocationComponent();
	f.add(lc);

	/* Show the frame */
	f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	f.pack();
	f.setVisible(true);

	/* "Animate" the addition of three waypoints */
	lc.addWaypoint(new Waypoint(40.80823,-73.96413));
	Thread.sleep(1000);
	lc.addWaypoint(new Waypoint(40.80601,-73.96554));
	Thread.sleep(1000);
	lc.addWaypoint(new Waypoint(40.80359,-73.96711));
    }
}