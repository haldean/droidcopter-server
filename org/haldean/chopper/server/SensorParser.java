package org.haldean.chopper.server;

public class SensorParser implements Updatable {
    //degrees
    private final int TILT = 2;
    private final int PITCH = 3;
    private final int ROLL = 1;
	
    //m/s^2:
    private final int XACCEL = 1;
    private final int YACCEL = 2;
    private final int ZACCEL = 3;

    private final int ALT = 1;
    private final int BEARING = 2;
    private final int LON = 3;
    private final int LAT = 4;
    private final int SPEED = 5;
    private final int ACCURACY = 6;

    private LocationComponent gps;
    private TiltComponent orient;
    private AccelerationComponent accel;

    public SensorParser() {
	;
    }

    public void setLocationComponent(LocationComponent _gps) {
	gps = _gps;
    }

    public void setTiltComponent(TiltComponent _orient) {
	orient = _orient;
    }

    public void setAccelerationComponent(AccelerationComponent _accel) {
	accel = _accel;
    }

    public void update(String msg) {
	String parts[] = msg.split(":");
	if (parts[0].equals("GPS")) {
	    if (new Double(parts[LAT]) != 0 &&
		new Double(parts[LON]) != 0 &&
		new Double(parts[ALT]) != 0) {
		Waypoint w = new Waypoint(new Double(parts[LAT]), 
					  new Double(parts[LON]), 
					  new Double(parts[ALT]),
					  new Double(parts[ACCURACY]));
		gps.addWaypoint(w);
	    }
	}
	if (parts[0].equals("ORIENT")) {
	    Orientation o = new Orientation(new Double(parts[ROLL]),
					    new Double(parts[TILT]),
					    new Double(parts[PITCH]));
	    orient.setTilt(o);
	}
	if (parts[0].equals("ACCEL")) {
	    accel.setAcceleration(new Double(parts[XACCEL]),
				  new Double(parts[YACCEL]),
				  new Double(parts[ZACCEL]));
	}
    }
}
