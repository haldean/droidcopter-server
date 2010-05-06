/* I LOVE YOU NASA */

package org.haldean.chopper.server;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

/* WorldWind imports. I love NASA. */
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.awt.*;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.*;
import gov.nasa.worldwind.util.*;

/* Layer imports. I fucking love NASA */
import gov.nasa.worldwind.layers.*;
import gov.nasa.worldwind.layers.placename.*;
import gov.nasa.worldwind.layers.Earth.*;

public class WorldWindComponent extends JPanel {
    /* The WorldWind component. I want to make sweet, sweet love to NASA */
    private final WorldWindowGLCanvas wwd;
    
    /* The locations and the line connecting them */
    private java.util.List<Position> locs;
    private Polyline pathLine;
    private SurfaceCircle chopperTargetInner;
    private SurfaceCircle chopperTargetOuter;
    private final SurfaceCircle clickLocation;

    /* Used to have the component follow the last position
     * of the chopper */
    private JPanel statusPane;
    private JPanel followPane;
    private JCheckBox follow;
    private double followAltitude = 1000;
    private final JTextField altField;

    /* Displays status messages */
    private JButton statusLabel;

    /** Create the component */
    public WorldWindComponent() {
	super(new BorderLayout());

	wwd = new WorldWindowGLCanvas();

	/* Create the default model as described in the current worldwind properties. */
	Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
	wwd.setModel(m);

	wwd.addMouseListener(new MouseAdapter() {
		public void mouseClicked(MouseEvent e) {
		    if (e.getButton() == MouseEvent.BUTTON3) {
			Position clickPosition = wwd.getView().computePositionFromScreenPoint(e.getX(), e.getY());
			clickLocation.setCenter(clickPosition);
			statusLabel.setText("Send to (" + 
					    Math.round(1000 * clickPosition.getLatitude().getDegrees()) / 1000.0 + "\u00B0, " +
					    Math.round(1000 * clickPosition.getLongitude().getDegrees()) / 1000.0 + "\u00B0)");
		    }
		}
	    });

	/* Add wwd to the panel */
	add(wwd, BorderLayout.CENTER);

	locs = new LinkedList<Position>();
	pathLine = new Polyline(locs);

	ShapeAttributes attributes = new BasicShapeAttributes();
	attributes.setDrawInterior(false);
	attributes.setDrawOutline(true);
	attributes.setOutlineMaterial(new Material(Color.RED));
	attributes.setOutlineOpacity(0.9);
	attributes.setOutlineWidth(2);

	chopperTargetInner = new SurfaceCircle(attributes);
	chopperTargetInner.setRadius(50);
	chopperTargetOuter = new SurfaceCircle(attributes);
	chopperTargetOuter.setRadius(500);

	attributes.setOutlineMaterial(new Material(Color.GREEN));
	clickLocation = new SurfaceCircle(attributes);
	clickLocation.setRadius(100);

	pathLine.setColor(new Color(255, 0, 0, 200));
	pathLine.setLineWidth(2);
	/* The line is ugly without this */
	pathLine.setAntiAliasHint(Polyline.ANTIALIAS_NICEST);

	/* Create a layer for the polyline and add it to the layer list.
	 * Please just take me, NASA. */
	RenderableLayer polyLayer = new RenderableLayer();
	polyLayer.addRenderable(pathLine);

	SurfaceShapeLayer shapeLayer = new SurfaceShapeLayer();
	shapeLayer.addRenderable(chopperTargetInner);
	shapeLayer.addRenderable(chopperTargetOuter);
	shapeLayer.addRenderable(clickLocation);

	LayerList layers = m.getLayers();
	/* Add high-quality city satellite imagery. Thanks Microsoft! */
	layers.add(new MSVirtualEarthLayer(MSVirtualEarthLayer.LAYER_HYBRID));
	/* Add high-quality topography data for the US. I fucking love NASA / USGS */
	layers.add(new USGSTopoHighRes());
	/* Add chopper location and path layers */
	layers.add(polyLayer);
	layers.add(shapeLayer);

	followPane = new JPanel(new FlowLayout());
	follow = new JCheckBox("Follow Altitude: ");
	altField = new JTextField(new Double(followAltitude).toString());
	altField.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent event) {
		    try {
			followAltitude = new Double(altField.getText());
		    } catch (Exception e) {
			;
		    }
		}
	    });
	
	followPane.add(follow);
	followPane.add(altField);

	statusLabel = new JButton("Right click to select location");
	statusPane = new JPanel(new BorderLayout());
	statusPane.add(followPane, BorderLayout.WEST);
	statusPane.add(statusLabel, BorderLayout.EAST);

	add(statusPane, BorderLayout.SOUTH);
    }

    public String getName() {
	return "Globe";
    }

    /** Add a waypoint, and optionally follow if the box is checked 
     *  @param _w The position to append to the path */
    public void addWaypoint(Position _w) {
	/* Requires a lock on locs or World Wind gets mad */
	synchronized (locs) {
	    locs.add(_w);
	    pathLine.setPositions(locs);
	    chopperTargetInner.setCenter(_w);
	    chopperTargetOuter.setCenter(_w);
	}
	/* If follow is checked and we aren't already animating, move 
	 * the view to a kilometer above the helicopter's current position */
	if (follow.isSelected() && ! wwd.getView().isAnimating())
	    wwd.getView().goTo(_w, _w.getElevation() + followAltitude);
    }

    public void updateUI() {
	if (statusPane != null)
	    statusPane.updateUI();
	if (follow != null)
	    follow.updateUI();
	if (altField != null)
	    altField.updateUI();
	if (statusLabel != null)
	    statusLabel.updateUI();
	if (followPane != null)
	    followPane.updateUI();
    }

    /** Test code. Take off from Nussbaum and fly West */
    public static void main(String args[]) {
	System.err.println("Running");
	JFrame f = new JFrame("World Wind Test");
	WorldWindComponent w = new WorldWindComponent();
	f.add(w);
	f.setPreferredSize(new Dimension(600, 600));
	f.pack();
	f.setVisible(true);

	try {
	    w.addWaypoint(Position.fromDegrees(40.80728, -73.962579, 100));
	    double alt = 200;
	    for (double lon = -73.962579; true; lon -= 0.0001) {
		Thread.sleep(500);
		w.addWaypoint(Position.fromDegrees(40.80728, lon, alt++));
	    }
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }
}

