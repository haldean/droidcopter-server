package org.haldean.chopper.server;

import gov.nasa.worldwind.geom.*;

/** This takes the sensor messages from the DataReceiver 
 *  and parses it, notifying the appropriate components
 *  with the new data */
public class SensorParser implements Updatable {
    /* The messages are all essentially flattened arrays
     * divided by colons. These constants are used to determine
     * which "index" in the string corresponds to what data */

    private final int TILT = 2;
    private final int PITCH = 3;
    private final int ROLL = 1;
	
    private final int XACCEL = 1;
    private final int YACCEL = 2;
    private final int ZACCEL = 3;

    private final int ALT = 1;
    private final int BEARING = 2;
    private final int LON = 3;
    private final int LAT = 4;
    private final int SPEED = 5;
    private final int ACCURACY = 6;

    /* These are the components that are updated
     * when new data comes in */
    private LocationComponent gps;
    private WorldWindComponent wwc;
    private TiltComponent orient;
    private AccelerationComponent accel;
    private SensorComponent sensors;

    public SensorParser() {
	;
    }

    /** Set the notified location component
     *  @param _gps The location component to notify */
    public void setLocationComponent(LocationComponent _gps) {
	gps = _gps;
    }

    /** Set the notified NASA World Wind globe component 
     *  @param _wwc The World Wind component to notify */
    public void setWorldWindComponent(WorldWindComponent _wwc) {
	wwc = _wwc;
    }

    /** Set the notified tilt component 
     *  @param _orient The tilt component to notify */
    public void setTiltComponent(TiltComponent _orient) {
	orient = _orient;
    }

    /** Set the notified acceleration component 
     *  @param _accel The acceleration component to notify */
    public void setAccelerationComponent(AccelerationComponent _accel) {
	accel = _accel;
    }
    
    /** Set the notified sensor component 
     *  @param _sensors The sensor component to notify */
    public void setSensorComponent(SensorComponent _sensors) {
	sensors = _sensors;
    }

    /** Update the components with new data 
     *  @param msg The received message to parse */
    public void update(String msg) {
	String parts[] = msg.split(":");

	/* If this is a GPS signal notify the World Wind component */
	if (parts[0].equals("GPS")) {
	    double lat = new Double(parts[LAT]);
	    double lon = new Double(parts[LON]);
	    double alt = new Double(parts[ALT]);

	    /* If this is true, the phone isn't receiving a GPS signal */
	    if (! (lat == 0 || lon == 0 || alt == 0))
		wwc.addWaypoint(Position.fromDegrees(lat, lon, alt));
	}

	/* Orientation */
	else if (parts[0].equals("ORIENT")) {
	    Orientation o = new Orientation(new Double(parts[ROLL]),
					    new Double(parts[TILT]),
					    new Double(parts[PITCH]));
	    orient.setTilt(o);
	}

	/* Acceleration */
	else if (parts[0].equals("ACCEL")) {
	    accel.setAcceleration(new Double(parts[XACCEL]),
				  new Double(parts[YACCEL]),
				  new Double(parts[ZACCEL]));
	}

	/* Sensors */
	else if (parts[0].equals("FLUX"))
	    sensors.setFlux(new Double(parts[1]));

	else if (parts[0].equals("LIGHT"))
	    sensors.setLight(new Double(parts[1]));

	else if (parts[0].equals("PROXIMITY"))
	    sensors.setProximity(new Double(parts[1]));

	else if (parts[0].equals("PRESSURE"))
	    sensors.setPressure(new Double(parts[1]));

	else if (parts[0].equals("TEMPERATURE"))
	    sensors.setTemperature(new Double(parts[1]));
    }
}
