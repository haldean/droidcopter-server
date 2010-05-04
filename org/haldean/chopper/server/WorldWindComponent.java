/* I LOVE YOU NASA */

package org.haldean.chopper.server;

import javax.swing.*;
import java.awt.*;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.util.*;

public class WorldWindComponent extends JPanel {
    private WorldWindowGLCanvas wwd;

    public WorldWindComponent() {
	super(new BorderLayout());

	this.wwd = this.createWorldWindow();
	this.wwd.setPreferredSize(canvasSize);

	// Create the default model as described in the current worldwind properties.
	Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
	this.wwd.setModel(m);

	// Setup a select listener for the worldmap click-and-go feature
	this.wwd.addSelectListener(new ClickAndGoSelectListener(this.getWwd(), WorldMapLayer.class));

	this.add(this.wwd, BorderLayout.CENTER);
    }
    
    protected WorldWindowGLCanvas createWorldWindow()
    {
	return new WorldWindowGLCanvas();
    }

    public WorldWindowGLCanvas getWwd()
    {
	return wwd;
    }

    public StatusBar getStatusBar()
    {
	return statusBar;
    }

    public static void main(String args[]) {
	JFrame f = new JFrame("World Wind Test");
	WorldWindComponent w = new WorldWindComponent();
	f.add(w);
	f.pack();
	f.setVisible(true);
    }
}