class ClickAndGoSelectListener implements SelectListener {
    private final WorldWindow wwd;
    private final Class pickedObjClass;    // Which picked object class do we handle
    private final double elevationOffset;  // Meters above the target position

    public ClickAndGoSelectListener(WorldWindow wwd, Class pickedObjClass) {
        if (wwd == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;
        this.pickedObjClass = pickedObjClass;
        this.elevationOffset = 0d;
    }

    public ClickAndGoSelectListener(WorldWindow wwd, Class pickedObjClass, double elevationOffset) {
        if (wwd == null) {
            String msg = Logging.getMessage("nullValue.WorldWindow");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        if (pickedObjClass == null) {
            String msg = Logging.getMessage("nullValue.ClassIsNull");
            Logging.logger().severe(msg);
            throw new IllegalArgumentException(msg);
        }

        this.wwd = wwd;
        this.pickedObjClass = pickedObjClass;
        this.elevationOffset = elevationOffset;
    }

    /**
     * Select Listener implementation.
     *
     * @param event the SelectEvent
     */
    public void selected(SelectEvent event) {
        if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
            // This is a left click
            if (event.getTopPickedObject().hasPosition()) {
                // There is a picked object with a position
                if (event.getTopObject().getClass().equals(pickedObjClass)) {
                    // This object class we handle and we have an orbit view
                    Position targetPos = event.getTopPickedObject().getPosition();
                    View view = this.wwd.getView();

		    // Use a PanToIterator to iterate view to target position
                    if(view != null) {
			// The elevation component of 'targetPos' here is not the surface elevation,
			// so we ignore it when specifying the view center position.
                        view.goTo(new Position(targetPos, 0),
				  targetPos.getElevation() + this.elevationOffset);
                    }
                }
            }
        }
    }
}
